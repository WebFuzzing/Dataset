FROM amazoncorretto:8-alpine-jdk

COPY ./dist/rest-ncs-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar rest-ncs-sut.jar \
    --server.port=8080