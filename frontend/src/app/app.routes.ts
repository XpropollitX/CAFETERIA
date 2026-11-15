import { Routes } from '@angular/router';
import { roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home-page.component')
      .then((module) => module.HomePageComponent),
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login-page.component')
      .then((module) => module.LoginPageComponent),
  },
  {
    path: 'registro',
    loadComponent: () => import('./features/auth/register-page.component')
      .then((module) => module.RegisterPageComponent),
  },
  {
    path: 'menu',
    loadComponent: () => import('./features/menu/menu-page.component')
      .then((module) => module.MenuPageComponent),
    canActivate: [roleGuard],
    data: { roles: ['CLIENTE', 'PERSONAL', 'ADMIN'] },
  },
  {
    path: 'carrito',
    loadComponent: () => import('./features/pedidos/cart-page.component')
      .then((module) => module.CartPageComponent),
    canActivate: [roleGuard],
    data: { roles: ['CLIENTE'] },
  },
  {
    path: 'cliente/pedidos',
    loadComponent: () => import('./features/pedidos/customer-orders-page.component')
      .then((module) => module.CustomerOrdersPageComponent),
    canActivate: [roleGuard],
    data: { roles: ['CLIENTE'] },
  },
  {
    path: 'personal/pedidos',
    loadComponent: () => import('./features/pedidos/staff-orders-page.component')
      .then((module) => module.StaffOrdersPageComponent),
    canActivate: [roleGuard],
    data: { roles: ['PERSONAL', 'ADMIN'] },
  },
  {
    path: 'admin/catalogo',
    loadComponent: () => import('./features/catalog/admin-catalog-page.component')
      .then((module) => module.AdminCatalogPageComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  { path: '**', redirectTo: '' },
];
