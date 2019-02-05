#!/bin/bash

set -e

export XAP_VERSION=12.3.0
export XAP_HOME=/c/tools/gigaspaces-xap-12.3.0-ga-b19000

export localRepositoryPath=localRepo

mkdir -p ${localRepositoryPath}

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -DlocalRepositoryPath=${localRepositoryPath} \
-Dfile=${XAP_HOME}/lib/platform/service-grid/xap-admin.jar -Dpackaging=jar \
-DgroupId=com.gigaspaces -DartifactId=xap-admin -Dversion=${XAP_VERSION}

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -DlocalRepositoryPath=${localRepositoryPath} \
-Dfile=${XAP_HOME}/lib/required/xap-openspaces.jar -Dpackaging=jar \
-DgroupId=com.gigaspaces -DartifactId=xap-openspaces -Dversion=${XAP_VERSION}

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -DlocalRepositoryPath=${localRepositoryPath} \
-Dfile=${XAP_HOME}/lib/required/xap-datagrid.jar -Dpackaging=jar \
-DgroupId=com.gigaspaces -DartifactId=xap-datagrid -Dversion=${XAP_VERSION}

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -DlocalRepositoryPath=${localRepositoryPath} \
-Dfile=${XAP_HOME}/lib/required/xap-common.jar -Dpackaging=jar \
-DgroupId=com.gigaspaces -DartifactId=xap-common -Dversion=${XAP_VERSION}

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -DlocalRepositoryPath=${localRepositoryPath} \
-Dfile=${XAP_HOME}/lib/required/xap-trove.jar -Dpackaging=jar \
-DgroupId=com.gigaspaces -DartifactId=xap-trove -Dversion=${XAP_VERSION}

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -DlocalRepositoryPath=${localRepositoryPath} \
-Dfile=${XAP_HOME}/lib/required/xap-asm.jar -Dpackaging=jar \
-DgroupId=com.gigaspaces -DartifactId=xap-asm -Dversion=${XAP_VERSION}

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -DlocalRepositoryPath=${localRepositoryPath} \
-Dfile=${XAP_HOME}/lib/platform/service-grid/xap-service-grid.jar -Dpackaging=jar \
-DgroupId=com.gigaspaces -DartifactId=xap-service-grid -Dversion=${XAP_VERSION}

echo "Success"
