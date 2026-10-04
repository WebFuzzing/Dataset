rootProject.name = "emb_jdk_21_gradle"

include("cs:rest:gdpr-kv")
include("cs:rest:traccar")

if (System.getenv("BUILD_EVOMASTER") != "false") {
    include("em:embedded:rest:gdpr-kv")
    include("em:external:rest:gdpr-kv")
    include("em:embedded:rest:traccar")
    include("em:external:rest:traccar")
}
