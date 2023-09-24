chmod 700 sandbox.pem
chmod +x ./gradlew
./gradlew :customer:clean
./gradlew :customer:build
./gradlew :customer:assemble
scp -i sandbox.pem ./build/libs/customer.jar ec2-user@3.7.215.40:~/microservices/customer/
ssh -i sandbox.pem ec2-user@3.7.215.40 'java -Dspring.profiles.active=prod -jar ~/microservices/customer/customer.jar &disown'
