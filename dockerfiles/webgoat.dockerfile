FROM amazoncorretto:21-alpine-jdk

COPY ./dist/webgoat-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./dockerfiles/additional_files/webgoat/test.mv.db .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Drunning.in.docker=true -jar webgoat-sut.jar \
    --webgoat.port=8080 --webwolf.port=8081 --server.address="0.0.0.0" --spring.profiles.active=dev --spring.datasource.driver-class-name=org.h2.Driver --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=none --spring.sql.init.mode=never --spring.datasource.url="jdbc:h2:file:./test" --spring.datasource.username=sa --spring.datasource.password