FROM openjdk:17-jdk-slim

WORKDIR /app

COPY . /app

RUN javac -d out src/*.java

ENV HOST=thecitygame.onrender.com
ENV PORT=10000
ENV JAVA_OPTS="-Djava.rmi.server.hostname=thecitygame.onrender.com -Djava.rmi.server.port=10000 -Djava.rmi.server.useCodebaseOnly=false"

EXPOSE ${PORT}

CMD java $JAVA_OPTS -cp out GameService 