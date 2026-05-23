# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Cache dependency resolution separately from the source copy so that
# re-builds only re-download dependencies when pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

RUN mvn install:install-file \
    -Dfile=libs/TwsApi.jar \
    -DgroupId=com.ib \
    -DartifactId=tws-api \
    -Dversion=10.45 \
    -Dpackaging=jar \

COPY src ./src
RUN mvn package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
