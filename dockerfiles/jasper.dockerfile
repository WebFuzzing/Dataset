FROM amazoncorretto:25-alpine-jdk

COPY ./dist/jasper-sut.jar .
COPY ./dist/jacocoagent.jar .



#ENV TOOL="undefined"
#ENV RUN="0"

ENTRYPOINT \
    java \
#    unfortunately dumponexit is completely unreliable in Docker :(
#    -javaagent:jacocoagent.jar=destfile=./jacoco/jasper__${TOOL}__${RUN}__jacoco.exec,append=false,dumponexit=true \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar jasper-sut.jar \
    --server.port=8080 --spring.profiles.active=api-docs --spring.datasource.url=jdbc:postgresql://db:5432/jasper --spring.datasource.username=postgres --spring.datasource.password=password --spring.datasource.hikari.auto-commit=false --spring.jpa.database-platform=jasper.config.PostgreSQLDialect --springdoc.api-docs.path=/api/v3/api-docs --jasper.allow-user-tag-header=true --jasper.allow-user-role-header=true --jasper.allow-auth-headers=true