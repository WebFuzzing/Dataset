FROM amazoncorretto:11-alpine-jdk

COPY ./dist/market-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Dspring.datasource.url="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1" -Dspring.datasource.username=sa -Dspring.datasource.password -jar market-sut.jar \
    --server.port=8080