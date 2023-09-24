chmod 700 sandbox.pem
chmod +x ./gradlew
./gradlew :product:clean
./gradlew :product:build
./gradlew :product:assemble
scp -i sandbox.pem ./build/libs/product.jar ec2-user@3.7.215.40:~/microservices/product/
ssh -i sandbox.pem ec2-user@3.7.215.40 'java -Dspring.profiles.active=prod -jar ~/microservices/product/product.jar &disown'
