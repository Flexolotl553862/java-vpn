FROM eclipse-temurin:25-jre
COPY ./build/libs/server.jar /app/server.jar
WORKDIR /app
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "server.jar", "--spring.profiles.active=prod"]