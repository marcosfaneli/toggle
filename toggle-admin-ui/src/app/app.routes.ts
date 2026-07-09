import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth.guard';
import { ClientsPage } from './features/clients/clients-page';
import { ServiceApiKeysPage } from './features/service-api-keys/service-api-keys-page';
import { ToggleFormPage } from './features/toggles/toggle-form-page';
import { TogglesPage } from './features/toggles/toggles-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'toggles' },
  { path: 'toggles/new', component: ToggleFormPage, canActivate: [authGuard, roleGuard(['ADMIN', 'MAINTAINER'])] },
  { path: 'toggles/:name/edit', component: ToggleFormPage, canActivate: [authGuard, roleGuard(['ADMIN', 'MAINTAINER'])] },
  { path: 'toggles', pathMatch: 'full', component: TogglesPage, canActivate: [authGuard] },
  { path: 'clients', component: ClientsPage, canActivate: [authGuard] },
  { path: 'service-api-keys', component: ServiceApiKeysPage, canActivate: [authGuard, roleGuard(['ADMIN'])] }
];
