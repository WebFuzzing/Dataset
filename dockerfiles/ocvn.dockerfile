FROM amazoncorretto:8-alpine-jdk

COPY ./dist/ocvn-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./dockerfiles/additional_files/ocvn/init_db.sql .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Dliquibase.enabled=false -Dspring.data.mongodb.uri=mongodb://db:27017/mongo_db -Dspring.datasource.url=jdbc:h2:mem:testdb -Dspring.datasource.driver-class-name=org.h2.Driver -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect -Dspring.jpa.properties.hibernate.enable_lazy_load_no_trans=true -Dspring.datasource.username=sa -Dspring.datasource.password -Ddg-toolkit.derby.port=0 -Dspring.cache.type=NONE -Dspring.datasource.data=file:./init_db.sql -jar ocvn-sut.jar \
    --server.port=8080