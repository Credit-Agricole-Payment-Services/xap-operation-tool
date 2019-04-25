#!/bin/bash

set -e

umask 022

SCRIPTS_DIR=$(dirname $0)

OPERATION=restart-managers-one-by-one

${SCRIPTS_DIR}/xot.sh ${OPERATION} "$@"
