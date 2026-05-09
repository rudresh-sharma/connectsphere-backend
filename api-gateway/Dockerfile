FROM eclipse-temurin:17-jre
WORKDIR /app
COPY api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
