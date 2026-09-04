FROM amazoncorretto:21-alpine-jdk

COPY ./dist/arimaa-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar arimaa-sut.jar \
    --server.port=8080 --spring.datasource.url=jdbc:mysql://db:3306/arimaadockermysqldb --spring.datasource.username=admin --spring.datasource.password=AdminPassword123! --spring.mongodb.uri=mongodb://mongodb:27017/arimaadb --spring.neo4j.uri=bolt://neo4j:7687 --spring.neo4j.authentication.username=neo4j --spring.neo4j.authentication.password=wfdNeo4jPass123