FROM amazoncorretto:21-alpine-jdk

COPY ./dist/pn-paper-tracker-sut.jar .
COPY ./dist/jacocoagent.jar .




COPY ./dockerfiles/additional_files/pn-paper-tracker/application.properties .

COPY ./dockerfiles/additional_files/pn-paper-tracker/init-localstack.sh .




ENTRYPOINT \
    java \
    -javaagent:jacocoagent.jar=output=tcpserver,address=*,port=6300,append=false,dumponexit=false \
    -Daws.accessKeyId=test -Daws.secretAccessKey=test -jar pn-paper-tracker-sut.jar \
    --server.port=8080 --logging.config=classpath:logback-base.xml --logging.level.io.awspring.cloud=INFO --logging.level.software.amazon.awssdk=INFO --logging.level.software.amazon.awssdk.request=INFO --aws.profile-name= --spring.cloud.aws.credentials.profile-name= --aws.endpoint-url=http://db:4566 --spring.cloud.aws.endpoint=http://db:4566 --spring.cloud.aws.sqs.endpoint=http://db:4566 --pn.paper-tracker.topics.queue-ocr-inputs-url=http://db:4566/000000000000/dl-sqs "--pn.paper-tracker.enable-ocr-validation-for=1970-01-01;AR:DRY;890:DRY"