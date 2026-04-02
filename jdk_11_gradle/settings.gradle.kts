rootProject.name = "emb_jdk_11_gradle"

include("cs:graphql:patio-api")
include("cs:rest:reservations-api")

if (System.getenv("BUILD_EVOMASTER") != "false") {
    include("em:embedded:graphql:patio-api")
    include("em:external:graphql:patio-api")
    include("em:embedded:rest:reservations-api")
    include("em:external:rest:reservations-api")
}
