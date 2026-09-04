FROM amazoncorretto:8-alpine-jdk

COPY ./dist/swagger-petstore-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./dockerfiles/additional_files/swagger-petstore/inflector.yaml .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar swagger-petstore-sut.jar \
    8080