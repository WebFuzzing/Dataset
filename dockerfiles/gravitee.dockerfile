FROM amazoncorretto:21-alpine-jdk

COPY ./dist/gravitee-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar gravitee-sut.jar \
    --jetty.port=8080 --ds.mongodb.host=db --ds.mongodb.port=27017 --repositories.analytics.type=none --services.core.http.enabled=false