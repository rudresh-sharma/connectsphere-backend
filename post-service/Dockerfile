FROM eclipse-temurin:17-jre
WORKDIR /app
COPY post-service/target/post-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
