# Project Explanation: Shared Expense Tracker (SplitEase)

## What it does
Users sign up and create groups. They add members, record expenses (split equally or by exact
amounts), see everyone's balance, get the **fewest payments** needed to settle up, and record
payments. All data is stored in a database, so it survives refreshes and restarts.

## Architecture
```
React (5173) ──/api──► Spring Boot (8080) ──► H2 database file
                         AuthInterceptor → Controller → Service → Repository (JPA)
```
- **Backend:** Java 21, Spring Boot 3.5, Spring Data JPA, Bean Validation, H2
- **Frontend:** React 19, TypeScript, Vite, React Router

## Data model
```
UserAccount 1─* ExpenseGroup 1─* Member
                    ├─* Expense (paidBy → Member) 1─* ExpenseShare (member, amount)
                    └─* Settlement (from → to, amount)
UserAccount 1─* AuthSession
```

## Key design decisions
1. **Money is stored as whole cents (`long`).** This avoids floating-point errors. Decimals are
   only used at the API boundary (`Money.toCents` / `fromCents`).
2. **Balances are calculated, not stored.**
   `balance = paid − own share + payments sent − payments received`
   - Each expense's shares add up to its total, and each payment adds and subtracts the same
     amount, so balances **always sum to exactly zero** and can never drift out of sync.
3. **Leftover cents in equal splits:** ₹100 ÷ 3 = 33.34 / 33.33 / 33.33. Extra cents go to the
   first members, ordered by id (`SplitCalculator`).

## Minimum payments (`DebtSimplifier`)
- Fewest payments = *n* − (largest number of subgroups whose balances sum to zero), where *n* is
  the number of people with a non-zero balance.
- **Up to 20 such people:** solved exactly with dynamic programming over subsets, O(n·2ⁿ).
  Each subgroup is then settled greedily in *s* − 1 payments.
- **More than 20:** greedy fallback, at most *n* − 1 payments.
- **Example where plain greedy loses:** balances `[-8, +6, -2, +3, +4, -3]`. Greedy needs 5
  payments; the optimum is 4. This case is covered by the tests.

## Validation and errors
- **Three layers:**
  - Bean Validation on request bodies (formats and limits)
  - service rules (e.g. shares must add up, payer ≠ recipient)
  - database unique constraints
- **One JSON error format:** `{status, message, fieldErrors}`
- **Status codes:**
  - **400:** invalid input
  - **401:** not logged in
  - **404:** not found, or another user's group
  - **409:** conflict (duplicate name, or removing a member with history)

## Authentication (an extra; the original brief listed it as out of scope)
- **Passwords:** hashed with BCrypt.
- **Tokens:** login returns a random token, and the database stores only its SHA-256 hash.
- **Logout:** deletes the session, so the token stops working immediately.
- **Enforcement:** `AuthInterceptor` guards `/api/**`, except login and register.
- **Isolation:** groups are looked up by `(id, ownerId)`, so users only see their own groups.

## Features
- Groups: create, delete
- Members: add, rename, remove (only if they have no history)
- Expenses: add, delete
- Payments: record, delete
- Balances and suggested payments
- Login, sign-up, logout

## Testing
- **Backend (26 tests):** split and debt-simplification unit tests (including 200 random cases),
  full API tests, and auth/access tests.
- **Frontend (4 tests):** money parsing.

## Possible improvements
- Flyway migrations
- PostgreSQL in Docker
- Editing expenses
- Percentage splits
- Pagination
- Sharing groups between accounts
- Limiting repeated login attempts
