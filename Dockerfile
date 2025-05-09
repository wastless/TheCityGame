FROM openjdk:17-jdk-slim

WORKDIR /app

COPY . /app

RUN javac -d out src/*.java

ENV HOST=thecitygame.onrender.com
ENV PORT=10000

EXPOSE ${PORT}

CMD java -Djava.rmi.server.hostname=${HOST} -Djava.rmi.server.port=${PORT} -Djava.rmi.server.useCodebaseOnly=false -cp out GameService 