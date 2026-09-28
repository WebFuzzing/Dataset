FROM amazoncorretto:21-alpine-jdk

COPY ./dist/movies-xml-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar movies-xml-sut.jar \
    --server.port=8080