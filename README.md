# commitstory

Transforma el historial de commits de Git en historias narrativas coherentes. Una full-stack web app con Spring Boot (backend) y Angular (frontend).

## Stack

- **Backend:** Java 17+, Spring Boot, Spring Data JPA, Flyway
- **Frontend:** Angular, TypeScript
- **Infra:** Docker, PostgreSQL

## Estructura

```
commitstory/
├── backend/          # API REST + lógica de negocio
│   └── src/main/java/com/thiz/commitstory/
│       ├── config/       # Configuración Spring
│       ├── controller/   # REST controllers
│       ├── dto/          # Data Transfer Objects
│       ├── entity/       # JPA entities
│       ├── repository/   # Spring Data repos
│       └── service/      # Lógica de negocio
├── frontend/         # SPA Angular
│   └── src/app/
│       ├── components/   # Componentes reutilizables
│       ├── models/       # Interfaces TypeScript
│       ├── pages/        # Páginas/rutas
│       └── services/     # Servicios HTTP
└── docker/           # Dockerfiles y compose
```

## Desarrollo

_WIP — próximamente instrucciones de setup._
