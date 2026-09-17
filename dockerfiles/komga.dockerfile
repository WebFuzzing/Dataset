FROM amazoncorretto:17-alpine-jdk

COPY ./dist/komga-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./dockerfiles/additional_files/komga/komga-database.sqlite .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar komga-sut.jar \
    --server.port=8080 --komga.config-dir=/tmp/komga --komga.database.file=/komga-database.sqlite