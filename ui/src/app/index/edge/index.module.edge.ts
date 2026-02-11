import { NgModule } from "@angular/core";
import { FlatWidgetButtonComponent } from "../../shared/components/flat/flat-widget-button/flat-widget-button";
import { SharedModule } from "../../shared/shared.module";
import { FilterComponent } from "../filter/filter.component";
import { LoginComponent } from "../login.component";
import { OverViewComponent } from "../overview/overview.component";
import { LoadingScreenComponent } from "../shared/loading-screen";
import { SumStateComponent } from "../shared/sumState";

/**
 * Edge-specific IndexModule that does NOT include RegistrationModule
 * This file is used for Edge builds via fileReplacements in angular.json
 */
@NgModule({
    imports: [
        SharedModule,
        // RegistrationModule is NOT imported for Edge builds
        FlatWidgetButtonComponent,
        FilterComponent,
    ],
    declarations: [
        SumStateComponent,
        LoginComponent,
        OverViewComponent,
        LoadingScreenComponent,
    ],
})
export class IndexModule { }
