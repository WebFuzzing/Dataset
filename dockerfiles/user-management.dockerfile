FROM amazoncorretto:8-alpine-jdk

COPY ./dist/user-management-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar user-management-sut.jar \
    --server.port=8080 --spring.datasource.url="jdbc:mysql://db:3306/users?useSSL=false&allowPublicKeyRetrieval=true"