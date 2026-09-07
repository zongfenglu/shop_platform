#!/usr/bin/env bash
# Print (or write) local domain hosts entries for a Linux test server.
#   bash docker/scripts/setup-local-domains.sh
#   sudo bash docker/scripts/setup-local-domains.sh --apply
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

APPLY=0
if [[ "${1:-}" == "--apply" ]]; then
  APPLY=1
fi

domain="shop.test"
if [[ -f .env ]]; then
  line=$(grep -E '^\s*PLATFORM_BASE_DOMAIN\s*=' .env | tail -n 1 || true)
  if [[ -n "$line" ]]; then
    domain=$(echo "$line" | sed -E 's/^\s*PLATFORM_BASE_DOMAIN\s*=\s*//' | tr -d ' "')
  fi
fi

ip=""
for cand in $(hostname -I 2>/dev/null || true); do
  case "$cand" in
    127.*|169.254.*|172.*) continue ;;
    192.168.*) ip="$cand"; break ;;
  esac
done
if [[ -z "$ip" ]]; then
  for cand in $(hostname -I 2>/dev/null || true); do
    case "$cand" in
      10.*) ip="$cand"; break ;;
    esac
  done
fi
if [[ -z "$ip" ]]; then
  ip="服务器IP"
fi

names=(
  "admin.$domain"
  "store.$domain"
  "h5.$domain"
  "www.$domain"
  "$domain"
)

echo "PLATFORM_BASE_DOMAIN = $domain"
echo "Add these lines to /etc/hosts on every tester PC (and this server if you browse locally):"
echo
for n in "${names[@]}"; do
  echo "$ip  $n"
done
echo
echo "URLs:"
echo "  admin  http://admin.$domain"
echo "  store  http://store.$domain"
echo "  H5     http://h5.$domain"
echo

if [[ "$APPLY" -eq 0 ]]; then
  echo "Re-run with --apply (root) to write this server's /etc/hosts."
  echo "Skip this if public DNS already points to this machine."
  exit 0
fi

hosts_path=/etc/hosts
added=0
for n in "${names[@]}"; do
  if grep -qE "[[:space:]]${n}([[:space:]]|$)" "$hosts_path"; then
    continue
  fi
  if [[ "$added" -eq 0 ]]; then
    printf '\n# shop-platform local domains\n' >> "$hosts_path"
  fi
  echo "$ip  $n" >> "$hosts_path"
  added=$((added + 1))
done
if [[ "$added" -eq 0 ]]; then
  echo "hosts already contains these names."
else
  echo "Wrote $added line(s) to $hosts_path."
fi
