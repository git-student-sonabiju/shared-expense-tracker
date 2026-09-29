// Mirrors the backend DTOs in com.sharedexpenses.web.dto.Responses.
// Amounts arrive as JSON numbers with at most two decimal places.

export interface GroupSummary {
  id: number;
  name: string;
  memberCount: number;
  createdAt: string;
}

export interface Member {
  id: number;
  name: string;
}

export interface Group {
  id: number;
  name: string;
  createdAt: string;
  members: Member[];
}

export type SplitType = 'EQUAL' | 'EXACT';

export interface Share {
  memberId: number;
  memberName: string;
  amount: number;
}

export interface Expense {
  id: number;
  description: string;
  amount: number;
  paidBy: Member;
  splitType: SplitType;
  createdAt: string;
  shares: Share[];
}

export interface Settlement {
  id: number;
  from: Member;
  to: Member;
  amount: number;
  note: string | null;
  createdAt: string;
}

export interface MemberBalance {
  memberId: number;
  memberName: string;
  /** Positive: the member is owed money. Negative: the member owes money. */
  balance: number;
}

export interface SuggestedPayment {
  fromMemberId: number;
  fromMemberName: string;
  toMemberId: number;
  toMemberName: string;
  amount: number;
}

export interface Balances {
  balances: MemberBalance[];
  suggestedPayments: SuggestedPayment[];
}

export interface NewExpense {
  description: string;
  amount: string;
  paidByMemberId: number;
  splitType: SplitType;
  splits: { memberId: number; amount?: string }[];
}

export interface NewSettlement {
  fromMemberId: number;
  toMemberId: number;
  amount: string;
  note?: string;
}

export interface User {
  id: number;
  username: string;
  displayName: string;
}

export interface Session {
  token: string;
  expiresAt: string;
  user: User;
}
