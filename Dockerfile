FROM eclipse-temurin:25-jdk AS build
ARG SERVICE
WORKDIR /src
COPY ${SERVICE}/ ./
RUN sh gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/build/libs/*-SNAPSHOT.jar app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "app.jar"]
