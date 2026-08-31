FROM amazoncorretto:8-alpine-jdk

COPY ./dist/languagetool-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar languagetool-sut.jar \
    --port 8080 --public