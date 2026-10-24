FROM maven:3.9.16-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY backend/.mvn .mvn
COPY backend/pom.xml ./
COPY backend/src src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid 10001 --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=build /workspace/target/perfumestore-backend-0.0.1-SNAPSHOT.jar /app/app.jar
RUN mkdir -p /app/data/catalog-images && chown -R app:app /app

USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=12 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
