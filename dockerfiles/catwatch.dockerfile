FROM amazoncorretto:8-alpine-jdk

COPY ./dist/catwatch-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Dserver.port=8080 -Dspring.datasource.url=jdbc:h2:mem:testdb -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect -Dspring.datasource.username=sa -Dspring.datasource.password -jar catwatch-sut.jar \
    