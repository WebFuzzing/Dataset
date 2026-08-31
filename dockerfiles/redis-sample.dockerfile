FROM amazoncorretto:21-alpine-jdk

COPY ./dist/redis-sample-sut.jar .
COPY ./dist/jacocoagent.jar .



#ENV TOOL="undefined"
#ENV RUN="0"

ENTRYPOINT \
    java \
#    unfortunately dumponexit is completely unreliable in Docker :(
#    -javaagent:jacocoagent.jar=destfile=./jacoco/redis-sample__${TOOL}__${RUN}__jacoco.exec,append=false,dumponexit=true \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar redis-sample-sut.jar \
    --server.port=8080 --spring.data.redis.host=db --spring.data.redis.port=6379 --spring.data.redis.password=53cret --spring.autoconfigure.exclude=org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration --app.numberOfRatings=5000 --app.ratingStars=5