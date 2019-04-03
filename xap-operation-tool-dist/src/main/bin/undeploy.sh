#!/bin/bash

set -e

umask 022

SCRIPTS_DIR=$(dirname $0)

OPERATION=undeploy

${SCRIPTS_DIR}/xot.sh ${OPERATION} "$@"
