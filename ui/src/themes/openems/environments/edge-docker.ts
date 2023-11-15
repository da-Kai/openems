import { Environment } from "src/environments";
import { theme } from "./theme";
import { AppConfiguration } from "read-appsettings-json";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Edge',
        url: AppConfiguration.Setting().edgeWebsocket,

        production: true,
        debugMode: false
    }
};
