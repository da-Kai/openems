import { Environment } from "src/environments";
import { theme } from "./theme";
import { AppConfiguration } from "read-appsettings-json";

export const environment: Environment = {
    ...theme, ...{

        backend: AppConfiguration.Setting().backend,
        url: AppConfiguration.Setting().url,

        production: true,
        debugMode: false,
    },
};
