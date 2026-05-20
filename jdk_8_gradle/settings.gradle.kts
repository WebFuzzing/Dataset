rootProject.name = "emb_jdk_8_gradle"

include("cs:rest:erc20-rest-service")

if (System.getenv("BUILD_EVOMASTER") != "false") {
    include("em:embedded:rest:erc20-rest-service")
    include("em:external:rest:erc20-rest-service")
}
