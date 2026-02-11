import { Environment, getWebsocketScheme } from "src/environments";
import { IS_BACKEND_BUILD, IS_EDGE_BUILD } from "src/environments/buildtime/backend-type";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Backend",
        url: `${getWebsocketScheme()}://${location.hostname}:8082`,

        production: false,
        debugMode: true,

        IS_BACKEND_BUILD: IS_BACKEND_BUILD,
        IS_EDGE_BUILD: IS_EDGE_BUILD,
    },
};
