FROM amazoncorretto:21-alpine-jdk

COPY ./dist/joinus-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar joinus-sut.jar \
    --server.port=8080 --spring.mongodb.uri=mongodb://db:27017/joinUs --spring.neo4j.uri=bolt://neo4j:7687 --spring.neo4j.authentication.username=neo4j --spring.neo4j.authentication.password=wfdNeo4jPass123 --logging.level.org.mongodb.driver=INFO --logging.level.org.neo4j.driver=INFO --logging.level.org.springframework.security=INFO