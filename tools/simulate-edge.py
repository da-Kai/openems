#!/usr/bin/env python3
"""
OpenEMS Edge Backend Simulator
===============================
Simulates one or more OpenEMS Edge devices that connect to the OpenEMS Backend
WebSocket API and continuously send realistic, randomised channel values.

Simulated components per Edge:
  - meter0        Consumption meter  (Simulator.ProductionMeter.Acting)
  - pvInverter0   PV inverter        (Simulator.PvInverter)
  - ess0          Battery ESS        (Simulator.EssSymmetric.Reacting)
  - meter1        Grid meter         (Simulator.GridMeter.Reacting)

Protocol (matching ControllerApiBackendImpl / WebsocketClient):
  - HTTP header "Apikey"      – authentication key
  - HTTP header "Instance-Id" – random UUID per Edge instance
  - JSON-RPC 2.0 notification "edgeConfig"        – sent once on connect
  - JSON-RPC 2.0 notification "timestampedData"   – sent every --interval seconds

Usage examples:
  # Single edge
  python3 simulate-edge.py --apikey MY_KEY --url ws://localhost:8081

  # 1 000 edges, 10-second send interval, WARNING-level logging
  python3 simulate-edge.py -k MY_KEY -n 1000 -i 10 --log-level WARNING

Requirements:
  pip install websockets
"""

import argparse
import asyncio
import json
import logging
import math
import random
import signal
import time
import uuid
from typing import Any

# ---------------------------------------------------------------------------
# Edge configuration helpers
# ---------------------------------------------------------------------------

def _build_edge_config() -> dict:
    """
    Returns the payload for the ``edgeConfig`` JSON-RPC notification.

    Structure matches EdgeConfig.toJson() / EdgeConfigNotification.getParams().
    Factory IDs match the OSGi @Component "name" attributes used in the Java
    simulator bundles; nature IDs are the fully-qualified Java interface names.
    """
    return {
        "components": {
            "meter0": {
                "alias": "Consumption Meter",
                "factoryId": "Simulator.ProductionMeter.Acting",
                "properties": {
                    "id": "meter0",
                    "alias": "Consumption Meter",
                    "enabled": True,
                },
                "channels": {},
            },
            "pvInverter0": {
                "alias": "PV Inverter",
                "factoryId": "Simulator.PvInverter",
                "properties": {
                    "id": "pvInverter0",
                    "alias": "PV Inverter",
                    "enabled": True,
                },
                "channels": {},
            },
            "ess0": {
                "alias": "Battery ESS",
                "factoryId": "Simulator.EssSymmetric.Reacting",
                "properties": {
                    "id": "ess0",
                    "alias": "Battery ESS",
                    "enabled": True,
                    "maxApparentPower": 10000,
                    "maxChargePower": 10000,
                    "maxDischargePower": 10000,
                    "capacity": 10000,
                    "initialSoc": 50,
                    "gridMode": "ON_GRID",
                },
                "channels": {},
            },
            "meter1": {
                "alias": "Grid Meter",
                "factoryId": "Simulator.GridMeter.Reacting",
                "properties": {
                    "id": "meter1",
                    "alias": "Grid Meter",
                    "enabled": True,
                },
                "channels": {},
            },
        },
        "factories": {
            "Simulator.ProductionMeter.Acting": {
                "name": "Simulator ProductionMeter Acting",
                "description": "Simulates an acting production meter.",
                "natureIds": [
                    "io.openems.edge.meter.api.ElectricityMeter",
                    "io.openems.edge.common.component.OpenemsComponent",
                ],
            },
            "Simulator.PvInverter": {
                "name": "Simulator PV-Inverter",
                "description": "Simulates a PV inverter.",
                "natureIds": [
                    "io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter",
                    "io.openems.edge.meter.api.ElectricityMeter",
                    "io.openems.edge.common.component.OpenemsComponent",
                ],
            },
            "Simulator.EssSymmetric.Reacting": {
                "name": "Simulator EssSymmetric Reacting",
                "description": "Simulates a symmetric ESS.",
                "natureIds": [
                    "io.openems.edge.ess.api.ManagedSymmetricEss",
                    "io.openems.edge.ess.api.SymmetricEss",
                    "io.openems.edge.common.component.OpenemsComponent",
                ],
            },
            "Simulator.GridMeter.Reacting": {
                "name": "Simulator GridMeter Reacting",
                "description": "Simulates a reacting grid meter.",
                "natureIds": [
                    "io.openems.edge.meter.api.ElectricityMeter",
                    "io.openems.edge.common.component.OpenemsComponent",
                ],
            },
        },
    }


# ---------------------------------------------------------------------------
# Per-Edge simulator
# ---------------------------------------------------------------------------

class EdgeSimulator:
    """
    Represents a single simulated OpenEMS Edge connection.

    State is maintained across send cycles to produce smooth, correlated
    time-series values (e.g. SoC drifts slowly, PV follows a day curve).
    """

    # ESS capacity [Wh] used for SoC tracking
    _ESS_CAPACITY_WH: int = 10_000
    # Maximum PV peak power [W] (negative = production in OpenEMS convention)
    _PV_PEAK_W: int = 5_000
    # Nominal grid voltage [V]
    _NOMINAL_VOLTAGE_V: float = 230.0

    def __init__(self, index: int, apikey: str, url: str, interval: float) -> None:
        self.index = index
        self.apikey = apikey
        self.url = url
        self.interval = interval
        self.instance_id: str = str(uuid.uuid4())

        # Randomise starting state so 1 000 edges don't all look identical
        self._soc: float = random.uniform(15.0, 85.0)
        # Random phase shift so PV curves don't all peak at the same moment
        self._day_phase_offset: float = random.uniform(-0.5, 0.5)  # hours
        # Slow drift seed for consumption baseline
        self._consumption_drift: float = random.uniform(800.0, 2500.0)

        self.log = logging.getLogger(f"edge[{index:04d}]")

    # ------------------------------------------------------------------
    # Value generation
    # ------------------------------------------------------------------

    def _solar_factor(self) -> float:
        """Returns 0–1 representing current solar irradiance (simplified day curve)."""
        # Local "clock" hour, adjusted by per-edge phase offset
        hour = ((time.time() / 3600.0) % 24.0) + self._day_phase_offset
        # Sine curve peaking at solar noon (hour 12), active 06:00–18:00
        factor = math.sin(math.pi * (hour - 6.0) / 12.0)
        return max(0.0, factor)

    def _generate_channel_values(self) -> dict[str, Any]:
        """
        Compute one set of realistic channel values for all four components.

        Channel ID naming follows the Java ChannelId.channelIdUpperToCamel()
        convention: UPPER_UNDERSCORE enum names → UpperCamelCase strings.
        (e.g. ACTIVE_POWER → "ActivePower", SOC → "Soc")
        """
        solar = self._solar_factor()

        # --- PV production [W] -------------------------------------------
        # Negative sign = production (OpenEMS convention for SymmetricEss /
        # ElectricityMeter ActivePower where negative means feed-in / produce)
        pv_w = -int(solar * random.gauss(self._PV_PEAK_W, 200.0))
        pv_w = min(0, pv_w)  # Clip: PV never "consumes"

        # --- Consumption [W] ---------------------------------------------
        # House load: baseline + small random variation
        consumption_w = int(self._consumption_drift + random.gauss(0, 150))
        consumption_w = max(50, consumption_w)

        # --- ESS power [W] (negative=charge, positive=discharge) ----------
        # Simple: charge when PV > consumption, discharge otherwise
        surplus = consumption_w + pv_w  # pv_w is negative, so surplus = load - pv
        if surplus > 0:
            # Load > PV → try to discharge ESS
            ess_w = min(surplus, 3000)
        else:
            # PV > Load → charge ESS with surplus
            ess_w = max(surplus, -3000)

        # Clamp to battery state
        if ess_w > 0 and self._soc < 5.0:
            ess_w = 0  # empty: can't discharge
        if ess_w < 0 and self._soc > 95.0:
            ess_w = 0  # full: can't charge

        # Update SoC: negative ess_w = charging (power flows into battery) → positive delta_wh
        delta_wh = -ess_w * self.interval / 3600.0
        self._soc += delta_wh / self._ESS_CAPACITY_WH * 100.0
        self._soc = max(0.0, min(100.0, self._soc))

        # --- Grid meter [W] (positive=import, negative=export) -----------
        grid_w = consumption_w + pv_w + ess_w

        # --- Voltage & frequency (per-phase, milli-units) ----------------
        v_mv = int(random.gauss(self._NOMINAL_VOLTAGE_V, 1.5) * 1000)
        freq_mhz = int(random.gauss(50.0, 0.03) * 1000)

        # Symmetric split across three phases (integer division rounding)
        def split3(value: int) -> tuple[int, int, int]:
            base = value // 3
            rem = value - base * 3
            return base + rem, base, base

        meter0_l1, meter0_l2, meter0_l3 = split3(consumption_w)
        pv_l1, pv_l2, pv_l3 = split3(pv_w)
        ess_l1, ess_l2, ess_l3 = split3(ess_w)
        grid_l1, grid_l2, grid_l3 = split3(grid_w)

        def current_ma(power_w: int, voltage_mv: int) -> int:
            v = voltage_mv / 1000.0
            if v == 0:
                return 0
            return int(abs(power_w) * 1000 / v)

        i_m0_ma = current_ma(meter0_l1, v_mv)
        i_pv_ma = current_ma(pv_l1, v_mv)
        i_grid_ma = current_ma(grid_l1, v_mv)

        # Energy counters (cumulative; approximated from current SoC)
        # ActiveChargeEnergy  = energy delivered INTO the battery so far ≈ SoC fraction of capacity
        # ActiveDischargeEnergy = energy taken OUT of the battery so far ≈ remaining capacity
        charge_energy_wh = int((self._soc / 100.0) * self._ESS_CAPACITY_WH)
        discharge_energy_wh = int((1.0 - self._soc / 100.0) * self._ESS_CAPACITY_WH)

        return {
            # ---- meter0: Consumption Meter ------------------------------
            "meter0/ActivePower": consumption_w,
            "meter0/ActivePowerL1": meter0_l1,
            "meter0/ActivePowerL2": meter0_l2,
            "meter0/ActivePowerL3": meter0_l3,
            "meter0/ReactivePower": 0,
            "meter0/VoltageL1": v_mv,
            "meter0/VoltageL2": v_mv,
            "meter0/VoltageL3": v_mv,
            "meter0/CurrentL1": i_m0_ma,
            "meter0/CurrentL2": i_m0_ma,
            "meter0/CurrentL3": i_m0_ma,
            "meter0/Frequency": freq_mhz,
            # ---- pvInverter0: PV Inverter --------------------------------
            "pvInverter0/ActivePower": pv_w,
            "pvInverter0/ActivePowerL1": pv_l1,
            "pvInverter0/ActivePowerL2": pv_l2,
            "pvInverter0/ActivePowerL3": pv_l3,
            "pvInverter0/ReactivePower": 0,
            "pvInverter0/VoltageL1": v_mv,
            "pvInverter0/VoltageL2": v_mv,
            "pvInverter0/VoltageL3": v_mv,
            "pvInverter0/CurrentL1": i_pv_ma,
            "pvInverter0/CurrentL2": i_pv_ma,
            "pvInverter0/CurrentL3": i_pv_ma,
            "pvInverter0/MaxApparentPower": self._PV_PEAK_W,
            # ---- ess0: Battery ESS --------------------------------------
            "ess0/Soc": int(round(self._soc)),
            "ess0/Capacity": self._ESS_CAPACITY_WH,
            "ess0/GridMode": 1,  # 1 = ON_GRID
            "ess0/ActivePower": ess_w,
            "ess0/ActivePowerL1": ess_l1,
            "ess0/ActivePowerL2": ess_l2,
            "ess0/ActivePowerL3": ess_l3,
            "ess0/ReactivePower": 0,
            "ess0/AllowedChargePower": -self._ESS_CAPACITY_WH,
            "ess0/AllowedDischargePower": self._ESS_CAPACITY_WH,
            "ess0/MaxApparentPower": self._ESS_CAPACITY_WH,
            "ess0/ActiveChargeEnergy": charge_energy_wh,
            "ess0/ActiveDischargeEnergy": discharge_energy_wh,
            # ---- meter1: Grid Meter -------------------------------------
            "meter1/ActivePower": grid_w,
            "meter1/ActivePowerL1": grid_l1,
            "meter1/ActivePowerL2": grid_l2,
            "meter1/ActivePowerL3": grid_l3,
            "meter1/ReactivePower": 0,
            "meter1/VoltageL1": v_mv,
            "meter1/VoltageL2": v_mv,
            "meter1/VoltageL3": v_mv,
            "meter1/CurrentL1": i_grid_ma,
            "meter1/CurrentL2": i_grid_ma,
            "meter1/CurrentL3": i_grid_ma,
            "meter1/Frequency": freq_mhz,
        }

    # ------------------------------------------------------------------
    # JSON-RPC message builders
    # ------------------------------------------------------------------

    @staticmethod
    def _notification(method: str, params: dict) -> str:
        """Serialise a JSON-RPC 2.0 notification (no ``id`` field)."""
        return json.dumps({"jsonrpc": "2.0", "method": method, "params": params})

    def _edge_config_msg(self) -> str:
        return self._notification("edgeConfig", _build_edge_config())

    def _timestamped_data_msg(self) -> str:
        timestamp_ms = int(time.time() * 1000)
        values = self._generate_channel_values()
        return self._notification("timestampedData", {str(timestamp_ms): values})

    # ------------------------------------------------------------------
    # Main async run loop
    # ------------------------------------------------------------------

    async def run(self, stop: asyncio.Event) -> None:
        """
        Connects to the backend, sends edgeConfig + channel values, and
        reconnects automatically if the connection drops.
        """
        # Import here so the module is usable even without websockets installed
        # (import error surfaces only when actually running a simulation).
        try:
            import websockets
        except ImportError as exc:
            raise SystemExit("websockets library not found – run: pip install websockets") from exc

        headers = {
            "Apikey": self.apikey,
            "Instance-Id": self.instance_id,
        }

        while not stop.is_set():
            try:
                self.log.debug("Connecting to %s", self.url)
                async with websockets.connect(
                    self.url,
                    additional_headers=headers,
                    ping_interval=20,
                    ping_timeout=30,
                    close_timeout=5,
                ) as ws:
                    self.log.info("Connected (Instance-Id=%s)", self.instance_id)

                    # 1. Send EdgeConfig immediately on connect
                    await ws.send(self._edge_config_msg())

                    # 2. Send initial channel values right away
                    await ws.send(self._timestamped_data_msg())

                    # 3. Periodic channel-value sends
                    while not stop.is_set():
                        try:
                            await asyncio.wait_for(stop.wait(), timeout=self.interval)
                        except asyncio.TimeoutError:
                            pass  # normal: interval elapsed

                        if stop.is_set():
                            break

                        try:
                            await ws.send(self._timestamped_data_msg())
                        except websockets.ConnectionClosed:
                            self.log.warning("Connection closed, reconnecting…")
                            break

            except (
                websockets.WebSocketException,
                ConnectionRefusedError,
                OSError,
            ) as exc:
                self.log.warning("Connection error: %s – retrying in 10 s", exc)
                try:
                    await asyncio.wait_for(stop.wait(), timeout=10.0)
                except asyncio.TimeoutError:
                    pass


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

async def _run_all(args: argparse.Namespace) -> None:
    log = logging.getLogger("simulator")
    log.info(
        "Starting %d Edge simulator(s) → %s  (interval=%.1f s)",
        args.count,
        args.url,
        args.interval,
    )

    stop = asyncio.Event()

    # Graceful shutdown on SIGINT / SIGTERM
    loop = asyncio.get_running_loop()
    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, stop.set)
        except NotImplementedError:
            # Windows does not support add_signal_handler for all signals
            pass

    simulators = [
        EdgeSimulator(i, args.apikey, args.url, args.interval)
        for i in range(args.count)
    ]

    tasks = [asyncio.create_task(sim.run(stop)) for sim in simulators]

    log.info("%d simulator task(s) running – press Ctrl+C to stop", len(tasks))
    await stop.wait()
    log.info("Shutting down…")

    for task in tasks:
        task.cancel()

    await asyncio.gather(*tasks, return_exceptions=True)
    log.info("All simulators stopped.")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Simulate OpenEMS Edge devices connecting to the Backend API.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument(
        "--url",
        default="ws://localhost:8081",
        help="Backend WebSocket URL (e.g. ws://backend-host:8081)",
    )
    parser.add_argument(
        "--apikey",
        "-k",
        required=True,
        metavar="KEY",
        help="API key used in the 'Apikey' HTTP header for authentication",
    )
    parser.add_argument(
        "--count",
        "-n",
        type=int,
        default=1,
        metavar="N",
        help="Number of Edge instances to simulate concurrently",
    )
    parser.add_argument(
        "--interval",
        "-i",
        type=float,
        default=5.0,
        metavar="SECONDS",
        help="Seconds between timestampedData sends",
    )
    parser.add_argument(
        "--log-level",
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"],
        help="Python logging level",
    )
    args = parser.parse_args()

    logging.basicConfig(
        level=getattr(logging, args.log_level),
        format="%(asctime)s [%(levelname)-8s] %(name)s: %(message)s",
    )

    asyncio.run(_run_all(args))


if __name__ == "__main__":
    main()
