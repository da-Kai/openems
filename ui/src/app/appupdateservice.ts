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
