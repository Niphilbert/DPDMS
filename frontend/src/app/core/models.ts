/** Shapes of the data exchanged with the backend. Mirrors the Java DTOs (see backend/docs/api.md). */

export type Role = 'ADMIN' | 'PROJECT_MANAGER' | 'DEVELOPER' | 'CLIENT';
export type StaffRole = Exclude<Role, 'CLIENT'>;
export type UserStatus = 'PENDING_VERIFICATION' | 'INVITED' | 'ACTIVE' | 'DEACTIVATED' | string;

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  roleLabel: string;
  status: UserStatus;
  companyId: string | null;
  companyName: string | null;
  selfRegistered: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export type AuthStatus = 'AUTHENTICATED' | 'CODE_REQUIRED' | 'PASSWORD_CHANGE_REQUIRED' | 'VERIFICATION_REQUIRED';

/** What the backend answers to every sign-in step. It says what the screen should do next. */
export interface AuthResult {
  status: AuthStatus;
  token?: string;
  challenge?: string;
  user?: User;
  email?: string;
  /** Only present in development (backend app.expose-secrets). Never shown otherwise. */
  devCode?: string;
}

export interface RegisterRequest {
  companyName: string;
  fullName: string;
  email: string;
  phone: string;
  password: string;
  acceptTerms: boolean;
}

export interface AppNotification {
  id: string;
  message: string;
  link: string | null;
  createdAt: string;
  read: boolean;
}

export interface NotificationList {
  unread: number;
  items: AppNotification[];
}

export interface AuditEntry {
  id: string;
  occurredAt: string;
  actorName: string;
  action: string;
  subject: string;
  ref: string;
}

export interface StaffCreated {
  user: User;
  /** Only present in development; in production it is e-mailed to the new person. */
  temporaryPassword?: string;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  fields: Record<string, string>;
}

// ---------------------------------------------------------------- phase 2

export type RequestStatus = 'NEW' | 'QUOTED' | 'CHANGES_REQUESTED' | 'ACCEPTED' | 'DECLINED';

export interface QuoteItem {
  description: string;
  amount: number;
}

export interface Quote {
  items: QuoteItem[];
  total: number;
  weeks: number;
  validUntil: string;
  sentAt: string;
  quotedByName: string;
}

export interface ProjectRequest {
  id: string;
  companyId: string;
  companyName: string;
  title: string;
  description: string;
  budget: string | null;
  deadline: string;
  status: RequestStatus;
  changeNote: string | null;
  quote: Quote | null;
  projectId: string | null;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface NewRequest {
  title: string;
  description: string;
  budget: string;
  deadline: string;
}

export interface QuoteInput {
  items: QuoteItem[];
  weeks: number;
  validUntil: string;
}

export type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'IN_REVIEW' | 'LAUNCHED';
export type TaskStatus = 'TODO' | 'DOING' | 'REVIEW' | 'DONE';

export interface Phase {
  index: number;
  name: string;
  progress: number | null;
  taskCount: number;
}

/** A task. Clients only receive id, phase, title, status, dueDate and late; empty fields are omitted. */
export interface Task {
  id: string;
  phase: number;
  title: string;
  status: TaskStatus;
  dueDate?: string;
  late: boolean;
  plannedHours?: number;
  assigneeId?: string;
  assigneeName?: string;
  loggedHours?: number;
  submissionFile?: string;
  submissionNote?: string;
  submittedAt?: string;
  submittedByName?: string;
  sentBackNote?: string;
  sentBackAt?: string;
}

/** A project. Developers never receive budget or openApprovals; clients never receive requestId or hours. */
export interface Project {
  id: string;
  requestId?: string;
  name: string;
  status: ProjectStatus;
  companyId: string;
  companyName: string;
  managerId: string;
  managerName: string;
  startDate: string | null;
  dueDate: string | null;
  budget?: number;
  progress: number;
  phases: Phase[];
  plannedHours?: number;
  loggedHours?: number;
  lateTasks: number;
  openApprovals?: number;
  tasks?: Task[];
  createdAt: string;
}

export interface MyTask {
  task: Task;
  projectId: string;
  projectName: string;
  projectStatus: ProjectStatus;
}

export interface NewTask {
  title: string;
  phase: number;
  plannedHours: number;
  dueDate: string;
  assigneeId?: string;
}

export interface TimeEntry {
  id: string;
  taskId: string;
  taskTitle: string;
  userId: string;
  userName: string;
  date: string;
  hours: number;
  note: string | null;
  createdAt: string;
}

export interface TimeLogged {
  entry: TimeEntry;
  taskLoggedHours: number;
  taskPlannedHours: number;
}

export interface TimeSummary {
  plannedHours: number;
  loggedHours: number;
  tasks: { taskId: string; title: string; assigneeId: string | null; assigneeName: string | null; plannedHours: number; loggedHours: number }[];
  entries: TimeEntry[];
}

export interface ProjectUpdate {
  id: string;
  message: string;
  createdAt: string;
}

export type ApprovalType = 'DESIGN' | 'PAGE_CONTENT' | 'DOCUMENT';
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'CHANGES_REQUESTED';

export interface Approval {
  id: string;
  projectId: string;
  taskId: string | null;
  item: string;
  type: ApprovalType;
  file: string;
  version: number;
  message: string | null;
  status: ApprovalStatus;
  answerNote: string | null;
  sentBy: string;
  sentByName: string;
  sentAt: string;
  answeredByName: string | null;
  answeredAt: string | null;
}

export interface NewApproval {
  item: string;
  type: ApprovalType;
  file: string;
  message?: string;
  taskId?: string;
}

export type ChangeStatus = 'PROPOSED' | 'ACCEPTED' | 'DECLINED';

export interface ChangeRequest {
  id: string;
  projectId: string;
  title: string;
  cost: number;
  status: ChangeStatus;
  proposedByName: string;
  createdAt: string;
  answeredByName: string | null;
  answeredAt: string | null;
}

export interface Access {
  userId: string;
  fullName: string;
  email: string;
  grantedAt: string;
}

// ---------------------------------------------------------------- stage 3 (docs/api-stage3.md)

export interface Company {
  id: string;
  name: string;
}

export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PARTIALLY_PAID' | 'PAID' | 'VOID';
/** What the screens show: the stored status, or OVERDUE when a sent or part-paid invoice is past its due date. */
export type InvoiceDisplayStatus = InvoiceStatus | 'OVERDUE';
export type PaymentMethod = 'MOBILE_MONEY' | 'BANK_TRANSFER' | 'CASH';

export interface InvoiceSummary {
  id: string;
  number: string;
  projectId: string | null;
  projectName: string | null;
  companyId: string;
  companyName: string;
  status: InvoiceStatus;
  displayStatus: InvoiceDisplayStatus;
  overdue: boolean;
  daysOverdue: number | null;
  issueDate: string;
  dueDate: string;
  taxRate: number;
  subtotal: number;
  taxAmount: number;
  total: number;
  paidAmount: number;
  balance: number;
  note: string | null;
  createdByName: string;
  createdAt: string;
  sentAt: string | null;
  voidedAt: string | null;
  /** Always null for clients. */
  voidReason: string | null;
}

export interface InvoiceLine {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Payment {
  id: string;
  amount: number;
  paidOn: string;
  method: PaymentMethod;
  reference: string | null;
  recordedByName: string;
  recordedAt: string;
  reversed: boolean;
  reversedAt: string | null;
  /** Null for clients. */
  reversedByName: string | null;
  /** Null for clients. */
  reverseReason: string | null;
}

export interface Invoice extends InvoiceSummary {
  lines: InvoiceLine[];
  payments: Payment[];
}

export interface InvoiceLineInput {
  description: string;
  quantity: number;
  unitPrice: number;
}

export interface InvoiceInput {
  projectId?: string;
  companyId?: string;
  issueDate: string;
  dueDate: string;
  taxRate: number;
  note?: string;
  lines: InvoiceLineInput[];
}

export interface InvoiceFilter {
  status?: InvoiceDisplayStatus | '';
  projectId?: string;
  companyId?: string;
  from?: string;
  to?: string;
}

export interface InvoiceTotals {
  invoiceCount: number;
  invoiced: number;
  paid: number;
  balanceDue: number;
  openCount: number;
  overdueCount: number;
  overdueAmount: number;
}

export interface ProjectMoney {
  projectId: string;
  budget: number;
  invoiced: number;
  paid: number;
  balanceDue: number;
  unbilled: number;
  overdueAmount: number;
}

export interface SuggestedLine extends InvoiceLineInput {
  lineTotal: number;
}

export interface InvoiceSuggestion {
  projectId: string;
  projectName: string;
  companyId: string;
  hourlyRate: number;
  loggedHours: number;
  budget: number;
  invoiced: number;
  unbilled: number;
  timeLine: SuggestedLine | null;
  balanceLine: SuggestedLine | null;
}

export type RenewalType = 'DOMAIN' | 'HOSTING' | 'SSL' | 'MAINTENANCE';
export type RenewalStatus = 'ACTIVE' | 'EXPIRING' | 'EXPIRED';

export interface RenewalHistoryRow {
  id: string;
  renewedOn: string;
  previousExpiresOn: string;
  newExpiresOn: string;
  price: number | null;
  note: string | null;
  renewedByName: string;
}

/** `price` is omitted for developers, `note` for clients, `history` on list rows. */
export interface Renewal {
  id: string;
  projectId: string;
  projectName: string;
  companyId: string;
  companyName: string;
  type: RenewalType;
  label: string;
  provider: string | null;
  expiresOn: string;
  daysLeft: number;
  status: RenewalStatus;
  price?: number;
  note?: string | null;
  lastRenewedOn: string | null;
  renewalCount: number;
  history?: RenewalHistoryRow[];
  createdAt: string;
  updatedAt: string;
}

export interface RenewalInput {
  projectId?: string;
  type: RenewalType;
  label: string;
  provider?: string;
  expiresOn: string;
  price: number;
  note?: string;
}

export interface RenewalFilter {
  status?: RenewalStatus | '';
  type?: RenewalType | '';
  companyId?: string;
  projectId?: string;
  expiringWithinDays?: number | null;
}

export interface ReminderRun {
  today: string;
  checked: number;
  sent: number;
  reminders: { renewalId: string; type: RenewalType; label: string; projectName: string; expiresOn: string; daysLeft: number; threshold: number }[];
}

export type TicketPriority = 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_CLIENT' | 'RESOLVED' | 'CLOSED';
export type SlaState = 'ON_TRACK' | 'AT_RISK' | 'BREACHED' | 'MET' | 'MISSED';

export interface TicketSummary {
  id: string;
  number: string;
  subject: string;
  priority: TicketPriority;
  status: TicketStatus;
  companyId: string;
  companyName: string;
  projectId: string | null;
  projectName: string | null;
  createdById: string;
  createdByName: string;
  assigneeId: string | null;
  assigneeName: string | null;
  slaHours: number;
  createdAt: string;
  dueAt: string;
  breached: boolean;
  /** Negative once the deadline has passed; null once the ticket is resolved or closed. */
  minutesLeft: number | null;
  /** Null for a ticket closed without being resolved. */
  slaState: SlaState | null;
  resolvedAt: string | null;
  /** The contract text says "true | false | null" (a JSON boolean or null). */
  resolvedWithinSla: boolean | null;
  closedAt: string | null;
  reopenCount: number;
  commentCount: number;
  updatedAt: string;
}

export interface TicketComment {
  id: string;
  authorId: string;
  authorName: string;
  authorRole: Role;
  body: string;
  internal: boolean;
  createdAt: string;
}

export interface Ticket extends TicketSummary {
  description: string;
  comments: TicketComment[];
}

export interface TicketFilter {
  status?: TicketStatus | '';
  priority?: TicketPriority | '';
  projectId?: string;
  companyId?: string;
  assigneeId?: string;
  unassigned?: boolean;
  breached?: boolean;
  mine?: boolean;
}

export interface NewTicket {
  subject: string;
  description: string;
  priority: TicketPriority;
  projectId?: string;
}

export interface SlaRun {
  now: string;
  checked: number;
  atRisk: number;
  breached: number;
  alerts: { ticketId: string; number: string; kind: 'AT_RISK' | 'BREACHED'; assigneeName: string | null }[];
}

export interface ReportSummary {
  from: string | null;
  to: string | null;
  generatedAt: string;
  hourlyRate: number;
  revenue: {
    invoiced: number;
    invoiceCount: number;
    paid: number;
    outstanding: number;
    overdue: number;
    overdueCount: number;
    byClient: { companyId: string; companyName: string; invoiced: number; paid: number }[];
  };
  projects: { total: number; byStatus: Record<ProjectStatus, number>; averageProgress: number | null };
  tickets: {
    total: number;
    open: number;
    breached: number;
    resolved: number;
    resolvedWithinSlaPercent: number | null;
    averageResolutionHours: number | null;
    byPriority: { priority: TicketPriority; total: number; open: number; breached: number }[];
  };
  renewals: {
    total: number;
    expiring: number;
    expired: number;
    byType: { type: RenewalType; active: number; expiring: number; expired: number }[];
  };
  hours: { total: number; byDeveloper: { userId: string; fullName: string; hours: number }[] };
  projectProfit: {
    projectId: string;
    projectName: string;
    companyName: string;
    status: ProjectStatus;
    budget: number;
    loggedHours: number;
    costSoFar: number;
    plannedHours: number;
    forecastCost: number;
    forecastMarginPercent: number | null;
  }[];
}

export interface MonthRevenue {
  /** YYYY-MM */
  month: string;
  invoiced: number;
  paid: number;
}

/** `GET /api/dashboard/stage3`: a block the caller may not see is omitted. */
export interface Stage3Dashboard {
  invoices?: { unpaidCount: number; unpaidAmount: number; overdueCount: number; overdueAmount: number; draftCount?: number };
  renewals?: { expiringCount: number; expiredCount: number };
  tickets?: {
    openCount: number;
    breachedCount?: number;
    atRiskCount?: number;
    unassignedCount?: number;
    waitingOnClientCount?: number;
  };
}
