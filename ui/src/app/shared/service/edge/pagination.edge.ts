// @ts-strict-ignore
import { Directive } from "@angular/core";
import { Router } from "@angular/router";
import { States } from "../../ngrx-store/states";
import { Edge } from "../../shared";
import { Service } from "../service";

/**
 * Edge-specific Pagination that doesn't subscribe to edges (not needed in single-edge mode)
 */
@Directive()
export class Pagination {

    private edge: Edge | null = null;
    private count = 0;

    constructor(
        public service: Service,
        private router: Router,
    ) { }

    public getAndSubscribeEdge(edge: Edge): Promise<void> {
        return new Promise<void>((resolve) => {
            this.service.updateCurrentEdge(edge.id).then((edge) => {

                this.edge = edge;
                // No subscription needed in Edge builds
            }).then(() => {
                this.service.websocket.state.set(States.EDGE_SELECTED);
            })
                .finally(resolve)
                .catch(() => {
                    this.router.navigate(["index"]);
                });
        });
    }

    public async subscribeEdge(_edge: Edge) {
        // No-op for Edge builds - subscription not needed in single-edge mode
    }
}
