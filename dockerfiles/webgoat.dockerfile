FROM amazoncorretto:21-alpine-jdk

COPY ./dist/webgoat-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./scripts/dockerize/data/additional_files/webgoat/test.mv.db .



#ENV TOOL="undefined"
#ENV RUN="0"

ENTRYPOINT \
    java \
#    unfortunately dumponexit is completely unreliable in Docker :(
#    -javaagent:jacocoagent.jar=destfile=./jacoco/webgoat__${TOOL}__${RUN}__jacoco.exec,append=false,dumponexit=true \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Drunning.in.docker=true -jar webgoat-sut.jar \
    --webgoat.port=8080 --webwolf.port=8081 --server.address="0.0.0.0" --spring.profiles.active=dev --spring.datasource.driver-class-name=org.h2.Driver --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=none --spring.sql.init.mode=never --spring.datasource.url="jdbc:h2:file:./test" --spring.datasource.username=sa --spring.datasource.password