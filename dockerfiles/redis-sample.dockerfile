FROM amazoncorretto:21-alpine-jdk

COPY ./dist/redis-sample-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar redis-sample-sut.jar \
    --server.port=8080 --spring.data.redis.host=db --spring.data.redis.port=6379 --spring.data.redis.password=53cret --spring.autoconfigure.exclude=org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration --app.numberOfRatings=5000 --app.ratingStars=5