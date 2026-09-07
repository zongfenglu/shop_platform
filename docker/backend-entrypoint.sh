#!/bin/sh
set -e
# Named volumes mount as root; the JVM runs as shop and must be able to write uploads/backups.
mkdir -p /data/uploads /data/backups
chown shop:shop /data/uploads /data/backups 2>/dev/null || true
exec runuser -u shop -- java -XX:+UseContainerSupport -XX:MaxRAMPercentage=60 \
  -Duser.timezone=Asia/Shanghai ${JAVA_OPTS} -jar /app/app.jar
