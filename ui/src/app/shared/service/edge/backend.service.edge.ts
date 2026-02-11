// @ts-strict-ignore
import { Injectable } from "@angular/core";
import { Edge } from "../../components/edge/edge";

/**
 * Edge-specific stub service that provides no-op implementations
 * This service is used in Edge builds to avoid importing backend-only code
 */
@Injectable({
    providedIn: "root",
})
export class BackendService {

    constructor() { }

    /**
     * Stub implementation for Edge builds - this method is never called in Edge mode
     * Type signature maintained for consistency with backend implementation
     * @param _req unused parameter (accepts any object to match GetEdgesRequest interface)
     * @returns rejected promise
     */
    public getEdges(_req: object): Promise<Edge[]> {
        return Promise.reject(new Error("getEdges is not available in Edge builds"));
    }
}
