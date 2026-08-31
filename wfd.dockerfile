FROM alpine

COPY auth        /wfd/auth
COPY dockerfiles /wfd/dockerfiles
COPY openapi     /wfd/openapi



###################
###### NOTES ######
###################
# Build
# docker build -t webfuzzing/wfd:<version>  -f wfd.dockerfile .
#
# Run
# docker run webfuzzing/wfd:<version>  <options>
#
# Publish (latest, otherwise tag with :<version>)
# docker login
# docker push webfuzzing/wfd
#
# Debugging
# docker run -it --entrypoint sh  webfuzzing/wfd:<version>