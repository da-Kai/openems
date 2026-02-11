// @ts-strict-ignore
import { Injectable } from "@angular/core";
import { Edge } from "../../components/edge/edge";
import { GetEdgesRequest } from "../../jsonrpc/request/getEdgesRequest";
import { GetEdgesResponse } from "../../jsonrpc/response/getEdgesResponse";
import { Role } from "../../type/role";
import { DateUtils } from "../../utils/date/dateutils";
import { Service } from "../service";

/**
 * Backend-specific service extension that handles multi-edge operations
 * This service is only used in Backend builds
 */
@Injectable({
    providedIn: "root",
})
export class BackendService {

    constructor(private service: Service) { }

    /**
     * Gets the page for the given number.
     * Backend-only method for fetching multiple edges
     *
     * @param req the get edges request
     * @returns a promise with the resulting edges
     */
    public getEdges(req: GetEdgesRequest): Promise<Edge[]> {
        return new Promise<Edge[]>((resolve, reject) => {
            this.service.websocket.sendSafeRequest(req)
                .then((response) => {

                    const result = (response as GetEdgesResponse).result;

                    // TODO change edges-map to array or other way around
                    const value = this.service.metadata.value;
                    const mappedResult = [];
                    for (const edge of result.edges) {
                        const mappedEdge = new Edge(
                            edge.id,
                            edge.comment,
                            edge.producttype,
                            ("version" in edge) ? edge["version"] : "0.0.0",
                            Role.getRole(edge.role.toString()),
                            edge.isOnline,
                            edge.lastmessage,
                            edge.sumState,
                            DateUtils.stringToDate(edge.firstSetupProtocol?.toString()),
                            edge.settings ?? null,
                        );
                        value.edges[edge.id] = mappedEdge;
                        mappedResult.push(mappedEdge);
                    }

                    this.service.metadata.next(value);
                    resolve(mappedResult);
                }).catch((err) => {
                    reject(err);
                });
        });
    }
}
