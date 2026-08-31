FROM amazoncorretto:11-alpine-jdk

COPY ./dist/http-patch-spring-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar http-patch-spring-sut.jar \
    --server.port=8080