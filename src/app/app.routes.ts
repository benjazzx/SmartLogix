import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: 'login',     loadComponent: () => import('./features/login/login.component').then(m => m.LoginComponent)       },
  { path: 'registro',  loadComponent: () => import('./features/registro/registro.component').then(m => m.RegistroComponent) },
  { path: 'recuperar', loadComponent: () => import('./features/recuperar/recuperar.component').then(m => m.RecuperarComponent) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        data: { roles: ['admin', 'bodeguero', 'transportista', 'cliente'] },
        canActivate: [roleGuard],
      },
      {
        path: 'productos',
        loadComponent: () => import('./features/productos/productos.component').then(m => m.ProductosComponent),
        data: { roles: ['admin', 'bodeguero'] },
        canActivate: [roleGuard],
      },
      {
        path: 'inventario',
        loadComponent: () => import('./features/inventario/inventario.component').then(m => m.InventarioComponent),
        data: { roles: ['admin', 'bodeguero'] },
        canActivate: [roleGuard],
      },
      {
        path: 'ordenes',
        loadComponent: () => import('./features/ordenes/ordenes.component').then(m => m.OrdenesComponent),
        data: { roles: ['admin', 'bodeguero', 'cliente'] },
        canActivate: [roleGuard],
      },
      {
        path: 'envios',
        loadComponent: () => import('./features/envios/envios.component').then(m => m.EnviosComponent),
        data: { roles: ['admin', 'transportista'] },
        canActivate: [roleGuard],
      },
      {
        path: 'usuarios',
        loadComponent: () => import('./features/usuarios/usuarios.component').then(m => m.UsuariosComponent),
        data: { roles: ['admin'] },
        canActivate: [roleGuard],
      },
      {
        path: 'perfil',
        loadComponent: () => import('./features/perfil/perfil.component').then(m => m.PerfilComponent),
        data: { roles: ['cliente'] },
        canActivate: [roleGuard],
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
