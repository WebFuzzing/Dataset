FROM amazoncorretto:21-alpine-jdk

COPY ./dist/traccar-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./dockerfiles/additional_files/traccar/templates .

COPY ./dockerfiles/additional_files/traccar/traccar.mv.db .

COPY ./dockerfiles/additional_files/traccar/traccar.xml .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar traccar-sut.jar \
    traccar.xml