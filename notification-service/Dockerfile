FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8088
# Cheia Firebase se monteaza ca volum, ex: -v ./secrets:/secrets -e FIREBASE_CREDENTIALS_PATH=/secrets/firebase-service-account.json
ENTRYPOINT ["java", "-jar", "app.jar"]
