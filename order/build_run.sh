chmod 700 sandbox.pem
chmod +x ./gradlew
./gradlew :order:clean
./gradlew :order:build
./gradlew :order:assemble
scp -i sandbox.pem ./build/libs/order.jar ec2-user@3.7.215.40:~/microservices/order/
ssh -i sandbox.pem ec2-user@3.7.215.40 'java -Dspring.profiles.active=prod -jar ~/microservices/order/order.jar &disown'
