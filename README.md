# commitstory

Transform Git commit history into coherent narrative stories. A full-stack web application built with Spring Boot (backend) and Angular (frontend).

## Stack

- **Backend:** Java 17+, Spring Boot, Spring Data JPA, Flyway, JGit
- **Frontend:** Angular, TypeScript
- **Infra:** Docker, PostgreSQL

## Structure

```
commitstory/
├── backend/              # REST API + business logic
│   └── src/main/java/com/thiz/commitstory/
│       ├── config/           # Spring configuration
│       ├── controller/       # REST controllers
│       ├── dto/              # Data Transfer Objects
│       ├── entity/           # JPA entities
│       ├── exception/        # Global exception handling
│       ├── repository/       # Spring Data repositories
│       └── service/          # Business logic
│           └── generator/    # Story generators (template, LLM)
├── frontend/             # Angular SPA
│   └── src/app/
│       ├── components/       # Reusable components
│       ├── models/           # TypeScript interfaces
│       ├── pages/            # Pages/routes
│       └── services/         # HTTP services
└── docker/               # Dockerfiles and compose
```

## Features

- **Repo management** — Register git repositories (local or remote)
- **Commit sync** — Import commits from local repos (JGit) or remote repos (GitHub API)
- **Story generation** — Two modes:
  - *Template* — groups commits by date and author into markdown narratives
  - *LLM* — generates AI-powered stories via OpenAI (configurable model)
- **REST API** — Full CRUD for repos, commits, and stories

## Configuration

### Environment variables

| Variable | Description | Required |
|---|---|---|
| `GITHUB_TOKEN` | GitHub personal access token for remote sync | For GitHub repos |
| `LLM_API_KEY` | OpenAI API key for LLM story generation | For LLM mode |
| `LLM_MODEL` | OpenAI model (default: `gpt-4o-mini`) | No |
| `LLM_PROVIDER` | LLM provider (default: `openai`) | No |
| `DB_PASSWORD` | PostgreSQL password (prod profile only) | For production |

## Development

### Prerequisites

- Java 17+
- Maven
- Node.js & npm (for Angular frontend)

### Run backend (dev mode)

```bash
cd backend
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080` with an H2 in-memory database. H2 console is available at `/h2-console`.

### Run with PostgreSQL (prod profile)

```bash
cd backend
DB_PASSWORD=your_password ./mvnw spring-boot:run -Dspring.profiles.active=prod
```

### API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/repos` | Register a new repository |
| `GET` | `/api/repos` | List all repositories |
| `GET` | `/api/repos/{id}` | Get repository details |
| `DELETE` | `/api/repos/{id}` | Delete a repository |
| `POST` | `/api/repos/{id}/sync` | Import commits from the repository |
| `GET` | `/api/repos/{id}/commits` | List commits (paginated) |
| `POST` | `/api/repos/{repoId}/stories` | Generate a story (TEMPLATE or LLM) |
| `GET` | `/api/stories` | List all stories (optional `?repoId=` filter) |
| `GET` | `/api/stories/{id}` | Get a story |
| `GET` | `/api/repos/{repoId}/stories` | List stories for a repository |

## License

MIT
