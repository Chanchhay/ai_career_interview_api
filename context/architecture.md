# Architecture

## Architectural Style

The backend is a modular monolith built with Spring Boot.

The frontend is split into two separately deployed Next.js apps:

- `apps/public-web`
- `apps/internal-console`

Both frontend apps call the same Spring Boot backend API.

## Stack

| Layer | Tool | Purpose |
|---|---|---|
| Public frontend | Next.js + TypeScript | Guest, seeker, recruiter web app |
| Internal frontend | Next.js + TypeScript | Admin, moderator, finance console |
| Frontend state | Redux Toolkit | Frontend-owned UI/application state |
| API client/cache | RTK Query | Spring Boot API fetching and caching |
| Styling | Tailwind CSS + shadcn/ui + Lucide | UI system |
| Backend | Spring Boot | Business logic and REST APIs |
| Security | Spring Security OAuth2 Resource Server | JWT validation and authorization |
| Identity | Keycloak | Users, login, roles, token issuing |
| Database | PostgreSQL | Main relational database |
| ORM | Spring Data JPA | Entity mapping and repositories |
| Migration | Flyway | Versioned database migrations |
| AI integration | Spring AI | Interview generation, scoring, feedback |
| Voice channel later | Vapi | Optional voice interface into Spring Boot |
| Local infrastructure | Docker Compose | PostgreSQL and Keycloak |

## Root Folder Structure

```text
ai-career-platform/
├── AGENTS.md
├── context/
├── backend/
│   ├── src/main/java/...
│   ├── src/main/resources/
│   ├── build.gradle or pom.xml
│   └── README.md
├── apps/
│   ├── public-web/
│   └── internal-console/
├── packages/
│   ├── shared-types/
│   ├── ui/
│   └── config/
├── docker-compose.yml
└── README.md
```

## Backend Package Structure

Use feature-based packages.

```text
backend/src/main/java/com/example/aicareerplatform/
├── AiCareerPlatformApplication.java
├── common/
│   ├── audit/
│   ├── response/
│   ├── exception/
│   └── pagination/
├── security/
├── identity/
├── admin/
├── moderator/
├── recruiter/
├── seeker/
├── company/
├── job/
├── application/
├── interview/
│   ├── ai/
│   └── human/
├── project/
├── forwarding/
├── finance/
└── notification/
```

Each feature package may contain:

```text
controller/
service/
repository/
dto/
mapper/
entity/
enums/
```

Do not use one global `controller/`, `service/`, `repository/`, `entity/` structure for the whole app.

## Frontend Applications

### Public Web

Path:

```text
apps/public-web
```

Users:

- Guest
- Job Seeker
- Recruiter

Responsibilities:

- Landing page
- Public job board
- Job detail
- Seeker registration/onboarding
- Seeker profile/resume/portfolio
- Job applications
- AI interview taking
- Application status tracking
- Recruiter company registration
- Recruiter company documents
- Recruiter job publishing
- Recruiter forwarded candidates
- Recruiter hiring report

### Internal Console

Path:

```text
apps/internal-console
```

Users:

- Admin
- Moderator
- Finance

Responsibilities:

- Admin dashboard
- User management
- Company verification queue
- Job report/takedown handling
- AI interview result review
- Human interview scheduling
- Human interview result recording
- Candidate forwarding
- Project assignment/review
- Hiring confirmation
- Commission and invoice management

## Frontend App Structure

Each app owns its own Redux store and RTK Query API layer.

Example for `apps/public-web`:

```text
apps/public-web/
├── app/
├── components/
├── features/
│   ├── auth/
│   ├── jobs/
│   ├── company/
│   ├── applications/
│   ├── interviews/
│   ├── resumes/
│   ├── portfolios/
│   └── ui/
├── lib/
│   ├── api/
│   ├── keycloak/
│   └── redux/
└── types/
```

Example for `apps/internal-console`:

```text
apps/internal-console/
├── app/
├── components/
├── features/
│   ├── auth/
│   ├── admin/
│   ├── moderation/
│   ├── interviews/
│   ├── forwarding/
│   ├── finance/
│   └── ui/
├── lib/
│   ├── api/
│   ├── keycloak/
│   └── redux/
└── types/
```

## Shared Packages

### `packages/shared-types`

Shared TypeScript DTOs for frontend apps.

Examples:

- `ApiResponse<T>`
- `PageResponse<T>`
- `CompanyResponse`
- `JobPostResponse`
- `JobApplicationResponse`
- `AiInterviewSessionResponse`
- `HumanInterviewResponse`
- `InvoiceResponse`

### `packages/ui`

Optional generic UI primitives shared by both frontend apps.

Examples:

- Button
- Card
- Badge
- Input
- StatusBadge
- PageHeader
- DataTable

Do not put role-specific UI in `packages/ui`.

### `packages/config`

Optional shared TypeScript, ESLint, Tailwind, or constants configuration.

## Deployment Model

Production:

```text
public-web.example.com        → apps/public-web
console.example.com           → apps/internal-console
api.example.com               → backend Spring Boot API
auth.example.com              → Keycloak
```

Development:

```text
localhost:3000 → apps/public-web
localhost:3001 → apps/internal-console
localhost:8080 → backend
localhost:9090 → Keycloak
localhost:5433 → PostgreSQL
```

## Keycloak Clients

| Client | Used By | Redirect URI |
|---|---|---|
| `public-web-client` | `apps/public-web` | `http://localhost:3000/*` |
| `internal-console-client` | `apps/internal-console` | `http://localhost:3001/*` |
| `backend-api` | Spring Boot API | Resource server audience/client |

## Authentication Flow

```text
User opens frontend app
        ↓
Frontend redirects to Keycloak login
        ↓
Keycloak authenticates user
        ↓
Frontend receives tokens
        ↓
Frontend calls Spring Boot API with Bearer token
        ↓
Spring Security validates JWT
        ↓
Backend applies role-based authorization
```

## Data Ownership

| Data | Source of Truth |
|---|---|
| Identity | Keycloak |
| Roles in token | Keycloak |
| Business profile | PostgreSQL |
| Company/job/application/interview/finance | PostgreSQL |
| Frontend API cache | RTK Query cache only |
| UI state | Redux slices/local React state |

## Frontend State Management

Use this rule:

```text
RTK Query = server data from Spring Boot API
Redux slices = frontend-only state
Spring Boot + PostgreSQL = source of truth
```

RTK Query handles:

- Companies
- Jobs
- Applications
- AI interview sessions
- Human interviews
- Project assignments
- Hiring records
- Invoices
- User profile data

Redux slices handle:

- Modal state
- Sidebar state
- Selected tab
- Form wizard step
- Temporary filters not stored in URL
- Lightweight auth display state

Do not duplicate RTK Query response data into normal Redux slices.

## Backend Data Flow

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

Controllers validate API input and return DTO responses.

Services enforce business rules and transaction boundaries.

Repositories only handle persistence.

## AI Data Flow

```text
Frontend starts AI interview
        ↓
Spring Boot AI Interview API
        ↓
Load job + seeker profile + resume
        ↓
Spring AI ChatClient
        ↓
Model provider: Gemini/OpenAI/etc.
        ↓
Structured response mapped to Java DTO
        ↓
Saved to PostgreSQL
```

## Voice Data Flow Later

```text
Vapi voice session
        ↓
Spring Boot webhook/API
        ↓
Spring AI service
        ↓
PostgreSQL
```

Vapi is not the business logic layer.

## Architecture Invariants

- Both frontend apps call the same backend API.
- Backend owns all business rules.
- Frontend never decides whether a seeker passed or failed; backend does.
- Frontend never trusts role from local UI state; backend validates JWT roles.
- Recruiter cannot see applicants before platform forwarding.
- Moderator cannot schedule human interview before AI pass.
- Finance cannot generate commission before hiring confirmation.
- No business data is permanently stored only in Redux.
