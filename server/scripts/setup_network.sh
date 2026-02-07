#!/bin/sh
set -eu

iptables -D FORWARD -s "$VPN_NET/$VPN_NET_LENGTH" -j ACCEPT 2>/dev/null || true
iptables -D FORWARD -i "$OUT_IFACE" -d "$VPN_NET/$VPN_NET_LENGTH" -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT 2>/dev/null || true
iptables -t nat -D POSTROUTING -s "$VPN_NET/$VPN_NET_LENGTH" -o "$OUT_IFACE" -j MASQUERADE 2>/dev/null || true

iptables -A FORWARD -s "$VPN_NET/$VPN_NET_LENGTH" -j ACCEPT
iptables -A FORWARD -i "$OUT_IFACE" -d "$VPN_NET/$VPN_NET_LENGTH" -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
iptables -t nat -A POSTROUTING -s "$VPN_NET/$VPN_NET_LENGTH" -o "$OUT_IFACE" -j MASQUERADE