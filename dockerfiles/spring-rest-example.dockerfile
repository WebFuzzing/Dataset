FROM amazoncorretto:17-alpine-jdk

COPY ./dist/spring-rest-example-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar spring-rest-example-sut.jar \
    --server.port=8080 --spring.datasource.username=root --spring.datasource.password=root --spring.datasource.url="jdbc:mysql://db:3306/example?useSSL=false&allowPublicKeyRetrieval=true"