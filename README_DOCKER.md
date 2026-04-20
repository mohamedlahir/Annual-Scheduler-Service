This README documents how to build and run the multi-container stack shown in your docker-compose.yml that includes Consul, Postgres, Kafka, and multiple Spring Boot services (gateway, auth, user, annual-scheduler).

Prerequisites
- Docker & Docker Compose installed (compose v2 recommended)
- Enough memory (Docker Desktop: 4GB+ recommended)
- Ports 8500, 5432, 9092, 8080..8084 available
- Project checked out so relative build contexts in docker-compose work (or adjust paths)

Files added/used
- `Dockerfile` — multi-stage Dockerfile added under the `annual.scheduler` project root (produces `/app/app.jar`).
- `docker-compose.yml` — user-provided Compose file (ensure this is saved at parent workspace root and contexts are correct).

Commands
1) Build the local `annual.scheduler` image only (from project dir):

```bash
# from: /Users/mohamedlahir/Desktop/practise/Java/Spring/Spring Boot App/annual.scheduler
docker build -t annual-scheduler-service:local .
```

2) Build all service images with docker-compose and start the stack

```bash
# from directory that contains your docker-compose.yml (adjust path if needed)
# build then start detached
docker compose build --pull
docker compose up -d

# show logs
docker compose logs -f --tail=100 annual-scheduler-service
```

3) Bring the stack down

```bash
docker compose down --volumes
```

Notes and tips
- The compose file uses local build contexts for other services (auth-service, user-service) relative to where compose file lives. Make sure those directories contain their Dockerfiles and can build in CI.
- The `postgres` service mounts `./init.sql` into the container. Ensure `init.sql` exists in the same dir as `docker-compose.yml` and creates the databases: `auth_db`, `user_profile_db`, `scheduling_db` (or adjust compose envs).
- Because services depend on Postgres health, Compose will wait on `pg_isready`. If tests or migrations run at startup, they will attempt database connections.
- If you face function-class scanning errors at startup (corrupt class files), delete `target/` in the corresponding project before building the Docker image.

Troubleshooting
- Common problem: file paths/context wrong. If docker build fails because source isn't found, adjust `context` paths in `docker-compose.yml`.
- If Spring service fails because classpath contains stray `.class` files, run `mvn -DskipTests clean package` locally to ensure a clean target.

Optional environment overrides
- Use `docker compose up -d --build` to rebuild images.
- You can pass additional `JAVA_OPTS` via compose `environment` for each service, e.g.:

```yaml
environment:
  - JAVA_OPTS=-Xmx512m
```

If you want, I can:
- create a top-level `docker-compose.yml` in this repository with your provided content (adjusting relative `context` paths to match workspace), and
- add `init.sql` template to create the required DBs and users.

Tell me whether you want me to add a `docker-compose.yml` file into the repo (I can place it at the workspace root) and whether I should add a sample `init.sql` that creates the three DBs used by your services.

