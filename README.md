# 🏦 Enterprise Banking System

> **Educational project** — This is a simulated banking application built for learning purposes. It does not handle real money or real financial transactions.

A full-stack enterprise banking application with a **Spring Boot 4** REST API backend and a **React + Vite** frontend, backed by **PostgreSQL on Neon**.

---

## ✨ Features

### Customer
- Register and log in with JWT authentication
- Create bank accounts (auto-generated account number)
- Deposit and withdraw funds — every operation (including failures) creates a transaction record
- Transfer funds between accounts with **double-entry ledger** (debit + credit entries)
- **Idempotent transfers** — submit the same `idempotencyKey` twice and get the same result without double-charging
- View full transaction and transfer history

### Admin
- Secure admin dashboard (ROLE_ADMIN only)
- System-wide summary statistics (total customers, balances, transactions today, etc.)
- Paginated list of customers, accounts, transactions, and audit logs
- Freeze, close, or re-activate any account
- Full audit log of all significant actions

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend framework | Spring Boot 4.1.2 |
| Language | Java 17 |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL (hosted on [Neon](https://neon.tech)) |
| Security | Spring Security 7, JWT (custom HS256 implementation) |
| Build | Maven 3.9 |
| Frontend framework | React 19 + Vite 6 |
| Styling | Tailwind CSS v4 |
| HTTP client | Axios |
| Routing | React Router v7 |
| API docs | SpringDoc OpenAPI 2.8.3 (Swagger UI) |
| Containerisation | Docker + Docker Compose |
| Testing | JUnit 5, Mockito, Spring Boot Test, H2 (PostgreSQL mode) |

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  React Frontend (Vite)                                       │
│  Auth → Dashboard → Account Details → Admin Dashboard        │
└──────────────────────┬───────────────────────────────────────┘
                       │  HTTP (JWT Bearer)
┌──────────────────────▼───────────────────────────────────────┐
│  Spring Boot REST API                                        │
│  AuthController · AccountController · TransactionController  │
│  TransferController · AdminController                        │
│                                                              │
│  Security: JWT filter → role-based access → ownership check  │
│  Transactions: pessimistic write lock on Account row         │
│  Transfers: deadlock-safe (lower account ID locked first)    │
│  Ledger: double-entry (DEBIT + CREDIT per transfer)         │
│  Audit: AuditLog entity written on every mutating action     │
└──────────────────────┬───────────────────────────────────────┘
                       │  JDBC / JPA
┌──────────────────────▼───────────────────────────────────────┐
│  PostgreSQL (Neon)                                           │
│  customers · accounts · transactions · ledger_entries        │
│  transfers · audit_logs                                      │
└──────────────────────────────────────────────────────────────┘
```

---

## 📁 Folder Structure

```
D:\Banking System\
├── .env                   # Real secrets — never committed
├── .env.example           # Template with fake values
├── .gitignore
├── docker-compose.yml
├── README.md
│
├── banking_system/        # Spring Boot backend
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/bank/banking_system/
│       │   ├── config/        # Security, OpenAPI, DataInitializer
│       │   ├── controller/    # REST controllers
│       │   ├── dto/           # Request / Response DTOs
│       │   ├── entity/        # JPA entities and enums
│       │   ├── exception/     # GlobalExceptionHandler
│       │   ├── repository/    # Spring Data repositories
│       │   └── service/       # Business logic
│       └── test/              # JUnit 5 tests (H2 in-memory)
│
└── banking-ui/            # React + Vite frontend
    ├── Dockerfile
    ├── nginx.conf
    └── src/
        ├── api/           # Axios instance + API functions
        ├── context/       # AuthContext (JWT storage)
        ├── components/    # Reusable UI components
        ├── pages/         # Route-level page components
        └── routes/        # ProtectedRoute, AdminRoute
```

---

## 🚀 Running Locally (without Docker)

### Prerequisites
- Java 17+
- Maven 3.9+ (or use the included `mvnw` wrapper)
- Node.js 20+
- A `.env` file in `D:\Banking System` (see `.env.example`)

### Backend

```bash
# Windows — load env vars from .env first, then run
cd "D:\Banking System\banking_system"

# Option A: set env vars in your shell, then:
mvn spring-boot:run

# Option B: pass them inline (PowerShell)
$env:DB_URL="..."; $env:DB_USERNAME="..."; ... mvn spring-boot:run
```

> **DNS note (Windows ISP quirk)**: If Neon hostname doesn't resolve, run with:
> ```
> mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Djdk.net.hosts.file=C:/path/to/custom_hosts"
> ```

The backend starts on **http://localhost:8080**.  
Swagger UI: **http://localhost:8080/swagger-ui.html**

### Frontend

```bash
cd "D:\Banking System\banking-ui"
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**.

---

## 🐳 Running with Docker Compose

### Prerequisites
- Docker Desktop (running)
- `.env` file in `D:\Banking System` (copy from `.env.example` and fill real values)

```bash
cd "D:\Banking System"
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

> **Note**: The backend healthcheck polls `/actuator/health` every 30 s. The frontend container waits until the backend is healthy before starting nginx.

To stop:
```bash
docker compose down
```

---

## 🔐 Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_URL` | JDBC URL for PostgreSQL (e.g. Neon) | ✅ |
| `DB_USERNAME` | Database username | ✅ |
| `DB_PASSWORD` | Database password | ✅ |
| `JWT_SECRET` | HS256 signing secret — **min 32 chars** | ✅ |
| `ADMIN_EMAIL` | Email for the seeded admin user | ✅ (default: `admin@bank.com`) |
| `ADMIN_PASSWORD` | Password for the seeded admin user | ✅ |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | Optional (default: `http://localhost:5173,http://localhost:3000`) |

---

## 🧪 Running Tests

Tests use **H2 in-memory database** (PostgreSQL compatibility mode) — no Neon connection required.

```bash
cd "D:\Banking System\banking_system"
mvn test -Dspring.profiles.active=test
```

Expected output: `BUILD SUCCESS` with all tests passing.

---

## 📡 API Overview

| Tag | Method | Path | Description |
|-----|--------|------|-------------|
| **Auth** | POST | `/api/auth/register` | Register a new customer |
| **Auth** | POST | `/api/auth/login` | Login → JWT token |
| **Accounts** | POST | `/api/accounts` | Create a bank account |
| **Accounts** | GET | `/api/accounts/me` | List my accounts |
| **Accounts** | GET | `/api/accounts/{accountNumber}` | Get account details |
| **Transactions** | POST | `/api/accounts/{accountNumber}/deposit` | Deposit funds |
| **Transactions** | POST | `/api/accounts/{accountNumber}/withdraw` | Withdraw funds |
| **Transactions** | GET | `/api/accounts/{accountNumber}/transactions` | Transaction history |
| **Transfers** | POST | `/api/transfers` | Transfer between accounts |
| **Transfers** | GET | `/api/accounts/{accountNumber}/transfers` | Transfer history |
| **Admin** | GET | `/api/admin/summary` | Dashboard statistics |
| **Admin** | GET | `/api/admin/customers` | Paginated customer list |
| **Admin** | GET | `/api/admin/accounts` | Paginated account list |
| **Admin** | PATCH | `/api/admin/accounts/{accountNumber}/status` | Update account status |
| **Admin** | GET | `/api/admin/transactions` | Paginated transaction list |
| **Admin** | GET | `/api/admin/audit-logs` | Paginated audit log |

Full interactive docs: **http://localhost:8080/swagger-ui.html**  
Click **Authorize → Bearer &lt;token&gt;** to test protected endpoints.

---

## 🔒 Security Notes

- **Passwords**: BCrypt hashed — never stored or returned in plain text
- **JWT**: Custom HS256 implementation; tokens expire after 24 hours
- **RBAC**: `ROLE_CUSTOMER` vs `ROLE_ADMIN` enforced at the security filter layer
- **Ownership**: Customers can only access their own accounts — enforced in service layer
- **Pessimistic locking**: Account rows are locked with `PESSIMISTIC_WRITE` during balance updates to prevent concurrent corruption
- **Deadlock safety**: Transfers always acquire locks in ascending account-ID order
- **Idempotency**: Transfers with the same `idempotencyKey` return the original result without re-executing
- **Audit log**: Every mutating operation (deposit, withdraw, transfer, status change, login) is recorded in `audit_logs`

---

## 📸 Screenshots

> _[Screenshot: Login page]_

> _[Screenshot: Customer Dashboard — account cards]_

> _[Screenshot: Account Details — Deposit / Withdraw / Transfer tabs]_

> _[Screenshot: Transaction history]_

> _[Screenshot: Admin Dashboard — Overview]_

> _[Screenshot: Admin — Accounts table with status management]_

> _[Screenshot: Swagger UI]_
