# Shared Expense Tracker

Track shared expenses in a group and settle up with the fewest payments.

**Stack:** Java 21 · Spring Boot 3.5 · JPA · H2 | React 19 · TypeScript · Vite

## Features
- Sign up, log in, log out. Each account's groups are private.
- Create and delete groups; add, rename and remove members.
- Add expenses split **equally** or by **exact amounts**.
- Live balances that always sum to exactly zero.
- Suggested payments using the **minimum number of transactions**.
- Record payments. Data is stored in a database, so it survives restarts.

## Run
Requires JDK 21+, Maven, and Node 20.19+.

```bash
cd backend && mvn spring-boot:run      # API on http://localhost:8080
cd frontend && npm install && npm run dev   # UI on http://localhost:5173
```

Open http://localhost:5173 and sign up.

## Test
```bash
cd backend && mvn test
cd frontend && npm test
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

## Design
See [explanation/PROJECT_EXPLANATION.md](explanation/PROJECT_EXPLANATION.md) for the design,
including how money is stored as cents, why balances always sum to zero, the
minimum-payments algorithm, validation, and authentication.
