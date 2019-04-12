#!/bin/bash

set -e

umask 022

SCRIPTS_DIR=$(dirname $0)

# the lib directory contains all of the JAR files that are needed
LIB_DIR=${SCRIPTS_DIR}/lib

# check that every jar files is readable by the current user, we want to failfast if this is not the case
# in case files can be read, we don't want any output (stdout is redirected to /dev/null)
# in case files cannot be read, we want an output
# find ${LIB_DIR} -name "*.jar" | xargs wc -c > /dev/null

# the config directory can be use to override some of the default resources present in the JAR files
# this allows the user to be able to edit configuration without having to rebuild
CLASSPATH=${SCRIPTS_DIR}/config

#CLASSPATH=${CLASSPATH}:${LIB_DIR}/*
#CLASSPATH=${CLASSPATH}:$(echo $LIB_DIR/*.jar | tr ' ' ':')
CLASSPATH=${CLASSPATH}:$(find "${LIB_DIR}" -name '*.jar' | tr '\n' ':')

OPERATION=$1

time java -Xms500M -Xmx500M -Dcom.gs.logging.disabled=true -cp "${CLASSPATH}" -jar $LIB_DIR/xap-operation-tool-main-${project.version}.jar "$@"

echo "Script $0 ${OPERATION} finished successfully"
