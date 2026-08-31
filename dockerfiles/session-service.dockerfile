FROM amazoncorretto:8-alpine-jdk

COPY ./dist/session-service-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar session-service-sut.jar \
    --server.port=8080 --spring.data.mongodb.uri=mongodb://db:27017/mongo_db --spring.cache.type=NONE