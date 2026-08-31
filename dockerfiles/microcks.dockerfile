FROM amazoncorretto:21-alpine-jdk

COPY ./dist/microcks-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -DPOSTMAN_RUNNER_URL=http://postman:3000 -DSERVICES_UPDATE_INTERVAL='0 0 0/2 * * *' -DKEYCLOAK_URL=http://keycloak:8080 -DKEYCLOAK_PUBLIC_URL=http://localhost:${AUTH_PORT:-8081} -DENABLE_CORS_POLICY=false -DCORS_REST_ALLOW_CREDENTIALS=true -DTEST_CALLBACK_URL=http://localhost:${HOST_PORT:-8080} -jar microcks-sut.jar \
    --server.port=8080 --spring.profiles.active=prod --grpc.server.port=0 --spring.data.mongodb.uri=mongodb://mongodb:27017/test --spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:${AUTH_PORT:-8081}/realms/microcks --spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://keycloak:8080/realms/microcks/protocol/openid-connect/certs