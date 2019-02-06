#!/bin/bash

set -e

umask 022

SCRIPTS_DIR=$(dirname $0)

# the lib directory contains all of the JAR files that are needed
LIB_DIR=${SCRIPTS_DIR}/lib

# the config directory can be use to override some of the default resources present in the JAR files
# this allows the user to be able to edit configuration without having to rebuild
CLASSPATH=${SCRIPTS_DIR}/config

#CLASSPATH=${CLASSPATH}:${LIB_DIR}/*
#CLASSPATH=${CLASSPATH}:$(echo $LIB_DIR/*.jar | tr ' ' ':')
CLASSPATH=${CLASSPATH}:$(find "${LIB_DIR}" -name '*.jar' | tr '\n' ':')

OPERATION=deploy

time java -Xms1G -Xmx1G -cp "${CLASSPATH}" -jar $LIB_DIR/xap-operation-tool-main-1.1.0-SNAPSHOT.jar $OPERATION "$@"

echo "Script $0 finished successfully"
