FROM amazoncorretto:21-alpine-jdk

COPY ./dist/person-controller-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar person-controller-sut.jar \
    --server.port=8080 --spring.data.mongodb.uri=mongodb://db:27017 --spring.cache.type=None