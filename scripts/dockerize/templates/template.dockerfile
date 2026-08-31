FROM {{BASE_IMAGE}}

COPY {{EMB_DIR}}/{{SUT_NAME}}-sut.jar .
COPY {{EMB_DIR}}/jacocoagent.jar .

{% if ADDITIONAL_FILES %}

{% for file in files %}
COPY {{file['source']}} .
{% endfor %}
{% endif %}


ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    {{ JVM_PARAMETERS if JVM_PARAMETERS }} -jar {{SUT_NAME}}-sut.jar \
    {{ INPUT_PARAMETERS if INPUT_PARAMETERS }}
