FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/credit-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java","-jar","app.jar"]