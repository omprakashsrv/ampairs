chmod 700 sandbox.pem
chmod +x ./gradlew
./gradlew :ampairs_service:clean
./gradlew :ampairs_service:build
./gradlew :ampairs_service:assemble
ssh -i sandbox.pem ec2-user@3.7.215.40 'mkdir ~/microservices/all'
scp -i sandbox.pem ./build/libs/ampairs_service.jar ec2-user@3.7.215.40:~/microservices/all/
ssh -i sandbox.pem ec2-user@3.7.215.40 'java -Dspring.profiles.active=prod -jar ~/microservices/all/ampairs_service.jar &disown'
