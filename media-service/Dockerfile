FROM eclipse-temurin:17-jre
WORKDIR /app
COPY media-service/target/media-service-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
