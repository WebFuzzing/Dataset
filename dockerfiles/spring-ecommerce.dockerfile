FROM amazoncorretto:8-alpine-jdk

COPY ./dist/spring-ecommerce-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Dfile.encoding=ISO-8859-1 -jar spring-ecommerce-sut.jar \
    --server.port=8080 --spring.datasource.host=mongodb --spring.datasource.port=27017 --spring.datasource.database=test --spring.data.mongodb.uri=mongodb://mongodb:27017/test --spring.redis.host=redis --spring.redis.port=6379 --spring.data.elasticsearch.cluster-name=elasticsearch --spring.data.elasticsearch.cluster-nodes=elasticsearch:9300 --spring.elasticsearch.rest.uris=elasticsearch:9200 --spring.data.elasticsearch.host=elasticsearch --spring.data.elasticsearch.port=9300 --spring.cache.type=NONE