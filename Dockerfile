FROM openjdk:17-jdk-slim

WORKDIR /app

COPY . .

RUN javac src/*.java

EXPOSE 1099

CMD ["java", "-cp", "src", "GameService"] 