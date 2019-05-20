#!/bin/bash

HOSTNAME=qlinap01

DIST_FILE=$(basename $(find ./xap-operation-tool-dist/target/ -name "*.tar.gz"))

scp ./xap-operation-tool-dist/target/${DIST_FILE} ${USERNAME}@${HOSTNAME}:/tmp

ssh ${USERNAME}@${HOSTNAME} << EOF
set -e
set -x
cd /tmp
tar xvfzp ${DIST_FILE}
DEST=$(find /tmp -maxdepth 1 -type d -name "xap-operation-tool-dist*")
cd $DEST
./install.sh
EOF

