<div align="center">

# Java VPN

[![Verification](https://github.com/Flexolotl553862/java-vpn/actions/workflows/verify.yml/badge.svg)](https://github.com/Flexolotl553862/java-vpn/actions/workflows/verify.yml)
![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?logo=springboot&logoColor=white)
![QUIC](https://img.shields.io/badge/transport-QUIC-635BFF)

Fast encrypted tunnels, mutual certificate authentication, and a friendly interactive CLI.

</div>

## Getting Started

### Prerequisites

- Linux or macOS operating system
- Java 25 or later
- Git

### How it will look like

![demo](./docs/demo.gif)

### Generate certificates

Run next commands only on your private device, and store your keys to the safe place.

1. Clone this repository:

   ```bash
   git clone https://github.com/Flexolotl553862/java-vpn.git
   ```

2. Create directory for certificates:

   ```bash
   mkdir -p certs
   ```
3. Set your server address and domain name, if you don't have any domain leave a DNS value empty:

   ```bash
   SERVER_DNS="vpn.example.com"
   SERVER_IP="0.0.0.0"
   ```

4. Generate the CA private key and certificate:

   ```bash
   openssl req -x509 -newkey rsa:4096 -sha256 -days 3650 -nodes \
     -keyout certs/ca-key.pem \
     -out certs/ca-cert.pem \
     -subj "/CN=java-vpn Root CA" \
     -addext "basicConstraints=critical,CA:TRUE" \
     -addext "keyUsage=critical,keyCertSign,cRLSign" \
   && openssl pkey -in certs/ca-key.pem
   ```

5. Generate and sign the VPN server certificate:

   Create `certs/server.ext`:

   ```bash
   cat > certs/server.ext <<EOF
   basicConstraints=critical,CA:FALSE
   subjectAltName=IP:${SERVER_IP}${SERVER_DNS:+,DNS:${SERVER_DNS}}
   keyUsage=critical,digitalSignature,keyEncipherment
   extendedKeyUsage=serverAuth
   EOF
   ```

   ```bash
   openssl req -new -newkey rsa:2048 -sha256 -nodes \
     -keyout certs/server-key.pem \
     -out certs/server.csr.pem \
     -subj "/CN=${SERVER_DNS:-$SERVER_IP}" \
   && openssl x509 -req -in certs/server.csr.pem \
     -CA certs/ca-cert.pem -CAkey certs/ca-key.pem \
     -CAserial certs/ca-cert.srl -CAcreateserial \
     -out certs/server-cert.pem -days 825 -sha256 \
     -extfile certs/server.ext
   ```

6. Generate and sign the client certificate. Its common name must be the client's internal VPN address:

   ```bash
   CLIENT_IP="10.2.1.17"
   ```

   Create `certs/client.ext`:

   ```bash
   cat > certs/client.ext <<EOF
   basicConstraints=critical,CA:FALSE
   subjectAltName=IP:${CLIENT_IP}
   keyUsage=critical,digitalSignature,keyEncipherment
   extendedKeyUsage=clientAuth
   EOF
   ```

   ```bash
   openssl req -new -newkey rsa:2048 -sha256 -nodes \
     -keyout certs/client-key.pem \
     -out certs/client.csr.pem \
     -subj "/CN=${CLIENT_IP}" \
   && openssl x509 -req -in certs/client.csr.pem \
     -CA certs/ca-cert.pem -CAkey certs/ca-key.pem \
     -CAserial certs/ca-cert.srl -CAcreateserial \
     -out certs/client-cert.pem -days 825 -sha256 \
     -extfile certs/client.ext
   ```

### Launch a database

You are expected to run a database instance on your own server

1. Copy [docker-compose.yml](./server/docker-compose.yml) on your server, and execute next command in the same
   directory:

   ```bash
   docker-compose up -d
   ```

### Launch a server container

1. Pull the image and run a container:
    ```bash
    docker pull andrey820/java-vpn-server:latest
    docker stop java-vpn-server
    docker rm java-vpn-server
    docker run -d \
        --network=host \
        --cap-add=NET_ADMIN \
        --device /dev/net/tun \
        -e SERVER_HOST=0.0.0.0 \
        -e SERVER_PORT=18083 \
        -e TUN_MOCK=none \
        -e VPN_NET=10.2.0.0 \
        -e VPN_NET_LENGTH=16 \
        -e OUT_IFACE=eth0 \
        -e LOGGING_LEVEL=INFO \
        -e POSTGRES_URL=jdbc:postgresql://127.0.0.1:5432/java_vpn_server \
        -e POSTGRES_USER=test \
        -e POSTGRES_PASSWORD=test \
        --name java-vpn-server \
        andrey820/java-vpn-server:latest
    ```

2. Check container logs:
    ```bash
    docker logs java-vpn-server -f
    ```

   you will see that application failed because no valid server certificate were found. It's expected behavior, you must
   add certificates first.

3. Connect to your database using DBeaver, IntelliJ IDEA or something else. Add server and client certificates using next script:

   ```sql
   insert into ca_certificate(cert_pem, description)
   values (:ca_cert, 'java-vpn Root CA');

   insert into server_certificate(cert_pem, private_key_pem, preferred)
   values (:server_cert, :server_key, true);

   insert into client(internal_address)
   values ('10.2.1.17');

   insert into client_certificate(cert_pem, client_address, trusted)
   values (:client_cert, '10.2.1.17', true);
   ```
   
4. Restart server container:

   ```bash
   docker restart java-vpn-server
   ```

### Run a client

1. In cloned repository execute:

   ```bash
   ./gradlew client:bootJar
   sudo java -Duser.home="$HOME" --enable-native-access=ALL-UNNAMED -jar client/build/libs/client.jar
   ```
   
2. Create a new VPN profile using next command:

   ```
   profile save --name Test --host 127.0.0.1 --port 8083 --chain path/to/cert/chain.pem --key path/to/private/key.pem
   ```
   
   And then connect:
   
   ```
   connect Test
   ```