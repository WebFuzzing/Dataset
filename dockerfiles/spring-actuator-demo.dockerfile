FROM amazoncorretto:8-alpine-jdk

COPY ./dist/spring-actuator-demo-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar spring-actuator-demo-sut.jar \
    --server.port=8080