#! /bin/bash -e

echo "Per maggiori informazioni vedere: https://github.com/pagopa/pn-codegen"

tag=$(mvn help:evaluate -Dexpression=pagopa.codegen.version -q -DforceStdout)
echo "Tag from pom.xml: ${tag}"
if [[ ! -z $1 ]]; then
    tag=$1
    echo "Tag from command line ${tag}"
fi
docker run --rm -v $(pwd):/usr/local/app/microsvc --name=pn-codegen ghcr.io/pagopa/pn-codegen:${tag}