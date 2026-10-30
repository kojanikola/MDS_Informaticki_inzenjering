# Build context is the repository root (the build needs the parent pom and events-contract):
#   docker build -f order-service/Dockerfile -t order-service .

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY events-contract/pom.xml events-contract/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY events-contract/src events-contract/src
COPY order-service/src order-service/src
# Tests need Docker (Testcontainers), so they run on the host with `mvn verify`, not inside the image build.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl order-service -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/order-service/target/order-service-*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
