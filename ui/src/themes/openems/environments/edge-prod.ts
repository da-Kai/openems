import { Environment , getWebsocketScheme } from "src/environments";
import { IS_BACKEND_BUILD, IS_EDGE_BUILD } from "src/environments/buildtime/backend-type";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Edge",
        url: `${getWebsocketScheme()}://${location.hostname}:8075`,

        production: true,
        debugMode: false,

        IS_BACKEND_BUILD: IS_BACKEND_BUILD,
        IS_EDGE_BUILD: IS_EDGE_BUILD,
    },
};
