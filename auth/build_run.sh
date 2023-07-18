chmod +x ./gradlew
./gradlew :auth:assemble
cp ./build/libs/auth.jar auth.jar
java -jar auth.jar