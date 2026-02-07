cd ../../
./gradlew client:bootJar
cd client/build/libs
sudo java --enable-native-access=ALL-UNNAMED -jar client.jar --spring.profiles.active=prod