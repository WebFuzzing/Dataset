FROM amazoncorretto:17-alpine-jdk

COPY ./dist/digitalbanking-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar digitalbanking-sut.jar \
    --server.port=8080 --REDIS_HOST=db --REDIS_PORT=6379 --REDIS_PASSWORD=jasonrocks --redis.host=db --redis.port=6379 --KAFKA_HOST=kafka --KAFKA_PORT=9092 --SPRING_CASSANDRA_HOST=cassandra --SPRING_CASSANDRA_PORT=9042 --SPRING_CASSANDRA_CLUSTER=test --SPRING_CASSANDRA_DATACENTER=datacenter1