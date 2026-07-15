<div align="center">

# Java VPN

### A small, secure VPN built with Java and QUIC

[![Verification](https://github.com/Flexolotl553862/java-vpn/actions/workflows/verify.yml/badge.svg)](https://github.com/Flexolotl553862/java-vpn/actions/workflows/verify.yml)
![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?logo=springboot&logoColor=white)
![QUIC](https://img.shields.io/badge/transport-QUIC-635BFF)

Fast encrypted tunnels, mutual certificate authentication, and a friendly interactive CLI.

</div>

## Highlights

- QUIC transport powered by Reactor Netty
- Native TUN support for Linux (x64/ARM64) and macOS (Apple Silicon)
- Certificate-based client and server authentication
- Saved VPN profiles, trusted certificates, and custom routes

## Run the client locally

You need **JDK 25**, a running Java VPN server, and the client/server certificates. Build as your regular user, then run with elevated privileges so the client can create a TUN interface and update routes:

```bash
./gradlew :client:bootJar
sudo java -Duser.home="$HOME" --enable-native-access=ALL-UNNAMED \
  -jar client/build/libs/client.jar
```

Inside the interactive shell, trust the server certificate, save a profile, and connect:

```text
trust add /path/to/server_cert.pem
profile save --name home --host vpn.example.com --port 8083 \
  --cert /path/to/client_cert.pem --key /path/to/client_key.pem
connect home
```

Use `help` to explore commands, `status` to check the tunnel, and `disconnect` to close it. Profiles and trusted certificates are stored in `~/.java-vpn`.

> [!NOTE]
> This is an experimental project. Review the code and configuration before using it for sensitive traffic.
