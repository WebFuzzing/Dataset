FROM amazoncorretto:17-alpine-jdk

COPY ./dist/petclinic-rest-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar petclinic-rest-sut.jar \
    --server.port=8080 --spring.jpa.show-sql=false