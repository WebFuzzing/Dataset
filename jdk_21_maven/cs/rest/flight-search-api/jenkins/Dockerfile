FROM jenkins/jenkins:lts

# Plugin list
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# For Groovy scripts, init.d directory
COPY init.groovy.d/ /var/jenkins_home/init.groovy.d/

# Install Docker CLI
USER root
RUN apt-get update && apt-get install -y docker.io
