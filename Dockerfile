FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S -u 100 -G appgroup appuser
WORKDIR /app
COPY --from=build --chown=appuser:appgroup /app/target/trust-service-*.jar app.jar
USER appuser
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
