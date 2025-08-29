#!/usr/bin/env python

## Script Build for Black-Box Experiments

import sys
import os
import stat
import pathlib
# import shutil

# recall sys.argv[0] is the script name
if len(sys.argv) < 4 or len(sys.argv) > 5 :
    print("Usage:\n<nameOfScript>.py <dir> <minSeed> <maxSeed> (<tool>,...,<tool>)?")
    exit(1)

# When creating a new set of experiments, all needed files will be saved in a folder
BASE_DIR = os.path.abspath(sys.argv[1])

# Experiments are repeated a certain number of times, with different seed for the
# random generator (if option is available). This specifies the starting seed.
MIN_SEED = int(sys.argv[2])

# Max seed, included. For example, if running min=10 and max=39, each experiment is
# going to be repeated 30 times, starting from seed 10 to seed 39 (both included).
MAX_SEED = int(sys.argv[3])


### main input parameter validation
if MIN_SEED > MAX_SEED:
    print("ERROR: min seed is greater than max seed")
    exit(1)


# base seeds used for runs. TCP port bindings will be based on such seed.
# If running new experiments while some previous are still running, to avoid TCP port
# conflict, can use an higher base seed. Each run reserves 10 ports. So, if you run
# 500 jobs with starting seed 10000, you will end up using ports up to 15000
BASE_SEED = 10000


SRC_DIR = os.path.dirname(os.path.abspath(__file__))

# 1 hour
MAX_TIME_SECONDS = 3600

TIMEOUT_COMMAND = "timeout 90m "

if not os.path.isdir(BASE_DIR):
    print("creating folder: " + BASE_DIR)
    os.makedirs(BASE_DIR)
else:
    print("ERROR: target folder already exists")
    exit(1)


class Sut:
    def __init__(self, name, auth, sleep, schemaformat, classfiles):
        self.name = name
        # whether it needs authentication info
        self.auth = auth
        # for how long script will sleep after Docker Compose is started...
        # to avoid fuzzing while API not initialized yet
        self.sleep = sleep
        # format (eg json/yaml) of OpenAPI schema in the benchmark
        self.schemaformat = schemaformat
        # relative location of class files, needed for JaCoCo computations
        self.classfiles = classfiles



SLEEP=120
YAML="yaml"
JSON="json"

# To ge the SUTs, you need in EMB to run the script "scripts/dist.py"
SUTS = [
    Sut("bibliothek",False,SLEEP,JSON,["jdk_17_gradle/cs/rest/bibliothek/build/classes"]),
    Sut("blogapi",True, SLEEP,JSON,["jdk_8_maven/cs/rest/original/blogapi/target/classes"]),
    Sut("catwatch",False,SLEEP,JSON,["jdk_8_maven/cs/rest/original/catwatch/catwatch-backend/target/classes"]),
    Sut("cwa-verification",False,SLEEP,JSON,["jdk_11_maven/cs/rest/cwa-verification-server/target/classes"]),
    Sut("erc20-rest-service",False,SLEEP,JSON,["jdk_8_gradle/cs/rest/erc20-rest-service/build/classes"]),
    Sut("familie-ba-sak",True,SLEEP,JSON,["jdk_17_maven/cs/rest/familie-ba-sak/target/classes"]),
    Sut("features-service",False,SLEEP,JSON,["jdk_8_maven/cs/rest/original/features-service/target/classes"]),
    Sut("genome-nexus",False,SLEEP,JSON,["jdk_8_maven/cs/rest-gui/genome-nexus/component/target/classes",
                                   "jdk_8_maven/cs/rest-gui/genome-nexus/model/target/classes",
                                   "jdk_8_maven/cs/rest-gui/genome-nexus/persistence/target/classes",
                                   "jdk_8_maven/cs/rest-gui/genome-nexus/service/target/classes",
                                   "jdk_8_maven/cs/rest-gui/genome-nexus/web/target/classes"]),
    Sut("gestaohospital",False,SLEEP,JSON,["jdk_8_maven/cs/rest-gui/gestaohospital/target/classes"]),
    Sut("http-patch-spring",False,SLEEP,JSON,["jdk_11_maven/cs/rest/http-patch-spring/target/classes"]),
    Sut("languagetool",False,SLEEP,JSON,["jdk_8_maven/cs/rest/original/languagetool/languagetool-core/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-gui-commons/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/all/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/ar/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/ast/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/be/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/br/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/ca/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/da/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/de/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/de-DE-x-simple-language/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/el/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/en/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/eo/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/es/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/fa/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/fr/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/ga/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/gl/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/is/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/it/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/ja/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/km/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/lt/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/ml/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/nl/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/pl/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/pt/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/ro/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/ru/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/sk/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/sl/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/sv/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/ta/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/tl/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/uk/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-language-modules/zh/target/classes",
                                   "jdk_8_maven/cs/rest/original/languagetool/languagetool-server/target/classes"]),
    Sut("market",True,SLEEP,JSON,["jdk_11_maven/cs/rest-gui/market/market-core/target/classes",
                             "jdk_11_maven/cs/rest-gui/market/market-rest/target/classes",
                             "jdk_11_maven/cs/rest-gui/market/market-web/target/classes"]),
    Sut("microcks",True,SLEEP,JSON,["jdk_21_maven/cs/rest-gui/microcks/commons/model/target/classes",
                                    "jdk_21_maven/cs/rest-gui/microcks/commons/util/target/classes",
                                    "jdk_21_maven/cs/rest-gui/microcks/commons/util-el/target/classes",
                                    "jdk_21_maven/cs/rest-gui/microcks/distro/uber/target/classes",
                                    "jdk_21_maven/cs/rest-gui/microcks/distro/uber-async-minion/target/classes",
                                    "jdk_21_maven/cs/rest-gui/microcks/minions/async/target/classes",
                                    "jdk_21_maven/cs/rest-gui/microcks/webapp/target/classes"]),
    Sut("ocvn",True,SLEEP,JSON,["jdk_8_maven/cs/rest-gui/ocvn/forms/target/classes",
                           "jdk_8_maven/cs/rest-gui/ocvn/persistence/target/classes",
                           "jdk_8_maven/cs/rest-gui/ocvn/persistence-mongodb/target/classes",
                           "jdk_8_maven/cs/rest-gui/ocvn/web/target/classes"]),
    Sut("ohsome-api",False,SLEEP,JSON,["jdk_17_maven/cs/rest/ohsome-api/target/classes"]),
    Sut("pay-publicapi",True,SLEEP,JSON,["jdk_11_maven/cs/rest/pay-publicapi/target/classes"]),
    Sut("person-controller",False,SLEEP,JSON,["jdk_21_maven/cs/rest/person-controller/target/classes"]),
    Sut("proxyprint",True,SLEEP,JSON,["jdk_8_maven/cs/rest/original/proxyprint/target/classes"]),
    Sut("quartz-manager",True,SLEEP,JSON,["jdk_11_maven/cs/rest-gui/quartz-manager/quartz-manager-parent/quartz-manager-common/target/classes",
                                          "jdk_11_maven/cs/rest-gui/quartz-manager/quartz-manager-parent/quartz-manager-starter-api/target/classes",
                                          "jdk_11_maven/cs/rest-gui/quartz-manager/quartz-manager-parent/quartz-manager-starter-persistence/target/classes",
                                          "jdk_11_maven/cs/rest-gui/quartz-manager/quartz-manager-parent/quartz-manager-starter-security/target/classes",
                                          "jdk_11_maven/cs/rest-gui/quartz-manager/quartz-manager-parent/quartz-manager-web-showcase/target/classes",]),
    Sut("reservations-api",True,SLEEP,JSON,["jdk_11_gradle/cs/rest/reservations-api/build/classes"]),
    Sut("rest-ncs",False,SLEEP,JSON,["jdk_8_maven/cs/rest/artificial/ncs/target/classes"]),
    Sut("rest-news",False,SLEEP,JSON,["jdk_8_maven/cs/rest/artificial/news/target/classes"]),
    Sut("rest-scs",False,SLEEP,JSON,["jdk_8_maven/cs/rest/artificial/scs/target/classes"]),
    Sut("restcountries",False,SLEEP,YAML,["jdk_8_maven/cs/rest/original/restcountries/target/classes"]),
    Sut("scout-api",True,SLEEP,JSON,["jdk_8_maven/cs/rest/original/scout-api/api/target/classes",
                                "jdk_8_maven/cs/rest/original/scout-api/auth/target/classes",
                                "jdk_8_maven/cs/rest/original/scout-api/data-access/target/classes",
                                "jdk_8_maven/cs/rest/original/scout-api/data-batch-jobs/target/classes"]),
    Sut("session-service",False,SLEEP,JSON,["jdk_8_maven/cs/rest/original/session-service/target/classes"]),
    Sut("spring-actuator-demo",True,SLEEP,JSON,["jdk_8_maven/cs/rest/original/spring-actuator-demo/target/classes"]),
    Sut("spring-batch-rest",False,SLEEP,JSON,["jdk_8_maven/cs/rest/original/spring-batch-rest/api/target/classes",
                                              "jdk_8_maven/cs/rest/original/spring-batch-rest/util/target/classes"]),
    Sut("spring-ecommerce",True,SLEEP,JSON,["jdk_8_maven/cs/rest/original/spring-ecommerce/target/classes"]),
    Sut("spring-rest-example",False,SLEEP,JSON,["jdk_17_maven/cs/rest/spring-rest-example/target/classes"]),
    Sut("swagger-petstore",False,SLEEP,JSON,["jdk_8_maven/cs/rest/original/swagger-petstore/target/classes"]),
    Sut("tiltaksgjennomforing",True,SLEEP,JSON,["jdk_17_maven/cs/rest/tiltaksgjennomforing/target/classes"]),
    Sut("tracking-system",True,SLEEP,JSON,["jdk_11_maven/cs/rest/tracking-system/target/classes"]),
    Sut("user-management",False,SLEEP,JSON,["jdk_8_maven/cs/rest/original/user-management/target/classes"]),
    Sut("webgoat",True,SLEEP,JSON,["jdk_21_maven/cs/rest-gui/webgoat/target/classes"]),
    Sut("youtube-mock",False,SLEEP,YAML,["jdk_8_maven/cs/rest/original/youtube-mock/target/classes"]),
]


WFD_DIR = os.environ.get('WFD_DIR',"")
if WFD_DIR == "":
    raise Exception("You must specify a WFD_DIR env variable specifying the folder where EMB repository was cloned")


DIST_DIR = WFD_DIR + "/dist"
if not os.path.exists(DIST_DIR):
    raise Exception(DIST_DIR + " does not exist. Did you run script/dist.py?")


BB_TOOL_DIR = os.environ.get('BB_TOOL_DIR',"")
if not os.path.exists(BB_TOOL_DIR):
    raise Exception("You must specify a BB_TOOL_DIR env variable specifying the folder where all the tools to compare are located")



SCRIPT_DIR = BASE_DIR+"/scripts"
RESULT_SCRIPT_DIR = BASE_DIR+"/result_scripts"
LOGS_DIR   = BASE_DIR+"/logs"
TESTS_DIR  = BASE_DIR+"/tests"
# online: coverage during the fuzzing session
JACOCO_ONLINE_DIR = BASE_DIR+"/jacoco-online"
EXEC_ONLINE_DIR = BASE_DIR+"/exec-online"
# test: coverage during executing generated tests
JACOCO_TEST_DIR = BASE_DIR+"/jacoco-test"
EXEC_TEST_DIR = BASE_DIR+"/exec-test"


def writeScript(basedir, code, port, tool, sut):
    script_path = basedir + "/" + tool  + "_" + sut.name + "_" + str(port) + ".sh"
    script = open(script_path, "w")
    script.write(code)

    st = os.stat(script_path)
    os.chmod(script_path, st.st_mode | stat.S_IEXEC)

def dockerId(port):
    return "id"+str(port)

def getScriptHead(port,tool,sut, exec_dir):
    s = ""
    s += "#!/bin/bash \n\n"
    s += "SUT=\"" + WFD_DIR + "/dockerfiles/"+sut.name+".yaml\" \n"
    s += "\n"

    # These environment variables are read inside Docker Compose
    # Must be on same line of docker, so that why using \.
    # Note: there must be NOTHING after the \, not even empty space " "...
    s += "HOST_PORT="+str(port) + " \\\n"
    s += "JACOCO_PORT="+str(port+1) + " \\\n"
    s += "AUTH_PORT="+str(port+2) + " \\\n"
    s += "HOST_LOG_DIR=\"" + exec_dir +  "\" \\\n"
    s += "MITM_LOG_FILE=" + getFileNamePrefix(sut,tool,port) + "__minlog.csv \\\n"
    # s += "TOOL="+tool + " \\\n"
    # s += "RUN="+str(port) + " \\\n"
    # s += "JACOCODIR=\"" + exec_dir  + "\" \\\n"
    #  Start SUT with Docker Compose
    s += "docker-compose --project-name "+dockerId(port)+" -f $SUT up --build --force-recreate " + getRedirectLog(getLogFile("sut",tool,sut.name,port)) + "  & \n\n"

    # No easy way to check if all is up and running, so wait for enough time before fuzzing
    s += "sleep " + str(sut.sleep) +"\n\n"

    return s


def getFileNamePrefix(sut,tool,port):
    return sut.name +"__" + tool +"__" + str(port)


def getScriptFooter(port,tool,sut, exec_dir, jacoco_dir):
    s = "\n\n"

    filename =  getFileNamePrefix(sut,tool,port) + "__jacoco"
    execFilePath = "\"" + exec_dir + "/" + filename + ".exec\""

    s += "java -jar \"" + DIST_DIR + "/jacococli.jar\" \\\n"
    s += "  dump  --address localhost --port "+str(port+1)+" \\\n"
    s += "  --destfile " + execFilePath + "\\\n"
    s += "  " + getRedirectLog(getLogFile("jacoco-dump",tool,sut.name,port)) +"\n"
    s += "\n"
    s += "sleep 30\n"
    s += "\n"

    # once fuzzer is completed, shut down Docker Compose
    s += "docker-compose -f $SUT down --remove-orphans \n\n"

    s += "# unfortunately, 'down' is not reliable... can leave images and networks up and running\n"
    s += "sleep 30\n"
    id = dockerId(port)
    s += "docker ps -q --filter \"label=com.docker.compose.project="+id+"\" | xargs -r docker stop\n"
    s += "sleep 30\n"
    s += "docker network rm "+ id+"_default \n"
    # this is the name of build images of SUT inside Docker-Compose
    s += "docker rmi -f "+ id+"-sut-"+sut.name+" \n"
    s += "\n"



    # transform .exec files into readable .csv files.
    s += "java -jar \"" + DIST_DIR + "/jacococli.jar\" \\\n"
    s += "  report " + execFilePath+ " \\\n"
    for path in sut.classfiles:
        s += "  --classfiles \"" + WFD_DIR + "/" + path + "\" \\\n"
    s += "  --csv \"" + jacoco_dir + "/" + filename + ".csv\" \\\n"
    s += "  " + getRedirectLog(getLogFile("jacoco-report",tool,sut.name,port)) +"\n"

    return s


def getLogFile(label,tool,sutname,port):
    return pathlib.PurePath(LOGS_DIR + "/"+ tool + "__" +  sutname + "__" + str(port)+"__" +label + ".txt").as_posix()

def getRedirectLog(logs):
    return  " >> \""+logs+"\" 2>&1"


def createJobs():


    port = BASE_SEED
    # 1 for SUT,  1 for JaCoCo TCP server and 1 optional for auth server
    delta = 3

    for sut in SUTS:

        createScriptForBase(sut, port)
        port = port + delta

        for seed in range(MIN_SEED, MAX_SEED + 1):

            for tool in BB_TOOLS:

                if tool == EVOMASTER_BB :
                    createScriptForEvoMaster(sut, port, seed)
                    createPythonTestRunner(EVOMASTER_BB,sut, port)
                    port = port + delta

                elif tool == ARAT_RL :
                    createScriptForARATRL(sut,port,seed)
                    port = port + delta

                elif tool == SCHEMATHESIS :
                    createScriptForSchemaThesis(sut,port,seed)
                    port = port + delta

                elif tool == RESTLER :
                    createScriptForRestler(sut,port,seed)
                    port = port + delta

                elif tool == LLAMARESTTEST :
                    createScriptForLLAMA(sut,port,seed)
                    port = port + delta

                elif tool == EMREST :
                    createScriptForEmRest(sut,port,seed)
                    port = port + delta

                else:
                    print("Not recognized tool: " + tool)
                    exit(1)



#############################################################################################
### Custom code for each tool
#############################################################################################

EVOMASTER_BB="evomasterbb"
ARAT_RL="aratrl"
LLAMARESTTEST="llamaresttest"
SCHEMATHESIS="schemathesis"
RESTLER="restler"
EMREST="emrest"


def getTestDir(tool,sutname,port):
    return pathlib.PurePath(TESTS_DIR + "/"+ tool  + "/" + sutname + "/" + str(port)).as_posix()


def getReportTimeout(logs):
    command =  "if [[ $? -eq 124 ]]; then \n"
    command += "  echo \"###################################################################\" >> " + logs + " \n"
    command += "  echo \"### ERROR TIMEOUT: execution of tool was stopped due to timeout ###\" >> " + logs + " \n"
    command += "  echo \"###################################################################\" >> " + logs + " \n"
    command += "fi\n"
    return command

# Needed to get boot-time coverage
def createScriptForBase(sut, port):
    tool = "base"

    code = (getScriptHead(port, tool, sut, EXEC_ONLINE_DIR)
            # don't run any tool here
            + getScriptFooter(port, tool, sut, EXEC_ONLINE_DIR, JACOCO_ONLINE_DIR))
    writeScript(SCRIPT_DIR,code, port, tool, sut)


def createPythonTestRunner(tool,sut,port):

    TEST_RUNNER_LABEL = "tests-"

    testdir = getTestDir(tool,sut.name,port)
    logs = getLogFile("runningtests",TEST_RUNNER_LABEL+tool,sut.name,port)

    command = "py -m pytest \"" + testdir +"\" --junitxml=\"" + testdir +"/test-results.xml\" " +  getRedirectLog(logs)

    code = (getScriptHead(port, TEST_RUNNER_LABEL+tool, sut, EXEC_TEST_DIR)
            + command
            + getScriptFooter(port, TEST_RUNNER_LABEL+tool, sut, EXEC_TEST_DIR, JACOCO_TEST_DIR))
    writeScript(RESULT_SCRIPT_DIR,code, port, tool, sut)


def createScriptForARATRL(sut, port, seed):
    tool = ARAT_RL

    logs = getLogFile("tool",tool,sut.name,port)
    testdir = getTestDir(tool,sut.name,port)

    ## start_tool
    start_tool_command = "mkdir -p \"" + testdir + "\" \n"
    start_tool_command += "pushd \"" + testdir + "\" \n\n"

    start_tool_command += TIMEOUT_COMMAND + " "
    start_tool_command += "py \"" + ARAT_RL_LOCATION + "\" \\\n"
    start_tool_command += " \"" + WFD_DIR + "/openapi-swagger/" + sut.name + "."+sut.schemaformat + "\"  \\\n"
    start_tool_command += " http://localhost:" + str(port) + "  \\\n"
    start_tool_command += " " + str(MAX_TIME_SECONDS) + "  \\\n"
    start_tool_command += getRedirectLog(logs)
    start_tool_command += "\n\n"
    start_tool_command += getReportTimeout(logs) + "\n\n"

    start_tool_command += "popd \n\n"

    code = (getScriptHead(port, tool, sut, EXEC_ONLINE_DIR)
            + start_tool_command
            + getScriptFooter(port, tool, sut, EXEC_ONLINE_DIR, JACOCO_ONLINE_DIR))
    writeScript(SCRIPT_DIR,code, port, tool, sut)


def createScriptForEvoMaster(sut, port, seed):
    tool = EVOMASTER_BB

    logs = getLogFile("tool",tool,sut.name,port)
    testdir = getTestDir(tool,sut.name,port)

    ## start_tool
    start_tool_command = TIMEOUT_COMMAND + " \\\n"
    start_tool_command += "java -jar \"" + EVOMASTER_JAR + "\" \\\n"
    start_tool_command += " --blackBox true  \\\n"
    start_tool_command += " --maxTime " + str(MAX_TIME_SECONDS) + "s  \\\n"
    start_tool_command += " --bbSwaggerUrl \"" + WFD_DIR + "/openapi-swagger/" + sut.name + "."+sut.schemaformat + "\"  \\\n"
    start_tool_command += " --bbTargetUrl http://localhost:" + str(port) + "  \\\n"
    start_tool_command += " --seed " + str(seed) + " \\\n"
    start_tool_command += " --showProgress false  \\\n"
    start_tool_command += " --createConfigPathIfMissing false \\\n"
    start_tool_command += " --outputFilePrefix test_evomaster \\\n"
    start_tool_command += " --outputFileSuffix \"_\" \\\n"
    start_tool_command += " --outputFolder \""+testdir+"\"  \\\n"
    start_tool_command += " --writeWFCReport true \\\n"
    if sut.auth:
        start_tool_command += " --configPath \""+WFD_DIR+"/auth/"+sut.name+"-auth.yaml\" \\\n"
        start_tool_command += " --overrideAuthExternalEndpointURL localhost:" + str(port+2)

    start_tool_command += getRedirectLog(logs)
    start_tool_command += "\n"
    start_tool_command += getReportTimeout(logs) + "\n\n"

    code = (getScriptHead(port, tool, sut, EXEC_ONLINE_DIR)
            + start_tool_command
            + getScriptFooter(port, tool, sut, EXEC_ONLINE_DIR, JACOCO_ONLINE_DIR))
    writeScript(SCRIPT_DIR,code, port, tool, sut)


def createScriptForLLAMA(sut, port, seed):
    tool = LLAMARESTTEST

    logs = getLogFile("tool",tool,sut.name,port)
    testdir = getTestDir(tool,sut.name,port)

    ## start_tool
    # need to start 3 levels down from where model files are stored
    start_tool_command = "pushd \"" + LLAMA_REST_TEST_LOCATION + "/x/y/z\" \n\n"

    start_tool_command += TIMEOUT_COMMAND + " "
    start_tool_command += "py \"" + LLAMA_REST_TEST_LOCATION + "/llamarest.py\" \\\n"
    start_tool_command += " \"" + WFD_DIR + "/openapi-swagger/" + sut.name + "."+sut.schemaformat + "\"  \\\n"
    start_tool_command += " http://localhost:" + str(port) + "  \\\n"
    start_tool_command += getRedirectLog(logs)
    start_tool_command += "\n\n"
    start_tool_command += getReportTimeout(logs) + "\n\n"

    start_tool_command += "popd \n\n"

    code = (getScriptHead(port, tool, sut, EXEC_ONLINE_DIR)
            + start_tool_command
            + getScriptFooter(port, tool, sut, EXEC_ONLINE_DIR, JACOCO_ONLINE_DIR))
    writeScript(SCRIPT_DIR,code, port, tool, sut)


def createScriptForSchemaThesis(sut, port, seed):
    tool = SCHEMATHESIS

    logs = getLogFile("tool",tool,sut.name,port)
    testdir = getTestDir(tool,sut.name,port)

    start_tool_command = TIMEOUT_COMMAND + " "
    start_tool_command += "schemathesis run \\\n"
    start_tool_command += " \"" + WFD_DIR + "/openapi-swagger/" + sut.name + "."+sut.schemaformat + "\"  \\\n"
    start_tool_command += " --url http://localhost:" + str(port) + "  \\\n"
    start_tool_command += getRedirectLog(logs)
    start_tool_command += "\n\n"
    start_tool_command += getReportTimeout(logs) + "\n\n"

    code = (getScriptHead(port, tool, sut, EXEC_ONLINE_DIR)
            + start_tool_command
            + getScriptFooter(port, tool, sut, EXEC_ONLINE_DIR, JACOCO_ONLINE_DIR))
    writeScript(SCRIPT_DIR,code, port, tool, sut)



def createScriptForRestler(sut, port, seed):
    tool = RESTLER

    logs = getLogFile("tool",tool,sut.name,port)
    testdir = getTestDir(tool,sut.name,port)

    ## start_tool
    start_tool_command = "mkdir -p \"" + testdir + "\" \n"
    start_tool_command += "pushd \"" + testdir + "\" \n\n"

    start_tool_command += TIMEOUT_COMMAND + " "
    start_tool_command += "py \"" + RESTLER_LOCATION + "/restler-quick-start.py\" \\\n"
    start_tool_command += " --restler_drop_dir \"" + RESTLER_LOCATION +"/restler_bin\" \\\n"
    start_tool_command += " --api_spec_path \"" + WFD_DIR + "/openapi-swagger/" + sut.name + "."+sut.schemaformat + "\"  \\\n"
    start_tool_command += " --host localhost \\\n"
    start_tool_command += " --port " + str(port) + "  \\\n"
    start_tool_command += getRedirectLog(logs)
    start_tool_command += "\n\n"
    start_tool_command += getReportTimeout(logs) + "\n\n"

    start_tool_command += "popd \n\n"

    code = (getScriptHead(port, tool, sut, EXEC_ONLINE_DIR)
            + start_tool_command
            + getScriptFooter(port, tool, sut, EXEC_ONLINE_DIR, JACOCO_ONLINE_DIR))
    writeScript(SCRIPT_DIR,code, port, tool, sut)


def createScriptForEmRest(sut, port, seed):
    tool = EMREST

    logs = getLogFile("tool",tool,sut.name,port)
    testdir = getTestDir(tool,sut.name,port)

    start_tool_command = "mkdir -p \"" + testdir + "\" \n"
    start_tool_command += "pushd \"" + EMREST_LOCATION + "\" \n\n"

    start_tool_command += TIMEOUT_COMMAND + " "
    start_tool_command += "conda run -n emrest python -m src.alg \\\n"
    start_tool_command += " --spec_file \"" + WFD_DIR + "/openapi-swagger/" + sut.name + "."+sut.schemaformat + "\"  \\\n"
    start_tool_command += " --budget " + str(MAX_TIME_SECONDS) + "  \\\n"
    start_tool_command += " --server http://localhost:" + str(port) + "  \\\n"
    start_tool_command += " --exp_name emrest  --pict lib/pict-mac \\\n"
    start_tool_command += " --output_path \""+ testdir + "\" \n"
    start_tool_command += getRedirectLog(logs)
    start_tool_command += "\n\n"
    start_tool_command += getReportTimeout(logs) + "\n\n"

    start_tool_command += "popd \n\n"

    code = (getScriptHead(port, tool, sut, EXEC_ONLINE_DIR)
            + start_tool_command
            + getScriptFooter(port, tool, sut, EXEC_ONLINE_DIR, JACOCO_ONLINE_DIR))
    writeScript(SCRIPT_DIR,code, port, tool, sut)


### end custom tool code
#############################################################################################
### Entry point of the script after input validation

# first, create needed folders
os.makedirs(LOGS_DIR)
os.makedirs(TESTS_DIR)
os.makedirs(SCRIPT_DIR)
os.makedirs(RESULT_SCRIPT_DIR)
os.makedirs(JACOCO_ONLINE_DIR)
os.makedirs(JACOCO_TEST_DIR)
os.makedirs(EXEC_ONLINE_DIR)
os.makedirs(EXEC_TEST_DIR)


#############################################################################################
### Custom code for experiments

# tools to use. comment out the ones which should not be used for these experiments
BB_TOOLS = [
    EVOMASTER_BB,
    ARAT_RL,
    LLAMARESTTEST,
    SCHEMATHESIS,
    RESTLER,
#     EMREST # only on Mac
]

if len(sys.argv) == 5:
    BB_TOOLS = sys.argv[4].split(",")

EVOMASTER_JAR =    BB_TOOL_DIR + "/evomaster.jar"
ARAT_RL_LOCATION = BB_TOOL_DIR + "/arat-rl/arat-rl.py"
LLAMA_REST_TEST_LOCATION = BB_TOOL_DIR + "/LlamaRestTest"
RESTLER_LOCATION = BB_TOOL_DIR + "/restler-fuzzer"
EMREST_LOCATION = BB_TOOL_DIR + "/EmRest/EmRest_core"

#############################################################################################
# main entry point: create all jobs
createJobs()

