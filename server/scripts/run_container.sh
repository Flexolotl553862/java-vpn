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
  --name java-vpn-server \
  andrey820/java-vpn-server:latest