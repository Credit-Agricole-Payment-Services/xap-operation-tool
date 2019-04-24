#!/bin/bash

set -e
set -x

mkdir -p /app/in/bin/${project.parent.artifactId}
chmod 755 /app/in/bin/${project.parent.artifactId}
cd /app/in/bin/${project.parent.artifactId}
rm -rf ${project.artifactId}-${project.version}-${git.commit.id.describe-short}
tar xvfzp /tmp/${project.artifactId}-${project.version}-${git.commit.id.describe-short}-dist.tar.gz
rm -f current && ln -s ${project.artifactId}-${project.version}-${git.commit.id.describe-short} current
