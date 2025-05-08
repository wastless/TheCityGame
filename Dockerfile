FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app
COPY . .
ENV MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=128m"
RUN mvn clean package -DskipTests -X

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 1099
ENV JAVA_OPTS="-Xmx512m -XX:MaxPermSize=128m"
RUN ls -la /app
RUN jar tf app.jar
CMD ["java", "-jar", "app.jar"] 