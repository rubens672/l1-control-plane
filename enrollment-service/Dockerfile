FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/enrollment-service-0.1.0.jar app.jar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]