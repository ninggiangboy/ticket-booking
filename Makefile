# Single command interface (DR-01). CI calls only these targets (DOC-63).
.PHONY: lint fmt test it build

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

# The api image is added with deploy/compose (P1-02).
build:
	./backend/gradlew -p backend bootJar
	pnpm --dir frontend build
	scripts/check-bundle-size.sh
