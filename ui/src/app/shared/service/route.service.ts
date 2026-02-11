import { Injectable, OnDestroy, signal, WritableSignal } from "@angular/core";
import { ActivatedRouteSnapshot, NavigationEnd, Router } from "@angular/router";
import { Subscription } from "rxjs";

@Injectable()
export class RouteService implements OnDestroy {

    public currentUrl: WritableSignal<string | null> = signal(null);

    private previousUrl: string | null = null;
    private subscriptions: Subscription = new Subscription();

    constructor(private router: Router) {
        this.previousUrl = this.currentUrl();
        this.subscriptions.add(
            router.events.subscribe(event => {
                if (event instanceof NavigationEnd) {
                    this.previousUrl = this.currentUrl();
                    this.currentUrl.set(event.urlAfterRedirects);;
                }
            })
        );
    }

    public ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }


    /**
     * Gets the previous url, active before this url
    *
    * @returns the previous url
    */
    public getPreviousUrl() {
        return this.previousUrl;
    }


    /**
     * Gets the current url
    *
    * @returns the current url
    */
    public getCurrentUrl() {
        return this.currentUrl();
    }

    /**
     * Gets the current url
    *
    * @returns the current url
    */
    public getCurrentUrl2(): string | null {
        // This method was creating a new subscription on every call causing memory leaks.
        // Use getCurrentUrl() instead which returns the current URL from the signal.
        return this.currentUrl();
    }

    /**
     * Gets the route params
     *
     * @param key the key
     * @returns the value for this key if found, else null
    */
    public getRouteParam<T>(key: string): T | null {
        const route = this.getDeepestRoute(this.router.routerState.snapshot.root);
        const routeParams = Object.entries(route.params)
            .reduce((obj: { [k: string]: any }, [k, v]) => { obj[k] = v; return obj; }, {});
        if (key in routeParams) {
            return routeParams[key] as T;
        }
        return null;
    }

    private getDeepestRoute(routeSnapshot: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
        while (routeSnapshot.firstChild) {
            routeSnapshot = routeSnapshot.firstChild;
        }
        return routeSnapshot;
    }
}
