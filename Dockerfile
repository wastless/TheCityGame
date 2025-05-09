FROM openjdk:17-jdk-slim

WORKDIR /app

COPY . /app

RUN javac -d out src/*.java

EXPOSE ${PORT}

CMD java -Djava.rmi.server.hostname=${HOST} -Djava.rmi.server.port=${PORT} -cp out GameService ${HOST} ${PORT} 