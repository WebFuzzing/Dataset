FROM amazoncorretto:17-alpine-jdk

COPY ./dist/ohsome-api-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./dockerfiles/additional_files/ohsome-api/heidelberg.mv.db .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar ohsome-api-sut.jar \
    --server.port=8080 --database.db=heidelberg