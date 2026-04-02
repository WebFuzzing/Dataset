rootProject.name = "emb_jdk_17_gradle"

include("cs:rest:bibliothek")

if (System.getenv("BUILD_EVOMASTER") != "false") {
    include("em:embedded:rest:bibliothek")
    include("em:external:rest:bibliothek")
}
