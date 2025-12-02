FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace/app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

# Rename the jar to something standard, filtering out the plain jar if it exists
RUN find build/libs -name "*.jar" -not -name "*-plain.jar" -exec cp {} build/libs/app.jar \;

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
