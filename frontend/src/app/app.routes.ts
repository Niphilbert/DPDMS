import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards';
import { MONEY_ROLES, REPORT_ROLES } from './core/permissions';

export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], title: 'Sign in', loadComponent: () => import('./pages/auth/login.component').then((m) => m.LoginComponent) },
  { path: 'register', canActivate: [guestGuard], title: 'Create account', loadComponent: () => import('./pages/auth/register.component').then((m) => m.RegisterComponent) },
  { path: 'verify-email', canActivate: [guestGuard], title: 'Confirm email', loadComponent: () => import('./pages/auth/verify-email.component').then((m) => m.VerifyEmailComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', title: 'Dashboard', loadComponent: () => import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      {
        path: 'requests',
        title: 'Requests and quotes',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'PROJECT_MANAGER', 'CLIENT'] },
        loadComponent: () => import('./pages/requests/requests-list.component').then((m) => m.RequestsListComponent),
      },
      {
        path: 'requests/:id',
        title: 'Request',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'PROJECT_MANAGER', 'CLIENT'] },
        loadComponent: () => import('./pages/requests/request-detail.component').then((m) => m.RequestDetailComponent),
      },
      { path: 'projects', title: 'Projects', loadComponent: () => import('./pages/projects/projects-list.component').then((m) => m.ProjectsListComponent) },
      { path: 'projects/:id', title: 'Project', loadComponent: () => import('./pages/projects/project-detail.component').then((m) => m.ProjectDetailComponent) },
      {
        path: 'tasks',
        title: 'My tasks',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'PROJECT_MANAGER', 'DEVELOPER'] },
        loadComponent: () => import('./pages/tasks/my-tasks.component').then((m) => m.MyTasksComponent),
      },
      {
        path: 'invoices',
        title: 'Invoices',
        canActivate: [roleGuard],
        data: { roles: MONEY_ROLES },
        loadComponent: () => import('./pages/invoices/invoices-list.component').then((m) => m.InvoicesListComponent),
      },
      {
        path: 'invoices/:id',
        title: 'Invoice',
        canActivate: [roleGuard],
        data: { roles: MONEY_ROLES },
        loadComponent: () => import('./pages/invoices/invoice-detail.component').then((m) => m.InvoiceDetailComponent),
      },
      { path: 'renewals', title: 'Renewals', loadComponent: () => import('./pages/renewals/renewals-list.component').then((m) => m.RenewalsListComponent) },
      { path: 'renewals/:id', title: 'Renewal', loadComponent: () => import('./pages/renewals/renewal-detail.component').then((m) => m.RenewalDetailComponent) },
      { path: 'tickets', title: 'Support tickets', loadComponent: () => import('./pages/tickets/tickets-list.component').then((m) => m.TicketsListComponent) },
      { path: 'tickets/:id', title: 'Ticket', loadComponent: () => import('./pages/tickets/ticket-detail.component').then((m) => m.TicketDetailComponent) },
      {
        path: 'reports',
        title: 'Reports',
        canActivate: [roleGuard],
        data: { roles: REPORT_ROLES },
        loadComponent: () => import('./pages/reports/reports.component').then((m) => m.ReportsComponent),
      },
      { path: 'notifications', title: 'Notifications', loadComponent: () => import('./pages/notifications/notifications.component').then((m) => m.NotificationsComponent) },
      {
        path: 'admin/users',
        title: 'Users and access',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./pages/admin/admin-users.component').then((m) => m.AdminUsersComponent),
      },
      {
        path: 'admin/audit',
        title: 'Audit trail',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./pages/admin/admin-audit.component').then((m) => m.AdminAuditComponent),
      },
      { path: '**', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
