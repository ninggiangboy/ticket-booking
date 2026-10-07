FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /src
COPY backend/ backend/
RUN --mount=type=cache,target=/root/.gradle ./backend/gradlew -p backend bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /src/backend/build/libs/api.jar /app/api.jar
EXPOSE 8081 9090
# Array form so `docker compose run --rm api --spring.profiles.active=invariants` reaches the app (DR-73)
ENTRYPOINT ["java", "-jar", "/app/api.jar"]
