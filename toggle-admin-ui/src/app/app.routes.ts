import { Routes } from '@angular/router';
import { ClientsPage } from './features/clients/clients-page';
import { ToggleFormPage } from './features/toggles/toggle-form-page';
import { TogglesPage } from './features/toggles/toggles-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'toggles' },
  { path: 'toggles/new', component: ToggleFormPage },
  { path: 'toggles/:name/edit', component: ToggleFormPage },
  { path: 'toggles', pathMatch: 'full', component: TogglesPage },
  { path: 'clients', component: ClientsPage }
];
