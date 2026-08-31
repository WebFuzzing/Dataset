FROM amazoncorretto:11-alpine-jdk

COPY ./dist/pay-publicapi-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./scripts/dockerize/data/additional_files/pay-publicapi/em_config.yaml .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Ddw.server.applicationConnectors[0].port=8080 -Ddw.server.adminConnectors[0].port=0 -Ddw.redis.endpoint=db:6379 -jar pay-publicapi-sut.jar \
    server em_config.yaml