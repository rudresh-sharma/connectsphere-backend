FROM eclipse-temurin:17-jre
WORKDIR /app
COPY comment-service/target/comment-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
