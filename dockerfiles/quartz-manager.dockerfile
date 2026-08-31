FROM amazoncorretto:11-alpine-jdk

COPY ./dist/quartz-manager-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
     -jar quartz-manager-sut.jar \
    --server.port=8080 --quartz-manager.security.accounts.in-memory.users[0].username=foo --quartz-manager.security.accounts.in-memory.users[0].password=bar --quartz-manager.security.accounts.in-memory.users[0].roles[0]=admin --quartz-manager.security.accounts.in-memory.users[1].username=foo2 --quartz-manager.security.accounts.in-memory.users[1].password=bar --quartz-manager.security.accounts.in-memory.users[1].roles[0]=admin