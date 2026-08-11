source("analyze.R")

suts <- function(){

  # This generated table should NOT be added to Git, as will change based on papers
  TABLE <- "./table_suts.tex"

  #Names here should match what in data.csv
  SUTS <- c(
### REST
"bibliothek",
"blogapi",
"catwatch",
"cwa-verification",
"erc20-rest-service",
"familie-ba-sak",
"features-service",
"genome-nexus",
"gestaohospital",
"http-patch-spring",
"languagetool",
"market",
"microcks",
"ocvn",
"ohsome-api",
"pay-publicapi",
"person-controller",
"proxyprint",
"quartz-manager",
"reservations-api",
"rest-ncs",
"rest-news",
"rest-scs",
"restcountries",
"scout-api",
"session-service",
"spring-actuator-demo",
"spring-batch-rest",
"spring-boot-redis-sample",
"spring-ecommerce",
"spring-rest-example",
"swagger-petstore",
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

