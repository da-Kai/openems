// @ts-strict-ignore
import { Environment, getWebsocketScheme } from "src/environments";
import { IS_BACKEND_BUILD, IS_EDGE_BUILD } from "src/environments/buildtime/backend-type";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{
        backend: "OpenEMS Backend",
        url: `${getWebsocketScheme()}://${location.host}/openems-backend`,

        production: true,
        debugMode: false,

        IS_BACKEND_BUILD: IS_BACKEND_BUILD,
        IS_EDGE_BUILD: IS_EDGE_BUILD,
    },
};
