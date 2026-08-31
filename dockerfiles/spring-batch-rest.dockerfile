FROM amazoncorretto:8-alpine-jdk

COPY ./dist/spring-batch-rest-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar spring-batch-rest-sut.jar \
    --server.port=8080 --spring.batch.job.enabled=false --lastNamePrefix= --upperCase=false