FROM amazoncorretto:17-alpine-jdk

COPY ./dist/kafka-rest-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./dockerfiles/additional_files/kafka-rest/kafka-rest.properties .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Dorg.apache.logging.log4j.level=INFO -jar kafka-rest-sut.jar \
    kafka-rest.properties