FROM amazoncorretto:25-alpine-jdk

COPY ./dist/commafeed-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Dquarkus.http.port=8080 -Dquarkus.datasource.db-kind=h2 -Dquarkus.datasource.jdbc.url="jdbc:h2:mem:commafeed;DB_CLOSE_DELAY=-1" -Dquarkus.datasource.username=sa -Dquarkus.datasource.password=sa -jar commafeed-sut.jar \
    