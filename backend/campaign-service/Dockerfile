# --- Build stage: Maven wrapper ile içeride derle ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests -B

# --- Runtime stage: sadece JRE + üretilen jar ---
FROM eclipse-temurin:25-jre
WORKDIR /app
# curl is only here for the compose health-check; nothing in the app uses it
RUN apt-get update && apt-get install -y --no-install-recommends curl     && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
