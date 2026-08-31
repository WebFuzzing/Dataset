FROM amazoncorretto:8-alpine-jdk

COPY ./dist/scout-api-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./scripts/dockerize/data/additional_files/scout-api/init_db.sql .

COPY ./scripts/dockerize/data/additional_files/scout-api/scout_api_evomaster.yml .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar scout-api-sut.jar \
    server scout_api_evomaster.yml