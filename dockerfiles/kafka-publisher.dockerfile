FROM amazoncorretto:25-alpine-jdk

COPY ./dist/kafka-publisher-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar kafka-publisher-sut.jar \
    --server.port=8080 --ELASTICSEARCH_HOST=elasticsearch --ELASTICSEARCH_REST_PORT=9200 --eureka.client.enabled=false --management.tracing.export.zipkin.enabled=false