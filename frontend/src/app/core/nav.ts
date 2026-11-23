import { Role } from './models';

export interface NavItem {
  label: string;
  path: string;
}

/**
 * The menu for each role, as in the prototype. Developers have no Invoices and no Reports (they never see money);
 * clients have no Reports.
 */
export const NAV: Record<Role, NavItem[]> = {
  ADMIN: [
    { label: 'Dashboard', path: '/dashboard' },
    { label: 'Requests and quotes', path: '/requests' },
    { label: 'Projects', path: '/projects' },
    { label: 'Invoices', path: '/invoices' },
    { label: 'Renewals', path: '/renewals' },
    { label: 'Support tickets', path: '/tickets' },
    { label: 'Reports', path: '/reports' },
    { label: 'Notifications', path: '/notifications' },
    { label: 'Users and access', path: '/admin/users' },
    { label: 'Audit trail', path: '/admin/audit' },
  ],
  PROJECT_MANAGER: [
    { label: 'Dashboard', path: '/dashboard' },
    { label: 'Requests and quotes', path: '/requests' },
    { label: 'Projects', path: '/projects' },
    { label: 'My tasks', path: '/tasks' },
    { label: 'Invoices', path: '/invoices' },
    { label: 'Renewals', path: '/renewals' },
    { label: 'Support tickets', path: '/tickets' },
    { label: 'Reports', path: '/reports' },
    { label: 'Notifications', path: '/notifications' },
  ],
  DEVELOPER: [
    { label: 'Dashboard', path: '/dashboard' },
    { label: 'My tasks', path: '/tasks' },
    { label: 'Projects', path: '/projects' },
    { label: 'Renewals', path: '/renewals' },
    { label: 'My tickets', path: '/tickets' },
    { label: 'Notifications', path: '/notifications' },
  ],
  CLIENT: [
    { label: 'Dashboard', path: '/dashboard' },
    { label: 'Requests and quotes', path: '/requests' },
    { label: 'My projects', path: '/projects' },
    { label: 'Invoices', path: '/invoices' },
    { label: 'My websites and renewals', path: '/renewals' },
    { label: 'Support', path: '/tickets' },
    { label: 'Notifications', path: '/notifications' },
  ],
};

export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: 'Administrator',
  PROJECT_MANAGER: 'Project manager',
  DEVELOPER: 'Developer',
  CLIENT: 'Client',
};

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('');
}
