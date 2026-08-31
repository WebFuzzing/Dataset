FROM amazoncorretto:11-alpine-jdk

COPY ./dist/tracking-system-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar tracking-system-sut.jar \
    --server.port=8080 --spring.profiles.active=dev --spring.datasource.url="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1" --spring.datasource.username=sa --spring.datasource.password