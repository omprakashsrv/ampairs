chmod +x ./gradlew
./gradlew :auth:clean
./gradlew :auth:build
./gradlew :auth:assemble
scp -i sandbox.pem ./build/libs/auth.jar ec2-user@3.7.215.40:microservices/auth/
ssh -i sandbox.pem ec2-user@3.7.215.40 'java -jar ~/microservices/auth/auth.jar &disown'
