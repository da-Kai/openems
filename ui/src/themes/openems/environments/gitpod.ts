import { Environment } from "src/environments";
import { IS_BACKEND_BUILD, IS_EDGE_BUILD } from "src/environments/buildtime/backend-type";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Backend",
        // gitpod puts the port number in front of the hostname
        url: "wss://8082-" + location.hostname.substring(location.hostname.indexOf("-") + 1),

        production: false,
        debugMode: true,

        IS_BACKEND_BUILD: IS_BACKEND_BUILD,
        IS_EDGE_BUILD: IS_EDGE_BUILD,
    },
};
