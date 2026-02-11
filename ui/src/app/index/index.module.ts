import { NgModule } from "@angular/core";
import { IS_BACKEND_BUILD } from "src/environments/buildtime/backend-type";
import { FlatWidgetButtonComponent } from "../shared/components/flat/flat-widget-button/flat-widget-button";
import { SharedModule } from "./../shared/shared.module";
import { FilterComponent } from "./filter/filter.component";
import { LoginComponent } from "./login.component";
import { OverViewComponent } from "./overview/overview.component";
import { RegistrationModule } from "./registration/registration.module";
import { LoadingScreenComponent } from "./shared/loading-screen";
import { SumStateComponent } from "./shared/sumState";

@NgModule({
    imports: [
        SharedModule,
        // RegistrationModule is only needed for Backend builds (user registration feature)
        // It will be tree-shaken out of Edge builds through compile-time elimination
        ...(IS_BACKEND_BUILD ? [RegistrationModule] : []),
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
