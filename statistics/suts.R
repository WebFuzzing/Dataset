source("analyze.R")

suts <- function(){

  # This generated table should NOT be added to Git, as will change based on papers
  TABLE <- "./table_suts.tex"

  #Names here should match what in data.csv
  SUTS <- c(
### REST
"adoptme",
"arimaa",
"bibliothek",
"blogapi",
"catwatch",
"commafeed",
"cwa-verification",
"digitalbanking",
"erc20-rest-service",
"familie-ba-sak",
"features-service",
"flight-search-api",
"gdpr-kv",
"traccar",
"genome-nexus",
"gestaohospital",
"http-patch-spring",
"jasper",
"joinus",
"kafka-rest",
"komga",
"languagetool",
"lovemining",
"management-cassandra",
"market",
"microcks",
"movies-xml",
"notebook-manager",
"ocvn",
"ohsome-api",
"pay-publicapi",
"person-controller",
"petclinic-rest",
"pn-paper-tracker",
"proxyprint",
"quartz-manager",
"redis-sample",
"reservations-api",
"rest-ncs",
"rest-news",
"rest-scs",
"restcountries",
"scout-api",
"session-service",
"spring-actuator-demo",
"spring-batch-rest",
"spring-ecommerce",
"spring-rest-example",
"swagger-petstore",
"testing-system",
"tiltaksgjennomforing",
"tracking-system",
"user-management",
"webgoat",
"youtube-mock",
### GraphQL
    # "petclinic-graphql",
    # "patio-api",
    # "timbuctoo",
    #"graphql-ncs",
    #"graphql-scs",
### RPC
    #"thrift-ncs",
    #"thrift-scs",
    #"grpc-ncs",
    #"grpc-scs",
    #"signal-registration",
#     "ind0",
#     "ind1",
    ""
  )

  latex(TABLE,SUTS,TRUE,TRUE)
}

