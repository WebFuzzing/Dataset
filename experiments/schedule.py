#!/usr/bin/env python

# When we generate an experiment folder (FOLDER) with exp.py script on local machine,
# with K generated bash scripts, we can use this schedule.py to run all these scripts
# with N of them in parallel. The others (assuming N << K) will be started as soon as
# one current running job is completed

import random
import sys
import os
import subprocess
import time
import platform
import pathlib
import psutil

# Note: here we for flush on ALL prints, otherwise we would end up with messed up logs

if len(sys.argv) != 4:
    print("Usage:\nschedule.py <N> <FOLDER> <TIMEOUT_MINUTES>", flush=True)
    exit(1)

# The number of jobs to run in parallel
N = int(sys.argv[1])

if N < 1:
    print("Invalid value for N: " + str(N), flush=True)
    exit(1)

# Location of experiment folder
FOLDER = sys.argv[2]

TIMEOUT_MINUTES = int(sys.argv[3])

if TIMEOUT_MINUTES < 1:
    print("Invalid value for TIMEOUT_MINUTES: " + str(TIMEOUT_MINUTES), flush=True)

SHELL = platform.system() == 'Windows'

SCRIPTS_FOLDER = pathlib.PurePath(FOLDER).as_posix()

def checkDocker():
    try:
        # Run 'docker info' command to check if Docker is running
        result = subprocess.run(
            ['docker', 'info'],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )

        # If the command failed, Docker is likely not running
        if result.returncode != 0:
            print("Docker is not running. Error:", result.stderr, file=sys.stderr, flush=True)
            sys.exit(1)

        # Unfortunately it seems by default Docker has very low network count...
        # So must make sure to clean up any un-used ones.
        # Had issues where previous experiments did not clean up properly, and all new failed for
        # lack of available networks
        print("Going to prune all unused networks ('docker network prune -f').", result.stderr, file=sys.stderr, flush=True)
        result = subprocess.run(
            ['docker', 'network', 'prune', '-f'],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        if result.returncode != 0:
            print("Failed to prune networks. Error:", result.stderr, file=sys.stderr, flush=True)
            sys.exit(1)

        return True
    except FileNotFoundError:
        print("Error: Docker is not installed or not in PATH", file=sys.stderr, flush=True)
        sys.exit(1)
    except Exception as e:
        print(f"Unexpected error checking Docker: {str(e)}", file=sys.stderr, flush=True)
        sys.exit(1)

checkDocker()

buffer = []

#collect name of all bash files
scripts = [f for f in os.listdir(SCRIPTS_FOLDER) if os.path.isfile(os.path.join(SCRIPTS_FOLDER, f))  and f.endswith(".sh")]

print("There are " + str(len(scripts)) + " Bash script files", flush=True)

random.shuffle(scripts)

k = 1

def runScript(s):
    global k
    print("Running script " + str(k)+ "/"+ str(len(scripts)) +": " + s, flush=True)
    k = k + 1

    command = ["bash", s]

    handler = subprocess.Popen(command, shell=SHELL, cwd=SCRIPTS_FOLDER, start_new_session=True)
    buffer.append(handler)

def killProcess(h):
    print("Terminating process.", flush=True)
    parent = psutil.Process(h.pid)
    children = parent.children(recursive=True)

    # Graceful terminate
    for p in children:
        p.terminate()
    parent.terminate()

    gone, alive = psutil.wait_procs(children + [parent], timeout=10)

    # Force kill remaining
    for p in alive:
        print(f"Force killing PID {p.pid}")
        p.kill()

    h.wait()


########################################################################################################################

last_start = time.time()

for s in scripts:
    if len(buffer) < N:
       last_start = time.time()
       runScript(s)
    else:
        while len(buffer) == N:
            for h in buffer:
                h.poll()
                if h.returncode is not None and h.returncode != 0:
                    print("Process terminated with code: " + str(h.returncode), flush=True)

            # keep the ones running... those have return code not set yet
            buffer = [h for h in buffer if h.returncode is None]
            if len(buffer) == N :
                # all running in buffer... but has any timeout?
                # TODO for simplicity we just check latest added... so timeout is not enforced for ALL jobs.
                # however, note that internally the jobs have their own timeouts... these here are just extra checks
                elapsed_time = time.time() - last_start
                if elapsed_time > TIMEOUT_MINUTES * 60:
                    killProcess(buffer[0])
                # wait before checking again
                time.sleep(5)
            else:
                last_start = time.time()
                runScript(s)
                break

print("Waiting for last scripts to end", flush=True)

budget = TIMEOUT_MINUTES * 60

for h in buffer:
    start = time.time()
    try:
        h.wait(budget)
        if h.returncode != 0:
            print("Process terminated with code: " + str(h.returncode), flush=True)
    except subprocess.TimeoutExpired:
        print("Timeout reached.", flush=True)
        killProcess(h)
    elapsed = time.time() - start
    budget = max(0, budget - elapsed)

print("All jobs are completed", flush=True)



