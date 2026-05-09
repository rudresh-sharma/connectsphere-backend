FROM eclipse-temurin:17-jre
WORKDIR /app
COPY payment-gateway/target/payment-gateway-*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
