#!/usr/bin/env bash

# This script is used to configure Maven settings.xml to use proxy if needed
set -e

configureProxy() {
  local proxyUrl="$1"

  local settingsFile=~/.m2/settings.xml

  local schm=$( echo "$proxyUrl" | sed -E 's,^(https?)://([^:/]+)(:([0-9]+))?$,\1,' )
  local host=$( echo "$proxyUrl" | sed -E 's,^(https?)://([^:/]+)(:([0-9]+))?$,\2,' )
  local port=$( echo "$proxyUrl" | sed -E 's,^(https?)://([^:/]+)(:([0-9]+))?$,\4,' )
  if [ -z "$port" ]; then
    if [ "$schm" = "https" ]; then
      port="443"
    else
      port="80"
    fi
  fi
  echo "Use proxy: "
  echo "- protocol: $schm"
  echo "- host    : $host"
  echo "- port    : $port"

  mkdir -p "$(dirname "$settingsFile")"

  cat > ~/.m2/settings.xml <<EOF
<settings>
  <proxies>
    <proxy>
      <id>proxy</id>
      <active>true</active>
      <protocol>${schm}</protocol>
      <host>${host}</host>
      <port>${port}</port>
    </proxy>
  </proxies>
</settings>
EOF
}

checkProxy() {
  for conf in "$https_proxy" "$HTTPS_PROXY" "$https_proxy" "$HTTPS_PROXY"; do
    # Pick first one non-null
    if [ ! -z "$conf" ]; then
      configureProxy "$conf"
      return 0
    fi
  done
}

checkProxy
exit $?
