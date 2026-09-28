FROM amazoncorretto:21-alpine-jdk

COPY ./dist/testing-system-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -DSECRET_KEY=wfd-testing-system-jwt-signing-key-0123456789 -jar testing-system-sut.jar \
    --server.port=8080 --spring.datasource.url=jdbc:postgresql://db:5432/testing_system --spring.datasource.username=root --spring.datasource.password=root --spring.data.redis.host=redis --spring.data.redis.port=6379 --logging.level.org.springframework.web=INFO --logging.level.org.springframework.jdbc.core.JdbcTemplate=INFO