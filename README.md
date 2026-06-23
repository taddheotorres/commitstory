# PrismGit

Transform Git commit history into coherent narrative stories. A full-stack analytics dashboard that tells the visual story of a Git repository.

**Live demo:** https://prismgit-production.up.railway.app

## Stack

- **Backend:** Java 17+, Spring Boot 3.4.1, Spring Data JPA, Flyway, JGit 7.1
- **Frontend:** Angular 19 standalone, TypeScript, RxJS
- **Database:** H2 (dev), PostgreSQL (prod)
- **Infra:** Docker, Docker Compose, Railway
- **Docs:** Swagger/OpenAPI (`/swagger-ui.html`)
- **Testing:** JUnit 5, Mockito (backend), Jasmine/Karma (frontend)

## Features

- **Repo management** — Register Git repositories (local path or remote URL)
- **Commit sync** — Import commits via JGit (local) or GitHub REST API (remote, paginated)
- **Analytics dashboard** — Summary stats, timeline bars, author bars, file heatmap, hourly/daily activity heatmaps
- **Story generation** — Two modes:
  - *Template* — Groups commits by date and author into markdown narratives
  - *LLM* — AI-powered stories via OpenAI (configurable model, graceful fallback)
- **REST API** — 14 endpoints, fully documented via Swagger
- **SPA routing** — Built-in Angular app with resource handler fallback
- **Logging** — SLF4J/@Slf4j with contextual levels (INFO, DEBUG, WARN, ERROR)

## Structure

```
prismgit/
├── backend/                  # REST API + business logic
│   └── src/main/java/com/thiz/prismgit/
│       ├── config/           # Spring configuration (SPA, OpenAPI, app)
│       ├── controller/       # REST controllers (Repo, Analytics, Story)
│       ├── dto/              # Data Transfer Objects
│       ├── entity/           # JPA entities (GitRepo, CommitEntry, Story)
│       ├── exception/        # Global exception handler
│       ├── repository/       # Spring Data repositories
│       └── service/          # Business logic + generators
│           └── generator/    # Template & LLM story generators
├── frontend/                 # Angular 19 SPA
│   └── src/app/
│       ├── models/           # TypeScript interfaces
│       ├── pages/            # Repos, Dashboard, StoryView
│       └── services/         # HTTP service
└── docker/                   # Docker Compose dev setup
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/repos` | Register a new repository |
| `GET` | `/api/repos` | List all repositories |
| `GET` | `/api/repos/{id}` | Get repository details |
| `DELETE` | `/api/repos/{id}` | Delete a repository |
| `POST` | `/api/repos/{id}/sync` | Import commits |
| `GET` | `/api/repos/{id}/commits` | List commits (paginated) |
| `GET` | `/api/repos/{id}/analytics/summary` | Analytics summary |
| `GET` | `/api/repos/{id}/analytics/timeline` | Daily commit timeline |
| `GET` | `/api/repos/{id}/analytics/activity/hour` | Hourly distribution |
| `GET` | `/api/repos/{id}/analytics/activity/day` | Day-of-week distribution |
| `POST` | `/api/repos/{repoId}/stories` | Generate a story |
| `GET` | `/api/repos/{repoId}/stories` | List stories for a repo |
| `GET` | `/api/stories` | List all stories |
| `GET` | `/api/stories/{id}` | Get a story |

Full interactive docs at `/swagger-ui.html` when running.

## Configuration

### Environment variables

| Variable | Description | Required |
|----------|-------------|----------|
| `GITHUB_TOKEN` | GitHub personal access token | For remote sync |
| `LLM_API_KEY` | OpenAI API key | For LLM story mode |
| `LLM_MODEL` | OpenAI model (default: `gpt-4o-mini`) | No |
| `LLM_PROVIDER` | LLM provider (default: `openai`) | No |
| `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` | PostgreSQL connection (prod) | Production |

### Profiles

- **Default (dev):** H2 in-memory database, Flyway migrations
- **`prod`:** PostgreSQL, Flyway clean disabled

## Development

### Prerequisites

- Java 17+, Maven
- Node.js 22+, npm

### Run backend (dev mode)

```bash
cd backend
./mvnw spring-boot:run
```

API at `http://localhost:8080` with H2 in-memory DB. H2 console: `/h2-console`.

### Run frontend (dev mode)

```bash
cd frontend
npm install
ng serve
```

Frontend at `http://localhost:4200`, proxies API calls to `localhost:8080`.

### Run with Docker Compose (full stack)

```bash
docker compose -f docker/docker-compose.yml up
```

## Testing

**Backend** (48+ tests): JUnit 5 + Mockito
- Controllers: Repo, Analytics, Story (20 tests)
- Services: Repo, Story, LLM Generator, RemoteSync (28+ tests)
- Mocked OpenAI and GitHub APIs

**Frontend** (52+ tests): Jasmine + Karma
- Components: Dashboard, Repos, StoryView (32 tests)
- Service: ApiService (20 tests)

```bash
# Backend
cd backend && mvn test

# Frontend
cd frontend && ng test
```

## Deploy

### Railway

The project auto-deploys from the `main` branch via Dockerfile (multi-stage: Angular build → Maven build → JRE runtime).

```bash
railway up --service prismgit
```

## License

MIT
