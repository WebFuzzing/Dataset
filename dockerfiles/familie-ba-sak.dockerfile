FROM amazoncorretto:17-alpine-jdk

COPY ./dist/familie-ba-sak-sut.jar .
COPY ./dist/jacocoagent.jar .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -DAZUREAD_TOKEN_ENDPOINT_URL=http://fake-azure-token-endpoint.no:8080 -DAZURE_OPENID_CONFIG_TOKEN_ENDPOINT=bar -DAZURE_APP_CLIENT_ID=bar -DNAIS_APP_NAME=bar -DUNLEASH_SERVER_API_URL=http://fake-unleash-server-api.no:8080 -DUNLEASH_SERVER_API_TOKEN=bar -DBA_SAK_CLIENT_ID=some-audience -jar familie-ba-sak-sut.jar \
    --server.port=8080 --spring.profiles.active=dev --management.server.port=-1 --server.ssl.enabled=false --spring.datasource.url=jdbc:postgresql://db:5432/familiebasak --spring.datasource.username=postgres --spring.datasource.password=password --sentry.logging.enabled=false --sentry.environment=local --funksjonsbrytere.kafka.producer.enabled=false --funksjonsbrytere.enabled=false --logging.level.root=OFF --logging.config=classpath:logback-spring.xml --logging.level.org.springframework=INFO --no.nav.security.jwt.issuer.azuread.discoveryurl=http://mock-oauth2-server:${AUTH_PORT:-8081}/azuread/.well-known/openid-configuration --prosessering.rolle=928636f4-fd0d-4149-978e-a6fb68bb19de --FAMILIE_EF_SAK_API_URL=http://fake-familie-ef-sak/api --FAMILIE_KLAGE_URL=http://fake-familie-klage --FAMILIE_BREV_API_URL=http://fake-familie-brev --FAMILIE_BA_INFOTRYGD_FEED_API_URL=http://fake-familie-ba-infotrygd-feed/api --FAMILIE_BA_INFOTRYGD_API_URL=http://fake-familie-ba-infotrygd --FAMILIE_TILBAKE_API_URL=http://fake-familie-tilbake/api --PDL_URL=http://fake-pdl-api.default --FAMILIE_INTEGRASJONER_API_URL=http://fake-familie-integrasjoner/api --FAMILIE_OPPDRAG_API_URL=http://fake-familie-oppdrag/api --SANITY_FAMILIE_API_URL=http://fake-xsrv1mh6.apicdn.sanity.io/v2021-06-07/data/query/ba-brev --ECB_API_URL=http://fake-data-api.ecb.europa.eu/service/data/EXR/ --rolle.veileder=93a26831-9866-4410-927b-74ff51a9107c --rolle.saksbehandler=d21e00a4-969d-4b28-8782-dc818abfae65 --rolle.beslutter=9449c153-5a1e-44a7-84c6-7cc7a8867233 --rolle.forvalter=c62e908a-cf20-4ad0-b7b3-3ff6ca4bf38b --rolle.kode6=5ef775f2-61f8-4283-bf3d-8d03f428aa14 --rolle.kode7=ea930b6b-9397-44d9-b9e6-f4cf527a632a