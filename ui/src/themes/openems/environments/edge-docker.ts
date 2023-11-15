import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Edge',
        url: "ws://127.0.0.1/websocket",

        production: true,
        debugMode: false
    }
};
