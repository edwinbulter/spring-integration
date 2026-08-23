FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/kafka-to-db-app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
