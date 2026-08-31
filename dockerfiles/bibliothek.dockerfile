FROM amazoncorretto:17-alpine-jdk

COPY ./dist/bibliothek-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar bibliothek-sut.jar \
    --server.port=8080 --databaseUrl=mongodb://db:27017/mongo_db --spring.data.mongodb.uri=mongodb://db:27017/mongo_db --app.storagePath=./tmp/bibliothek/