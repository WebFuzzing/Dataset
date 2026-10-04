FROM amazoncorretto:25-alpine-jdk

COPY ./dist/kafka-producer-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar kafka-producer-sut.jar \
    --server.port=8080 --KAFKA_HOST=kafka --KAFKA_PORT=9092 --eureka.client.enabled=false --management.tracing.export.zipkin.enabled=false