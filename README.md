# Shared Expense Tracker

Track shared expenses in a group and settle up with the fewest payments.

**Stack:** Java 21 · Spring Boot 3.5 · JPA · H2 | React 19 · TypeScript · Vite

## Features
- Sign up, log in, log out. Each account's groups are private.
- Create and delete groups; add, rename and remove members. A member can only be removed if
  they have no expenses or payments, which keeps past records intact.
- Add expenses split **equally** or by **exact amounts**.
- Live balances that always sum to exactly zero.
- Suggested payments using the **minimum number of transactions**.
- Record payments. Data is stored in a database, so it survives restarts.

## Run
Requires JDK 21+, Maven, and Node 20.19+. Use **two terminals**, because each command keeps
running.

```bash
# Terminal 1: API on http://localhost:8080
cd backend
mvn spring-boot:run
```

```bash
# Terminal 2: UI on http://localhost:5173
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and sign up.

**Where data is stored:** in `data/expenses.mv.db`, inside the folder you start the backend from.
Starting it from `backend/` (as above) uses `backend/data/`. Starting it from an IDE whose
working directory is the project root uses `data/` at the root, which is a separate, empty
database. Always start the backend from the same folder. To reset all data, stop the backend
and delete the `data` folder.

**Currency:** amounts are shown in ₹ (INR) by default. To use another currency, set it in
`frontend/.env`, e.g. `VITE_CURRENCY=USD`.

## Test
From the project root:

```bash
(cd backend && mvn test)
(cd frontend && npm test)
```

## API (all under `/api`, Bearer token required except register/login)

| Endpoint | Purpose |
|---|---|
| `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me` | Accounts |
| `GET/POST /groups`, `GET/DELETE /groups/{id}` | Groups |
| `POST /groups/{id}/members`, `PATCH/DELETE /groups/{id}/members/{mid}` | Members |
| `GET/POST /groups/{id}/expenses`, `DELETE .../expenses/{eid}` | Expenses |
| `GET/POST /groups/{id}/settlements`, `DELETE .../settlements/{sid}` | Payments |
| `GET /groups/{id}/balances` | Balances and suggested payments |

## How it works

1. **Create a group** and add the people sharing costs.
2. **Add expenses.** Choose who paid and who shares it.
   - **Equal split:** the amount is divided evenly. Leftover cents go to the first members, so
     ₹100 ÷ 3 = 33.34 + 33.33 + 33.33.
   - **Exact split:** you enter each person's share, and the shares must add up to the total.
3. **Check balances.** A positive balance means the member **gets money back**; a negative one
   means they **owe**. All balances always add up to exactly zero.
4. **Settle up.** The app lists the fewest payments that clear every debt. Click **Record** when
   a payment is made, and the balances update.

### Example
Asha pays ₹900 for dinner, split equally between Asha, Ben and Chitra. Then Ben pays ₹300 for a
taxi shared by Ben and Chitra.

| Member | Paid | Share | Balance |
|---|---|---|---|
| Asha | 900 | 300 | **+600** (gets back) |
| Ben | 300 | 450 | **−150** (owes) |
| Chitra | 0 | 450 | **−450** (owes) |

Suggested payments: **Chitra → Asha ₹450** and **Ben → Asha ₹150**. That's 2 payments, the minimum.

## Project structure

```
backend/   Spring Boot REST API
  domain/        JPA entities (group, member, expense, share, settlement, user, session)
  repository/    Database access with Spring Data JPA
  service/       Business rules and validation
  settlement/    Split and minimum-payment algorithms (pure Java)
  auth/          Login, logout, token check
  web/           Controllers, request/response DTOs, error handling
frontend/  React + TypeScript client
  pages/         Login, group list, group details
  components/    Expense form, balances, members, payments
  api.ts         All calls to the backend
explanation/     Detailed design notes
```

## Example request

```http
POST /api/groups/1/expenses
Authorization: Bearer <token>
Content-Type: application/json

{ "description": "Dinner", "amount": "900.00", "paidByMemberId": 1, "splitType": "EQUAL",
  "splits": [{ "memberId": 1 }, { "memberId": 2 }, { "memberId": 3 }] }
```

Invalid input gets a clear error message. This example is abridged: the full response also
includes `timestamp` and `error`, and `fieldErrors` when a specific field is at fault.

```json
{ "status": 400, "message": "splits must add up to the expense amount 50.00 but add up to 40.00" }
```

The API returns **400** for invalid input, **401** when you're not logged in, **404** when
something isn't found, and **409** for conflicts such as a duplicate member name.

## Key design decisions
- **Money is stored as whole cents (`long`),** not floating point, so there are no rounding
  errors.
- **Balances are calculated from expenses and payments** every time, never stored, so they
  can't drift out of sync.
- **Minimum payments:** for groups of up to 20 people with non-zero balances, an exact
  dynamic-programming algorithm finds the true minimum. Larger groups use a greedy fallback.
- **Security:**
  - passwords are hashed with BCrypt
  - login tokens are random, and only their hash is stored
  - logging out ends the session immediately
  - users can only see their own groups

See [explanation/PROJECT_EXPLANATION.md](explanation/PROJECT_EXPLANATION.md) for the full design
notes.
