FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/portal-1.0.0.jar app.jar
COPY ../elysee-events /app/elysee-events

RUN mkdir -p /app/uploads /app/logs

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
