// @ts-strict-ignore
import { Injectable } from "@angular/core";
import { Edge } from "../components/edge/edge";

/**
 * Placeholder BackendService - this file should be replaced during build
 * via fileReplacements in angular.json
 */
@Injectable({
    providedIn: "root",
})
export class BackendService {
    constructor() {
        throw new Error("BackendService should be replaced via fileReplacements");
    }

    public getEdges(_req: object): Promise<Edge[]> {
        throw new Error("BackendService should be replaced via fileReplacements");
    }
}
