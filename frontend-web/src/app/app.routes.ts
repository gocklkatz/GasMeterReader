import { Routes } from '@angular/router';
import { ReadingUploadComponent } from './reading-upload/reading-upload';
import { ReadingsBrowseComponent } from './readings-browse/readings-browse';
import { LoginComponent } from './auth/login/login';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: ReadingUploadComponent, canActivate: [authGuard] },
  { path: 'browse', component: ReadingsBrowseComponent, canActivate: [authGuard] }
];
