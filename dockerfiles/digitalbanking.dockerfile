FROM amazoncorretto:17-alpine-jdk

COPY ./dist/digitalbanking-sut.jar .
COPY ./dist/jacocoagent.jar .



#ENV TOOL="undefined"
#ENV RUN="0"

ENTRYPOINT \
    java \
#    unfortunately dumponexit is completely unreliable in Docker :(
#    -javaagent:jacocoagent.jar=destfile=./jacoco/digitalbanking__${TOOL}__${RUN}__jacoco.exec,append=false,dumponexit=true \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar digitalbanking-sut.jar \
    --server.port=8080 --REDIS_HOST=db --REDIS_PORT=6379 --REDIS_PASSWORD=jasonrocks --redis.host=db --redis.port=6379 --KAFKA_HOST=kafka --KAFKA_PORT=9092 --SPRING_CASSANDRA_HOST=cassandra --SPRING_CASSANDRA_PORT=9042 --SPRING_CASSANDRA_CLUSTER=test --SPRING_CASSANDRA_DATACENTER=datacenter1