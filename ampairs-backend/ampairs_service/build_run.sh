chmod 700 ampairs.pem
chmod +x ./gradlew
./gradlew :ampairs_service:clean
./gradlew :ampairs_service:build
./gradlew :ampairs_service:assemble
mkdir ./latest
cp ./build/libs/ampairs_service.jar ./latest
cp ./src/main/resources/application-prod.yml ./latest/application.yml
tar -czf build.tar.gz ./latest/

scp -i ampairs.pem ./build.tar.gz ubuntu@13.203.135.159:~/builds/

ssh -i ampairs.pem ubuntu@13.203.135.159 \
'tar -xzf ~/builds/build.tar.gz -C ~/builds/ && \
sudo systemctl stop ampairs.service && \
sudo cp -r ~/builds/latest/* /var/lib/ampairs/ && \
sudo systemctl start ampairs.service'
