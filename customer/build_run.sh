chmod +x ./gradlew
./gradlew :customer:assemble
cp ./build/libs/customer.jar customer.jar
java -jar customer.jar