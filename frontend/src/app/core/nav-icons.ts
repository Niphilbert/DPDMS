/**
 * Presentational helpers for the menu: one outline icon per page and the group each menu item sits under.
 * Icons are 24x24 stroke paths drawn inline (no icon library). This file does not change the NAV menu itself.
 */
export const ICON = {
  dashboard: 'M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z',
  requests: 'M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9zM14 3v6h6M8 13h8M8 17h5',
  projects: 'M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
  tasks: 'M9 11l3 3 8-8M20 12v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h9',
  invoices: 'M6 2h12v20l-3-2-3 2-3-2-3 2zM9 8h6M9 12h6M9 16h3',
  renewals: 'M21 12a9 9 0 0 1-15.5 6.3M3 12A9 9 0 0 1 18.5 5.7M21 4v5h-5M3 20v-5h5',
  tickets: 'M21 11.5a8.4 8.4 0 0 1-9 8.4 8.5 8.5 0 0 1-3.8-.9L3 21l1.9-5.1A8.4 8.4 0 1 1 21 11.5z',
  reports: 'M4 20V10M10 20V4M16 20v-7M22 20H2',
  bell: 'M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6M10 19a2 2 0 0 0 4 0',
  users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8',
  shield: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10zM9 12l2 2 4-4',
  logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
  menu: 'M3 6h18M3 12h18M3 18h18',
  layers: 'M12 2l9 5-9 5-9-5zM3 12l9 5 9-5M3 17l9 5 9-5',
  clock: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20zM12 6v6l4 2',
  alert: 'M12 3l10 18H2zM12 10v5M12 18h.01',
  check: 'M20 6L9 17l-5-5',
  lock: 'M5 11h14v10H5zM8 11V7a4 4 0 0 1 8 0v4',
  rocket: 'M5 15c-1.5 1.3-2 5-2 5s3.7-.5 5-2c.7-.8.7-2.1-.1-2.9s-2.1-.8-2.9-.1zM12 15l-3-3a22 22 0 0 1 2-4 12.9 12.9 0 0 1 11-6c0 2.7-.8 7.5-6 11a22 22 0 0 1-4 2zM9 12H4s.6-3 2-4c1.6-1.1 5 0 5 0M12 15v5s3-.6 4-2c1.1-1.6 0-5 0-5',
  chat: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z',
  wallet: 'M20 12V8H6a2 2 0 0 1 0-4h12v4M4 6v12a2 2 0 0 0 2 2h14v-4M18 12a2 2 0 0 0 0 4h4v-4z',
  globe: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20zM2 12h20M12 2a15 15 0 0 1 0 20 15 15 0 0 1 0-20z',
  edit: 'M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z',
  eye: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12zM12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
} as const;

export type IconName = keyof typeof ICON;

/** The icon for a menu path (and a neutral fallback). */
export const NAV_ICON: Record<string, IconName> = {
  '/dashboard': 'dashboard',
  '/requests': 'requests',
  '/projects': 'projects',
  '/tasks': 'tasks',
  '/invoices': 'invoices',
  '/renewals': 'renewals',
  '/tickets': 'tickets',
  '/reports': 'reports',
  '/notifications': 'bell',
  '/admin/users': 'users',
  '/admin/audit': 'shield',
};

export const NAV_GROUP_ORDER = ['Overview', 'Delivery', 'Billing and care', 'Administration'] as const;

/** The sidebar heading each menu path is listed under. */
export const NAV_GROUP: Record<string, (typeof NAV_GROUP_ORDER)[number]> = {
  '/dashboard': 'Overview',
  '/notifications': 'Overview',
  '/requests': 'Delivery',
  '/projects': 'Delivery',
  '/tasks': 'Delivery',
  '/invoices': 'Billing and care',
  '/renewals': 'Billing and care',
  '/tickets': 'Billing and care',
  '/reports': 'Billing and care',
  '/admin/users': 'Administration',
  '/admin/audit': 'Administration',
};

export function navIcon(path: string): string {
  return ICON[NAV_ICON[path] ?? 'dashboard'];
}
