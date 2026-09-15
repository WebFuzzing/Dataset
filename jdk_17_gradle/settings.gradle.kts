rootProject.name = "emb_jdk_17_gradle"

include("cs:rest:bibliothek")

// the komga SUT is a multi-module build; only its server module is needed here
include("cs:rest:komga")
project(":cs:rest:komga").projectDir = file("cs/rest/komga/komga")

if (System.getenv("BUILD_EVOMASTER") != "false") {
    include("em:embedded:rest:bibliothek")
    include("em:external:rest:bibliothek")
    include("em:embedded:rest:komga")
    include("em:external:rest:komga")
}
