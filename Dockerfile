FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
ENV MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=128m"
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 1099
ENV JAVA_OPTS="-Xmx512m -XX:MaxPermSize=128m"
CMD ["java", "-jar", "app.jar"] 