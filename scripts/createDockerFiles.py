#!/usr/bin/env python

import os
import pandas as pd
import shutil

SCRIPT_LOCATION = os.path.dirname(os.path.realpath(__file__))
SUTS_LOCATION = os.path.join(SCRIPT_LOCATION, 'dockerize/data/sut.csv')
GENERATOR_LOCATION = os.path.join(SCRIPT_LOCATION, 'dockerize/docker_generator.py')

LEGACY_ADDITIONAL_FILES = os.path.join(SCRIPT_LOCATION, 'dockerize/data/additional_files')
if os.path.isdir(LEGACY_ADDITIONAL_FILES):
    raise EnvironmentError(
        f"'{LEGACY_ADDITIONAL_FILES}' should not exist anymore. "
        "Additional files must be put under 'dockerfiles/additional_files'."
    )

PYTHON_CMD = shutil.which('python3') or shutil.which('python')
if not PYTHON_CMD:
    raise EnvironmentError("Could not find Python interpreter")

suts = pd.read_csv(SUTS_LOCATION)
dockerized_suts = suts[suts['Dockerized'] == True]
EXPOSE_PORT = 8080

for _, sut in dockerized_suts.iterrows():
    os.system(f"{PYTHON_CMD} {GENERATOR_LOCATION} {sut['NAME']} {EXPOSE_PORT}")
