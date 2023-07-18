chmod +x ./gradlew
./gradlew :auth:clean
./gradlew :auth:build
./gradlew :auth:assemble
cp ./build/libs/auth.jar auth.jar
java -jar auth.jar