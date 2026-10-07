# Single command interface (DR-01). CI calls only these targets (DOC-63).
ENV_FILE ?= deploy/compose/.env
COMPOSE = docker compose --env-file $(ENV_FILE) -f deploy/compose/docker-compose.yml
S ?=

.PHONY: lint fmt test it build up down reset logs psql dev env

lint:
	./backend/gradlew -p backend spotlessCheck
	pnpm --dir frontend lint
	pnpm --dir frontend format:check
	pnpm --dir frontend typecheck
	pnpm --dir frontend i18n:check
	scripts/check-migrations.sh
	scripts/check-secrets.sh

fmt:
	./backend/gradlew -p backend spotlessApply
	pnpm --dir frontend format

test:
	./backend/gradlew -p backend test
	pnpm --dir frontend test --run

it:
	./backend/gradlew -p backend integrationTest

build:
	./backend/gradlew -p backend bootJar
	pnpm --dir frontend build
	scripts/check-bundle-size.sh
	docker build -f backend/Dockerfile -t ticket-api:local .

# --- Local stack (DOC-61 §3, DOC-62 §11) ---
env:
	@test -f $(ENV_FILE) || cp deploy/compose/.env.example $(ENV_FILE)

up: env
	$(COMPOSE) up -d --build --wait
	@. $(ENV_FILE); echo "Ready: http://localhost:$$NGINX_PORT   Mailpit: http://localhost:$$MAILPIT_UI_PORT"

down: env
	$(COMPOSE) down

# Wipes postgres-data, storage-data and Mailpit, then rebuilds without seeding (DR-78)
reset: env
	$(COMPOSE) down -v
	$(COMPOSE) up -d --build --wait

logs: env
	$(COMPOSE) logs -f --tail=200 $(S)

psql: env
	$(COMPOSE) exec postgres sh -c 'psql -U $$POSTGRES_USER $$POSTGRES_DB'

# Infrastructure through compose, api through bootRun, frontend through Vite (DOC-61 §2)
dev: env
	$(COMPOSE) -f deploy/compose/docker-compose.dev.yml up -d --wait postgres redis storage mailpit
	@set -a; . $(ENV_FILE); set +a; \
	export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:$$POSTGRES_PORT/$$POSTGRES_DB \
	  SPRING_DATA_REDIS_HOST=localhost SPRING_DATA_REDIS_PORT=$$REDIS_PORT \
	  SPRING_MAIL_HOST=localhost SPRING_MAIL_PORT=$$MAILPIT_SMTP_PORT \
	  STORAGE_S3_ENDPOINT=http://localhost:8333 APP_BASE_URL=http://localhost:5173; \
	trap 'kill 0' INT TERM; \
	./backend/gradlew -p backend bootRun & \
	pnpm --dir frontend dev & \
	wait
