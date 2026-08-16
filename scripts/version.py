#!/usr/bin/env python3

import sys
import re
import platform
import os
from subprocess import run

if len(sys.argv) != 3:
    print("Usage:\n<nameOfScript>.py wfd|em <version-number>")
    exit(1)

target = sys.argv[1].strip()

if target != "wfd" and target != "em":
    println("Invalid target: " + target+". Only accepted 'wfd' and 'em' as valid targets")
    exit(1)

version = sys.argv[2].strip()

versionRegex = re.compile(r"^(\d)+\.(\d)+\.(\d)+(-SNAPSHOT)?$")

if versionRegex.match(version) == None:
    print("Invalid version format")
    exit(1)


SCRIPT_LOCATION = os.path.dirname(os.path.realpath(__file__))
PROJ_LOCATION = os.path.abspath(os.path.join(SCRIPT_LOCATION, os.pardir))

JAVA_HOME_8 = os.environ.get('JAVA_HOME_8', '')
if JAVA_HOME_8 == '':
    print("\nERROR: JAVA_HOME_8 environment variable is not defined")
    exit(1)

JAVA_HOME_11 = os.environ.get('JAVA_HOME_11', '')
if JAVA_HOME_11 == '':
    print("\nERROR: JAVA_HOME_11 environment variable is not defined")
    exit(1)

JAVA_HOME_17 = os.environ.get('JAVA_HOME_17', '')
if JAVA_HOME_17 == '':
    print("\nERROR: JAVA_HOME_17 environment variable is not defined")
    exit(1)

JAVA_HOME_21 = os.environ.get('JAVA_HOME_21', '')
if JAVA_HOME_21 == '':
    print("\nERROR: JAVA_HOME_21 environment variable is not defined")
    exit(1)

JAVA_HOME_25 = os.environ.get('JAVA_HOME_25', '')
if JAVA_HOME_25 == '':
    print("\nERROR: JAVA_HOME_25 environment variable is not defined")
    exit(1)


SHELL = platform.system() == 'Windows'

def replaceInPom(file):
    regex = re.compile(r'.*<evomaster-version>.*</evomaster-version>.*')
    replacement = '        <evomaster-version>'+version+'</evomaster-version>\n'
    replace(file, regex, replacement)

def replace(file, regex, replacement):

    with open(file, "r") as sources:
        lines = sources.readlines()
    with open(file, "w") as sources:
        for line in lines:

            if regex.match(line):
                sources.write(replacement)
            else:
                sources.write(line)

def replaceInDist():
    regex = re.compile(r'.*EVOMASTER_VERSION.*=.*".*".*')
    replacement = 'EVOMASTER_VERSION = "'+version+'"\n'
    replace("scripts/dist.py", regex, replacement)

def replaceInDockerEnv():
    regex = re.compile(r'^EVOMASTER_VERSION\s*=\s*.*$')
    replacement = 'EVOMASTER_VERSION='+version+'\n'
    replace("scripts/build/.env", regex, replacement)

def replaceInProperty(file):
    regex = re.compile(r'.*EVOMASTER_VERSION.*=.*')
    replacement = 'EVOMASTER_VERSION='+version+'\n'
    replace(file, regex, replacement)

def replaceInGradle(file):
    regex = re.compile(r'.*EVOMASTER_VERSION.*=.*')
    replacement = "        EVOMASTER_VERSION = \""+version+"\"\n"
    replace(file, regex, replacement)

def versionSetMaven(folder, jdk_home):

    env_vars = os.environ.copy()
    env_vars["JAVA_HOME"] = jdk_home

    # Note: this will change version in sub-modules only if those have the same groupId. So the ones in CS are unaffected
    mvnres = run(["mvn", "versions:set", "-DnewVersion="+version], shell=SHELL, cwd=PROJ_LOCATION+folder, env=env_vars)
    mvnres = mvnres.returncode

    if mvnres != 0:
        print("\nERROR: Maven command failed")
        exit(1)


# def replaceInJS(folder):
#     regex = re.compile(r'\s*"evomaster-client-js"\s*:.*')
#     replacement = ""
#     if version.endswith("-SNAPSHOT"):
#         replacement = "    \"evomaster-client-js\": \"file:../../evomaster-client-js\",\n"
#     else:
#         replacement = "    \"evomaster-client-js\": \""+version+"\",\n"
#     replace(PROJ_LOCATION+folder+"/package.json", regex, replacement)
#
#     # Note: as we need to update the lock files, then we have to make an install
#     res = run(["npm", "i"], shell=SHELL, cwd=PROJ_LOCATION+folder)
#     res = res.returncode
#
#     if res != 0:
#         print("\nERROR: 'npm i' command failed")
#         exit(1)
#
# def replaceAllJs():
#     replaceInJS("/js_npm/rest/cyclotron")
#     replaceInJS("/js_npm/rest/disease-sh-api")
#     replaceInJS("/js_npm/rest/ncs")
#     replaceInJS("/js_npm/rest/realworld-app")
#     replaceInJS("/js_npm/rest/scs")
#     replaceInJS("/js_npm/rest/spacex-api")
#
# def replaceInCS():
#     regex = re.compile(r'\s*<PackageReference\s+Include="EvoMaster.Controller"\s+Version=".*"\s+/>\s*')
#     replacement = '         <PackageReference Include="EvoMaster.Controller" Version="'+version+'" />\n'
#     replace("dotnet_3/em/common.props", regex, replacement)


######################################################################################################

if target == "em":
    replaceInPom("jdk_8_maven/pom.xml")
    replaceInPom("jdk_11_maven/pom.xml")
    replaceInPom("jdk_17_maven/pom.xml")
    replaceInPom("jdk_21_maven/pom.xml")
    replaceInPom("jdk_25_maven/pom.xml")

    replaceInGradle("jdk_8_gradle/build.gradle")
    replaceInGradle("jdk_11_gradle/build.gradle")
    replaceInGradle("jdk_17_gradle/build.gradle")
    replaceInDist()
    replaceInDockerEnv()

if target == "wfd":
    versionSetMaven("/jdk_8_maven",JAVA_HOME_8)
    versionSetMaven("/jdk_11_maven",JAVA_HOME_11)
    versionSetMaven("/jdk_17_maven",JAVA_HOME_17)
    versionSetMaven("/jdk_21_maven",JAVA_HOME_21)
    versionSetMaven("/jdk_25_maven",JAVA_HOME_25)

# We no longer support those in EMB... not enough resources. although might change in future
#replaceAllJs()
#replaceInCS()