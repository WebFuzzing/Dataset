FROM amazoncorretto:21-alpine-jdk

COPY ./dist/gdpr-kv-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar gdpr-kv-sut.jar \
    --server.port=8080 --server.aws.endpoint=http://db:4566 --server.aws.region=us-west-2 --server.aws.use-localstack=true