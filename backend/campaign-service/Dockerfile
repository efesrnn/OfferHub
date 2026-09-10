# --- Build stage: Maven wrapper ile içeride derle ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# The cache mount keeps ~/.m2 outside the image layer. Without it every pom.xml change
# baked a fresh ~450MB copy of the whole dependency tree into the build cache and left the
# previous one behind, which is how that cache reached 19GB. Here the jars live in one
# shared cache instead, reused across rebuilds and across services.
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests -B

# --- Runtime stage: sadece JRE + üretilen jar ---
FROM eclipse-temurin:25-jre
WORKDIR /app
# curl is only here for the compose health-check; nothing in the app uses it
RUN apt-get update && apt-get install -y --no-install-recommends curl     && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
