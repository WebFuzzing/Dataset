FROM amazoncorretto:8-alpine-jdk

COPY ./dist/restcountries-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar restcountries-sut.jar \
    --server.port=8080