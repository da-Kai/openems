#!/usr/bin/env python3
"""
Backend.Edge.App WebSocket Reconnection Burst Test
====================================================

Simulates multiple OpenEMS Edge devices connecting to the Backend.Edge.App
WebSocket server to test its resilience under reconnection bursts.

Protocol details (from io.openems.backend.edge.server.OnOpen):
  - The server reads the 'apikey' HTTP header during the WebSocket upgrade handshake.
  - On success the server maps the apikey to an Edge-ID from its local Cache.
  - After the handshake JSON-RPC messages are exchanged over the socket.

Usage:
    python3 backend_edge_reconnection_test.py [OPTIONS]

Examples:
    # Basic burst: 50 edges connect simultaneously, each reconnecting 5 times
    python3 backend_edge_reconnection_test.py

    # Custom: 200 concurrent edges, 10 reconnects, against a remote server
    python3 backend_edge_reconnection_test.py \\
        --host 192.168.1.10 --port 8081 \\
        --edges 200 --reconnects 10 \\
        --apikey my-secret-key

Requirements:
    pip install websockets
"""

import argparse
import asyncio
import logging
import random
import statistics
import time
import uuid
from dataclasses import dataclass, field
from typing import Optional

try:
    import websockets
    from websockets.exceptions import (
        ConnectionClosedError,
        ConnectionClosedOK,
        InvalidStatus,
    )
except ImportError:
    raise SystemExit(
        "The 'websockets' package is required.\n"
        "Install it with:  pip install websockets"
    )

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Result tracking
# ---------------------------------------------------------------------------


@dataclass
class EdgeResult:
    """Per-edge-simulation result."""

    edge_index: int
    apikey: str
    attempts: int = 0
    successes: int = 0
    failures: int = 0
    auth_rejected: int = 0
    connect_times_ms: list[float] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Single connection helper
# ---------------------------------------------------------------------------


async def connect_once(
    uri: str,
    apikey: str,
    hold_seconds: float,
    edge_index: int,
) -> tuple[bool, float, Optional[str]]:
    """
    Open one WebSocket connection to the Backend.Edge.App server.

    The server (io.openems.backend.edge.server.OnOpen) reads the 'apikey' HTTP
    header from the WebSocket upgrade request. If authentication fails the server
    closes the connection with code 1003 (REFUSE).

    Returns:
        (success, connect_time_ms, error_message_or_None)
    """
    t0 = time.perf_counter()
    extra_headers = {"apikey": apikey}

    try:
        async with websockets.connect(
            uri,
            additional_headers=extra_headers,
            open_timeout=10,
            close_timeout=5,
        ) as ws:
            connect_ms = (time.perf_counter() - t0) * 1000
            log.debug(
                "Edge %d connected in %.1f ms (apikey=%s)", edge_index, connect_ms, apikey
            )

            # Keep the connection alive for the configured hold period so the
            # server registers the edge as "connected".  We simply wait; the
            # server may send JSON-RPC notifications which we silently drain.
            try:
                await asyncio.wait_for(ws.recv(), timeout=hold_seconds)
            except asyncio.TimeoutError:
                pass  # expected – we just wanted to hold the connection open
            except (ConnectionClosedOK, ConnectionClosedError):
                pass  # server closed first; count as success nonetheless

            return True, connect_ms, None

    except InvalidStatus as exc:
        connect_ms = (time.perf_counter() - t0) * 1000
        msg = f"HTTP {exc.response.status_code} – authentication rejected"
        log.debug("Edge %d: %s", edge_index, msg)
        return False, connect_ms, msg

    except OSError as exc:
        connect_ms = (time.perf_counter() - t0) * 1000
        msg = f"OS error: {exc}"
        return False, connect_ms, msg

    except Exception as exc:  # noqa: BLE001
        connect_ms = (time.perf_counter() - t0) * 1000
        msg = f"{type(exc).__name__}: {exc}"
        return False, connect_ms, msg


# ---------------------------------------------------------------------------
# Per-edge simulation
# ---------------------------------------------------------------------------


async def simulate_edge(
    *,
    uri: str,
    apikey: str,
    edge_index: int,
    reconnects: int,
    delay_between_reconnects_s: float,
    hold_seconds: float,
    jitter_s: float,
    semaphore: asyncio.Semaphore,
) -> EdgeResult:
    """
    Simulate a single OpenEMS Edge that connects, disconnects, and reconnects
    `reconnects` times to stress-test the server's reconnection handling.
    """
    result = EdgeResult(edge_index=edge_index, apikey=apikey)

    for attempt in range(reconnects):
        result.attempts += 1

        async with semaphore:
            success, elapsed_ms, error = await connect_once(
                uri=uri,
                apikey=apikey,
                hold_seconds=hold_seconds,
                edge_index=edge_index,
            )

        result.connect_times_ms.append(elapsed_ms)

        if success:
            result.successes += 1
        else:
            result.failures += 1
            if error:
                result.errors.append(error)
                # Detect authentication rejections (HTTP 4xx from server)
                if "authentication rejected" in error or "403" in error or "401" in error:
                    result.auth_rejected += 1

        # Wait before next reconnect (with optional jitter to avoid thundering herd)
        if attempt < reconnects - 1:
            sleep_time = delay_between_reconnects_s
            if jitter_s > 0:
                sleep_time += random.uniform(0, jitter_s)
            if sleep_time > 0:
                await asyncio.sleep(sleep_time)

    return result


# ---------------------------------------------------------------------------
# Main burst runner
# ---------------------------------------------------------------------------


async def run_burst(
    host: str,
    port: int,
    apikey: str,
    num_edges: int,
    reconnects: int,
    delay_between_reconnects_s: float,
    hold_seconds: float,
    jitter_s: float,
    max_concurrent: int,
) -> list[EdgeResult]:
    """Launch all edge simulations concurrently and collect results."""
    uri = f"ws://{host}:{port}"
    semaphore = asyncio.Semaphore(max_concurrent)

    log.info("=" * 60)
    log.info("Backend.Edge.App Reconnection Burst Test")
    log.info("=" * 60)
    log.info("  URI              : %s", uri)
    log.info("  Edges            : %d", num_edges)
    log.info("  Reconnects/edge  : %d", reconnects)
    log.info("  Total connections: %d", num_edges * reconnects)
    log.info("  Max concurrent   : %d", max_concurrent)
    log.info("  Hold per conn    : %.2f s", hold_seconds)
    log.info("  Delay between    : %.2f s (+%.2f jitter)", delay_between_reconnects_s, jitter_s)
    log.info("  Apikey           : %s", apikey)
    log.info("=" * 60)

    tasks = [
        simulate_edge(
            uri=uri,
            apikey=apikey,
            edge_index=i,
            reconnects=reconnects,
            delay_between_reconnects_s=delay_between_reconnects_s,
            hold_seconds=hold_seconds,
            jitter_s=jitter_s,
            semaphore=semaphore,
        )
        for i in range(num_edges)
    ]

    t_start = time.perf_counter()
    results: list[EdgeResult] = await asyncio.gather(*tasks)
    elapsed = time.perf_counter() - t_start

    _print_summary(results, elapsed)
    return results


def _print_summary(results: list[EdgeResult], elapsed_s: float) -> None:
    total_attempts = sum(r.attempts for r in results)
    total_success = sum(r.successes for r in results)
    total_failure = sum(r.failures for r in results)
    total_auth_rejected = sum(r.auth_rejected for r in results)

    all_times = [t for r in results for t in r.connect_times_ms]

    log.info("")
    log.info("=" * 60)
    log.info("RESULTS")
    log.info("=" * 60)
    log.info("  Wall clock time  : %.2f s", elapsed_s)
    log.info("  Total attempts   : %d", total_attempts)
    log.info("  Successful       : %d  (%.1f%%)",
             total_success, 100 * total_success / max(total_attempts, 1))
    log.info("  Failed           : %d  (%.1f%%)",
             total_failure, 100 * total_failure / max(total_attempts, 1))
    log.info("  Auth rejected    : %d", total_auth_rejected)

    if all_times:
        log.info("  Connect time (ms):")
        log.info("    min  = %.1f", min(all_times))
        log.info("    max  = %.1f", max(all_times))
        log.info("    mean = %.1f", statistics.mean(all_times))
        if len(all_times) > 1:
            log.info("    p50  = %.1f", statistics.median(all_times))
            sorted_times = sorted(all_times)
            p95_idx = int(len(sorted_times) * 0.95)
            p99_idx = int(len(sorted_times) * 0.99)
            log.info("    p95  = %.1f", sorted_times[p95_idx])
            log.info("    p99  = %.1f", sorted_times[p99_idx])

    # Show up to 10 unique error messages
    all_errors = [e for r in results for e in r.errors]
    if all_errors:
        unique_errors: dict[str, int] = {}
        for e in all_errors:
            unique_errors[e] = unique_errors.get(e, 0) + 1
        log.info("")
        log.info("  Error summary (top 10):")
        for msg, count in sorted(unique_errors.items(), key=lambda x: -x[1])[:10]:
            log.info("    [%dx] %s", count, msg)

    log.info("=" * 60)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Reconnection burst test for the Backend.Edge.App WebSocket server.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    p.add_argument("--host", default="localhost",
                   help="Hostname or IP of the Backend.Edge.App server.")
    p.add_argument("--port", type=int, default=8081,
                   help="WebSocket server port (Backend.Edge.App default: 8081).")
    p.add_argument("--apikey", default="test-apikey-" + str(uuid.uuid4()),
                   help="Apikey sent in the HTTP upgrade header (must be known to the server's Cache).")
    p.add_argument("--edges", type=int, default=50,
                   help="Number of simulated Edge devices.")
    p.add_argument("--reconnects", type=int, default=5,
                   help="How many times each Edge connects/disconnects.")
    p.add_argument("--delay", type=float, default=0.0,
                   help="Seconds to wait between reconnects (per edge).")
    p.add_argument("--jitter", type=float, default=0.1,
                   help="Random jitter added to --delay to desynchronise edges.")
    p.add_argument("--hold", type=float, default=0.5,
                   help="Seconds to keep the WebSocket open after connecting.")
    p.add_argument("--max-concurrent", type=int, default=0,
                   help="Maximum simultaneous open connections (0 = unlimited).")
    p.add_argument("--verbose", action="store_true",
                   help="Enable DEBUG logging.")
    return p.parse_args()


def main() -> None:
    args = parse_args()

    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    max_concurrent = args.max_concurrent if args.max_concurrent > 0 else args.edges * args.reconnects

    asyncio.run(
        run_burst(
            host=args.host,
            port=args.port,
            apikey=args.apikey,
            num_edges=args.edges,
            reconnects=args.reconnects,
            delay_between_reconnects_s=args.delay,
            hold_seconds=args.hold,
            jitter_s=args.jitter,
            max_concurrent=max_concurrent,
        )
    )


if __name__ == "__main__":
    main()
