FROM eclipse-temurin:17-jre
WORKDIR /app
COPY admin-server/target/admin-server-*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
