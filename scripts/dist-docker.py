#!/usr/bin/env python3
"""Docker-based build script for WFD project.

Builds all jdk_* projects using Docker containers
and copies the results to the dist folder.
"""

import argparse
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
PROJ_DIR = SCRIPT_DIR.parent

COMPOSE_FILE = "./scripts/build/docker-compose.build.yml"
ENV_FILE = SCRIPT_DIR / "build" / ".env"

JDK_VERSIONS = ["8", "11", "17", "21"]
BUILD_TOOLS = ["maven", "gradle"]

ALL_SERVICES = [
    ("8",  "maven"),
    ("8",  "gradle"),
    ("11", "maven"),
    ("11", "gradle"),
    ("17", "maven"),
    ("17", "gradle"),
    ("21", "maven"),
]


def detect_compose_cmd():
    try:
        subprocess.run(
            ["docker", "compose", "version"],
            check=True, capture_output=True,
        )
        return ["docker", "compose"]
    except (subprocess.CalledProcessError, FileNotFoundError):
        pass
    if shutil.which("docker-compose"):
        return ["docker-compose"]
    print("ERROR: Docker Compose is not installed")
    sys.exit(1)


def format_elapsed_time(seconds):
    m, s = divmod(int(seconds), 60)
    return f"{m}m {s}s" if m else f"{s}s"


def run(cmd, check=True):
    return subprocess.run(cmd, check=check)


def run_build(compose, service, *, background=False, evomaster=False):
    env_args = ["-e", f"BUILD_EVOMASTER={'true' if evomaster else 'false'}"]
    cmd = compose + ["-f", COMPOSE_FILE, "run", "--rm", "-T"] + env_args + [service]
    if background:
        return subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return subprocess.run(cmd, check=False)


def load_env(key):
    for line in ENV_FILE.read_text().splitlines():
        line = line.strip()
        if line.startswith(f"{key}="):
            return line.split("=", 1)[1]
    return None


def copy_additional_files(*, evomaster=False):
    dist = PROJ_DIR / "dist"
    dist.mkdir(parents=True, exist_ok=True)

    jacoco_dir = PROJ_DIR / "jacoco"
    for name in ["jacocoagent.jar", "jacococli.jar"]:
        src = jacoco_dir / name
        if src.exists():
            shutil.copy2(src, dist / name)
            print(f"  Copied {name}")
        else:
            print(f"  WARNING: {src} not found, skipping")

    if evomaster:
        version = load_env("EVOMASTER_VERSION")
        if not version:
            print("  ERROR: EVOMASTER_VERSION not found in .env")
            sys.exit(1)
        agent_name = f"evomaster-client-java-instrumentation-{version}.jar"
        src = Path.home() / ".m2" / "repository" / "org" / "evomaster" / "evomaster-client-java-instrumentation" / version / agent_name
        if src.exists():
            shutil.copy2(src, dist / "evomaster-agent.jar")
            print(f"  Copied evomaster-agent.jar (v{version})")
        else:
            print(f"  ERROR: {src} not found")
            sys.exit(1)

    print("Additional files copied!\n")


def show_jar(path):
    p = PROJ_DIR / "dist" / path
    if p.exists():
        print(f"{p.name}  ({p.stat().st_size // 1024} KB)")


def count_jars():
    return len(list((PROJ_DIR / "dist").glob("**/*.jar")))


def service_dir_exists(jdk, tool):
    return (PROJ_DIR / f"jdk_{jdk}_{tool}").is_dir()


def should_build(jdk, tool, jdk_filter, tool_filter):
    if jdk_filter != "all" and jdk_filter != jdk:
        return False
    if tool_filter != "all" and tool_filter != tool:
        return False
    return True


def parse_args():
    parser = argparse.ArgumentParser(
        description="WFD Docker Build Script",
        formatter_class=argparse.RawTextHelpFormatter,
        epilog=(
            "Examples:\n"
            "  dist-docker.py                    # Build all projects sequentially\n"
            "  dist-docker.py --parallel         # Build all projects in parallel\n"
            "  dist-docker.py 8 gradle           # Build only JDK 8 Gradle projects\n"
            "  dist-docker.py 11 maven -p        # Build JDK 11 Maven in parallel mode\n"
            "  dist-docker.py --copy-files       # Only copy additional files (jacoco)\n"
            "  dist-docker.py --copy-files -E    # Copy additional files (jacoco + evomaster-agent)\n"
            "  dist-docker.py --interactive      # Prompt before deleting dist/ on full build\n"
            "  dist-docker.py --evomaster        # Also build evomaster runners + copy evomaster-agent\n"
            "  dist-docker.py -E --parallel      # Full build in parallel"
        ),
    )
    parser.add_argument(
        "jdk_version",
        nargs="?",
        default="all",
        metavar="JDK_VERSION",
        help="8, 11, 17, 21, or 'all' (default: all)",
    )
    parser.add_argument(
        "build_tool",
        nargs="?",
        default="all",
        metavar="BUILD_TOOL",
        help="maven, gradle, or 'all' (default: all)",
    )
    parser.add_argument(
        "--parallel", "-p",
        action="store_true",
        help="Run builds in parallel (faster but uses more resources)",
    )
    parser.add_argument(
        "--copy-files", "-c",
        action="store_true",
        dest="copy_only",
        help="Only copy additional files (evomaster-agent, jacoco)",
    )
    parser.add_argument(
        "--interactive", "-i",
        action="store_true",
        help="Prompt for confirmation before destructive operations (e.g. deleting dist/)",
    )
    parser.add_argument(
        "--evomaster", "-E",
        action="store_true",
        help="Also build evomaster runner jars (em/) and copy additional files. "
             "Default: SUT-only mode (only cs/ is built, no evomaster runners)",
    )
    args = parser.parse_args()

    valid_jdks = set(JDK_VERSIONS) | {"all"}
    if args.jdk_version not in valid_jdks:
        parser.error(f"Invalid JDK version '{args.jdk_version}'. Valid options: {', '.join(sorted(valid_jdks))}")

    valid_tools = set(BUILD_TOOLS) | {"all"}
    if args.build_tool not in valid_tools:
        parser.error(f"Invalid build tool '{args.build_tool}'. Valid options: {', '.join(sorted(valid_tools))}")

    return args


def main():
    args = parse_args()
    compose = detect_compose_cmd()

    print("WFD Docker Build Script")
    print(f"Project directory: {PROJ_DIR}")
    print(f"JDK Version: {args.jdk_version}")
    print(f"Build Tool:  {args.build_tool}")
    print(f"Mode: {'PARALLEL' if args.parallel else 'Sequential'}")
    print(f"Build scope: {'Full (SUT + Evomaster runners)' if args.evomaster else 'SUT-only (cs/ only)'}")
    print(f"Using: {' '.join(compose)}\n")

    # Check Docker
    if not shutil.which("docker"):
        print("ERROR: Docker is not installed or not in PATH")
        sys.exit(1)

    dist_dir = PROJ_DIR / "dist"

    os.environ.setdefault("HOME", str(Path.home()))

    # --- Copy-only mode ---
    if args.copy_only:
        print("Copy Additional Files Only Mode")
        dist_dir.mkdir(parents=True, exist_ok=True)
        os.chdir(PROJ_DIR)
        copy_additional_files(evomaster=args.evomaster)
        print("Files copied successfully!")
        for f in ["evomaster-agent.jar", "jacocoagent.jar", "jacococli.jar"]:
            show_jar(f)
        return

    # --- Collect services ---
    services = [
        f"build-jdk{jdk}-{tool}"
        for jdk, tool in ALL_SERVICES
        if should_build(jdk, tool, args.jdk_version, args.build_tool)
        and service_dir_exists(jdk, tool)
    ]

    # --- Full-build warning & dist cleanup ---
    if args.jdk_version == "all" and args.build_tool == "all":
        print("WARNING: Full build mode detected!")
        print("This will DELETE the entire dist/ folder and rebuild all projects.\n")
        if dist_dir.exists():
            jar_count = len(list(dist_dir.glob("**/*.jar")))
            print(f"Current dist folder contents:\n  - {jar_count} JAR files found\n  - Location: {dist_dir}")
        else:
            print("Dist folder does not exist yet.")

        if args.interactive:
            print()
            try:
                reply = input("Do you want to continue and DELETE dist folder? (y/N): ").strip().lower()
            except EOFError:
                reply = ""

            if reply not in ("y", "yes"):
                print("\nBuild cancelled by user.")
                print("Tip: Use 'dist-docker.py <JDK> <TOOL>' for incremental builds")
                print("     Example: dist-docker.py 8 gradle")
                return

        print("\nCleaning dist folder (building all projects)...")
        if dist_dir.exists():
            shutil.rmtree(dist_dir)
        dist_dir.mkdir(parents=True)
        print("Dist folder cleaned")
    else:
        print("Incremental build mode - preserving existing dist folder...")
        dist_dir.mkdir(parents=True, exist_ok=True)
        print("Building into existing dist folder (will overwrite duplicates)")
    print()

    # --- Ensure Maven / Gradle cache dirs exist ---
    home = Path.home()
    for cache_name, cache_dir in [(".m2", home / ".m2"), (".gradle", home / ".gradle")]:
        if not cache_dir.exists():
            print(f"WARNING: {cache_name} cache not found at {cache_dir}, creating...")
            cache_dir.mkdir(parents=True)

    os.chdir(PROJ_DIR)

    if not services:
        print("No matching builds found!")
        print(f"JDK Version: {args.jdk_version}\nBuild Tool:  {args.build_tool}\n")
        print("Available combinations:")
        for d in sorted(PROJ_DIR.glob("jdk_*")):
            if d.is_dir():
                print(f"  - {d.name}")
        return

    print("Building Docker images and running builds...")
    print(f"Services to build: {services}\n")

    total = len(services)
    build_start = time.time()

    # Step 1: Build Docker images in parallel (output suppressed to avoid interleaving)
    print(f"Step 1: Building {total} Docker image(s)...")
    img_start = time.time()
    img_pending = {}
    for svc in services:
        proc = subprocess.Popen(
            compose + ["-f", COMPOSE_FILE, "build", svc],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        img_pending[svc] = (proc, time.time())
    done = 0
    while img_pending:
        for svc, (proc, svc_start) in list(img_pending.items()):
            if proc.poll() is not None:
                done += 1
                print(f"  [{done}/{total}] Image ready: {svc}  ({format_elapsed_time(time.time() - svc_start)})")
                del img_pending[svc]
        if img_pending:
            time.sleep(2)
    print(f"All Docker images built! (took {format_elapsed_time(time.time() - img_start)})\n")

    # Step 2: Run builds
    step2_start = time.time()
    print("Step 2: Running builds...")
    if args.parallel:
        print(">>> Running builds in PARALLEL mode...")
        print(f">>> WARNING: This will use significant CPU and RAM!")
        print(f">>> Launching {total} build(s)...\n")

        pending = {}
        for svc in services:
            proc = run_build(compose, svc, background=True, evomaster=args.evomaster)
            pending[svc] = (proc, time.time())
            print(f"  [launched] {svc}")
        print(f"\nWaiting for all {total} builds to complete...\n")

        failed = 0
        done = 0
        while pending:
            for svc, (proc, svc_start) in list(pending.items()):
                code = proc.poll()
                if code is not None:
                    done += 1
                    svc_elapsed = format_elapsed_time(time.time() - svc_start)
                    if code != 0:
                        print(f"  [{done}/{total}] FAILED: {svc}  (exit code {code}, {svc_elapsed})")
                        failed += 1
                    else:
                        print(f"  [{done}/{total}] OK: {svc}  ({svc_elapsed})")
                    del pending[svc]
            if pending:
                time.sleep(2)

        if failed:
            print(f"\nERROR: {failed} build(s) failed!")
            sys.exit(1)

        print(f"\nAll parallel builds completed successfully! (took {format_elapsed_time(time.time() - step2_start)})\n")
    else:
        print(">>> Running builds in SEQUENTIAL mode...\n")
        for i, svc in enumerate(services, 1):
            print(f">>> [{i}/{total}] Building: {svc}")
            svc_start = time.time()
            result = run_build(compose, svc, evomaster=args.evomaster)
            svc_elapsed = format_elapsed_time(time.time() - svc_start)
            if result.returncode != 0:
                print(f"\nERROR: {svc} build failed! (after {svc_elapsed})")
                sys.exit(1)
            print(f"    Completed in {svc_elapsed}\n")

    copy_additional_files(evomaster=args.evomaster)

    # Cleanup
    print("Cleaning up Docker containers...")
    run(compose + ["-f", COMPOSE_FILE, "down"])

    # Summary
    total_elapsed = format_elapsed_time(time.time() - build_start)
    print()
    print("Build Summary")
    print(f"Builds executed: {len(services)}")
    print(f"Total time: {total_elapsed}")
    print("Checking dist folder contents...\n")

    jar_count = count_jars()
    print(f"Total JAR files created: {jar_count}\n")

    if jar_count > 0:
        print("Files in dist:")
        for f in sorted(dist_dir.iterdir()):
            print(f"  {f.name}  ({f.stat().st_size // 1024} KB)")
        print()
        print("SUCCESS - Builds completed!")
        print(f"Output location: {dist_dir}")
        print(f"JDK Version: {args.jdk_version}")
        print(f"Build Tool:  {args.build_tool}")
    else:
        print("WARNING - No JAR files found in dist!")
        sys.exit(1)


if __name__ == "__main__":
    main()
