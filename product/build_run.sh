chmod +x ./gradlew
./gradlew :product:assemble
cp ./build/libs/product.jar product.jar
java -jar product.jar