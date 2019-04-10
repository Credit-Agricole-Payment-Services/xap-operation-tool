# xap-operation-tool : version ${project.version}-${git.commit.id.describe-short}

## Installation : 

Le livrable est un fichier au format ```.tar.gz```` :
```
${project.artifactId}-${project.version}-${git.commit.id.describe-short}
```

Le répertoire conseillé pour l'installation est ```/app/in/bin/${project.parent.artifactId}```

Un lien symbolique pointe vers la version en cours d'utilisation.

```
umask 022
mkdir -p /app/in/bin/${project.parent.artifactId}
cd /app/in/bin/${project.parent.artifactId}
tar xvfzp /tmp/${project.artifactId}-${project.version}-${git.commit.id.describe-short}-dist.tar.gz
ln -sf ${project.artifactId}-${project.version}-${git.commit.id.describe-short} current 
```

## Usage :

```
/app/in/bin/xap-operation-tool/deploy.sh --restartEmptyContainers -c /app/in/exploit/gsxap -f /app/in/livraisons/current/b1134/capsin/xap/in-xap-app-1.2.0-SNAPSHOT-xa.zip


/app/in/bin/xap-operation-tool/deploy.sh --restartEmptyContainers -c /app/in/exploit/gsxap -f /app/in/livraisons/

/app/in/bin/xap-operation-tool/deploy.sh --restartEmptyContainers -c /app/in/etc/sctinst/config/xap/deploy -f /app/in/livraisons/v1.2.0/in-delivery-1.2.0-20190326.171623-19-all.tar.gz:capsin/xap/in-xap-app-1.2.0-SNAPSHOT-xa.zip

```

options :
```
-c : chemin vers le répertoire qui contient les descripteurs de déploiement JSON (nom de la pu suivi de ".json")
-f : chemin vers le fichier ZIP (ou le répertoire) qui oontient les JAR des PU, ainsi que le fichier appplication.xml
```
