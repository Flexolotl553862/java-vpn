REMOTE_IP="185.10.19.139"
DEFAULT_GATEWAY="192.168.10.1"
VPN_IFACE="utun4"

sudo route add -host $REMOTE_IP $DEFAULT_GATEWAY

sudo route add -net 127.0.0.0/1 -interface $VPN_IFACE
sudo route add -net 128.0.0.0/1 -interface $VPN_IFACE