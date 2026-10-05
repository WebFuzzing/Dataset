#!/bin/bash

###############################################################
### Build all images under "dockerfiles",
### in multi-platform (ie linux/amd64 and linux/arm64) mode.
### However, multi-platform builds require publishing, due to manifest list, registry issues.
###
### To enable multi-platform builds, might require to run following at least once:
### docker buildx create --driver=docker-container --use --name=mybuilder

#### WARNING: should only publish an image, once, and if in any rare occasion of updating.
#### This also requires to run "docker login" with WFC credentials for Docker Hub.
#### As such, only architect can do this operation.

### Need a "docker login" to be able to publish, and so run this script

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Set the dockerfiles directory relative to script location
DOCKERFILES_DIR="${SCRIPT_DIR}/../dockerfiles"

# Check if the dockerfiles directory exists
if [ ! -d "$DOCKERFILES_DIR" ]; then
    echo "Error: Directory $DOCKERFILES_DIR does not exist"
    exit 1
fi

# Find all .dockerfile files and process them
find "$DOCKERFILES_DIR" -maxdepth 1 -name "*.dockerfile" -type f | while read -r file; do
    # Get the filename without path
    filename=$(basename "$file")

    # Extract X from X.dockerfile (remove the .dockerfile extension)
    X="${filename%.dockerfile}"

    # Build the Docker image
    echo "Building and publishing image: webfuzzing/wfd-$X:FINAL from $file"
    docker buildx build --platform linux/amd64,linux/arm64 -t "webfuzzing/wfd-$X:FINAL" -f "$file"  --push .

    if [ $? -eq 0 ]; then
        echo "Successfully built and published webfuzzing/wfd-$X:FINAL"
    else
        echo "Failed to build&publish webfuzzing/wfd-$X:FINAL"
        exit -1
    fi

    echo "---"
done

echo "All builds completed!"