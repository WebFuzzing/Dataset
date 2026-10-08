FROM amazoncorretto:8-alpine-jdk

COPY ./dist/notebook-manager-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar notebook-manager-sut.jar \
    --server.port=8080 --spring.datasource.url="jdbc:mysql://db:3306/notebook_manager?useSSL=false&allowPublicKeyRetrieval=true" --spring.datasource.username=root --spring.datasource.password=root