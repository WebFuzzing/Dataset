FROM amazoncorretto:8-alpine-jdk

COPY ./dist/blogapi-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar blogapi-sut.jar \
    --server.port=8080 --spring.datasource.url="jdbc:mysql://db:3306/blogapi?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"