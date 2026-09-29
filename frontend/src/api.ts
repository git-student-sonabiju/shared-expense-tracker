import type {
  Balances,
  Expense,
  Group,
  GroupSummary,
  Member,
  NewExpense,
  NewSettlement,
  Session,
  Settlement,
  User,
} from './types';

export interface FieldError {
  field: string;
  message: string;
}

/** An error response from the API, carrying the server's message and any per-field details. */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly fieldErrors: FieldError[] = [],
  ) {
    super(message);
  }
}

const BASE = import.meta.env.VITE_API_BASE_URL ?? '/api';
const TOKEN_KEY = 'splitease.token';

// The session token lives in localStorage so a page refresh keeps the user logged in.
export const tokenStore = {
  get(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set(token: string | null) {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token);
      else localStorage.removeItem(TOKEN_KEY);
    } catch {
      /* storage unavailable (private mode); the session just won't survive a refresh */
    }
  },
};

let onUnauthorized: () => void = () => {};

/** Called whenever an authenticated request comes back 401 (expired or revoked session). */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = tokenStore.get();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(BASE + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running?', 0);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && token && !path.startsWith('/auth/')) {
      onUnauthorized();
    }
    throw new ApiError(data?.message ?? `Request failed (${response.status})`, response.status, data?.fieldErrors ?? []);
  }
  return data as T;
}

export const api = {
  register: (username: string, displayName: string, password: string) =>
    request<Session>('POST', '/auth/register', { username, displayName, password }),
  login: (username: string, password: string) => request<Session>('POST', '/auth/login', { username, password }),
  logout: () => request<void>('POST', '/auth/logout'),
  me: () => request<User>('GET', '/auth/me'),

  listGroups: () => request<GroupSummary[]>('GET', '/groups'),
  createGroup: (name: string, members: string[]) => request<Group>('POST', '/groups', { name, members }),
  getGroup: (groupId: number) => request<Group>('GET', `/groups/${groupId}`),
  deleteGroup: (groupId: number) => request<void>('DELETE', `/groups/${groupId}`),

  addMember: (groupId: number, name: string) => request<Member>('POST', `/groups/${groupId}/members`, { name }),
  renameMember: (groupId: number, memberId: number, name: string) =>
    request<Member>('PATCH', `/groups/${groupId}/members/${memberId}`, { name }),
  removeMember: (groupId: number, memberId: number) =>
    request<void>('DELETE', `/groups/${groupId}/members/${memberId}`),

  listExpenses: (groupId: number) => request<Expense[]>('GET', `/groups/${groupId}/expenses`),
  addExpense: (groupId: number, expense: NewExpense) =>
    request<Expense>('POST', `/groups/${groupId}/expenses`, expense),
  deleteExpense: (groupId: number, expenseId: number) =>
    request<void>('DELETE', `/groups/${groupId}/expenses/${expenseId}`),

  listSettlements: (groupId: number) => request<Settlement[]>('GET', `/groups/${groupId}/settlements`),
  recordSettlement: (groupId: number, settlement: NewSettlement) =>
    request<Settlement>('POST', `/groups/${groupId}/settlements`, settlement),
  deleteSettlement: (groupId: number, settlementId: number) =>
    request<void>('DELETE', `/groups/${groupId}/settlements/${settlementId}`),

  getBalances: (groupId: number) => request<Balances>('GET', `/groups/${groupId}/balances`),
};

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong';
}
