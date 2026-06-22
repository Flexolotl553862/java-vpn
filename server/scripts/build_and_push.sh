cd ../../
./gradlew server:bootJar
cd server
docker buildx build --platform linux/amd64 -t andrey820/java-vpn-server:latest .
docker push andrey820/java-vpn-server:latest