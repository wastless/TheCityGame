FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

RUN javac src/*.java

EXPOSE 1099

CMD ["java", "-cp", "src", "GameService"] 