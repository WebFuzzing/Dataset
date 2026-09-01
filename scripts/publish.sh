#!/bin/bash

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
    echo "Building image: webfuzzing/wfd-$X:FINAL from $file"
    docker build -t "webfuzzing/wfd-$X:FINAL" -f "$file" .

    if [ $? -eq 0 ]; then
        echo "Successfully built webfuzzing/wfd-$X:FINAL"
    else
        echo "Failed to build webfuzzing/wfd-$X:FINAL"
        exit -1
    fi


    echo "---"
done

echo "All builds completed!"