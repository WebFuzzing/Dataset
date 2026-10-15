FROM amazoncorretto:21-alpine-jdk

COPY ./dist/flight-search-api-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar flight-search-api-sut.jar \
    --server.port=8080 --spring.data.mongodb.host=db --spring.data.mongodb.port=27017 --spring.data.mongodb.database=flightdatabase