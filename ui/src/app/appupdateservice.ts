import { Injectable } from "@angular/core";
import { SwUpdate } from "@angular/service-worker";

import { Service } from "./shared/shared";

@Injectable({
    providedIn: "root",
})
export class CheckForUpdateService {

    constructor(private update: SwUpdate,
        private service: Service,
    ) { }
}
// Will be used in Future
@Injectable()
export class LogUpdateService {

    constructor(updates: SwUpdate) {
        updates.versionUpdates.subscribe(evt => {
            switch (evt.type) {
                case "VERSION_DETECTED":
                    // New app version detected
                    break;
                case "VERSION_READY":
                    // New app version ready for use
                    break;
                case "VERSION_INSTALLATION_FAILED":
                    // Failed to install app version
                    break;
                default:
                    break;
            }
        });
    }
}
