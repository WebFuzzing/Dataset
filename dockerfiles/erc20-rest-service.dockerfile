FROM amazoncorretto:8-alpine-jdk

COPY ./dist/erc20-rest-service-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar erc20-rest-service-sut.jar \
    --server.port=8080