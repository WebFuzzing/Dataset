FROM amazoncorretto:11-alpine-jdk

COPY ./dist/management-cassandra-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Dmgmtapi.cql.tcp=cassandra:9999 -jar management-cassandra-sut.jar \
    -K true -H tcp://0.0.0.0:8080 -S mgmtapi.sock