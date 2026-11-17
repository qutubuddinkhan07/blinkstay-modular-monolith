# ---- build stage: compile the jar ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

# ---- run stage: only the JRE and the jar ----
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd -r -u 1001 spring
COPY --from=build /app/target/*.jar app.jar
USER spring
# use 75% of the container memory for the heap; SerialGC suits 1 CPU and small memory
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]