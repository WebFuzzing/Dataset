# start multiple app server instances for load testing
nohup java -DServer.port=8080 -jar ../target/redis-0.0.1-SNAPSHOT.jar > /tmp/appServer8080.out 2>&1 &
nohup java -DServer.port=8081 -jar ../target/redis-0.0.1-SNAPSHOT.jar > /tmp/appServer8081.out 2>&1 &
nohup java -DServer.port=8082 -jar ../target/redis-0.0.1-SNAPSHOT.jar > /tmp/appServer8082.out 2>&1 &
nohup java -DServer.port=8083 -jar ../target/redis-0.0.1-SNAPSHOT.jar > /tmp/appServer8083.out 2>&1 &
nohup java -DServer.port=8084 -jar ../target/redis-0.0.1-SNAPSHOT.jar > /tmp/appServer8084.out 2>&1 &
nohup java -DServer.port=8085 -jar ../target/redis-0.0.1-SNAPSHOT.jar > /tmp/appServer8085.out 2>&1 &
