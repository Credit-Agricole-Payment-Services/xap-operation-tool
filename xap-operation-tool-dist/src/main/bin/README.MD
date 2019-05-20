# xap-operation-tool : version ${project.version}-${git.commit.id.describe-short}

## Installation : 

Le livrable est un fichier au format ```.tar.gz```` :
```
${project.artifactId}-${project.version}-${git.commit.id.describe-short}-dist.tar.gz
```

Le répertoire conseillé pour l'installation est ```/app/in/bin/${project.parent.artifactId}```

Il est conseillé de :
- installer chaque nouvelle version dans un répertoire spécifique (le nom du répertoire contient le numéro de version). Faire le ménage quand même de temps en temps pour éviter de conserver des versions obsolètes.
- créér un lien symbolique vers le répertoire contenant la version désirée.
- supprimer le répertoire précédent s'il existe déjà, en particulier en cas d'installation d'une version SNAPSHOT.

Généralement, une installation consiste à effectuer les opérations suivantes :

```
mkdir -p /app/in/bin/${project.parent.artifactId}
chmod 755 /app/in/bin/${project.parent.artifactId}
cd /app/in/bin/${project.parent.artifactId}
rm -rf ${project.artifactId}-${project.version}-${git.commit.id.describe-short}
tar xvfzp /tmp/${project.artifactId}-${project.version}-${git.commit.id.describe-short}-dist.tar.gz
rm -f current && ln -s ${project.artifactId}-${project.version}-${git.commit.id.describe-short} current 
```

*ATTENTION* : à bien préserver le filemode (option ```p```) lors de l'extraction du ````tar.gz```` pour que les fichiers ai les droits attendus.

## Usage :

### Deploy.sh

options :
```
--restartEmptyContainers : les GSC, qui au départ ne contiennent aucune PU, sont d'abord redemarrée
-c : chemin vers le répertoire qui contient les descripteurs de déploiement JSON (nom de la pu suivi de ".json")
-f : chemin vers le fichier ZIP (ou le répertoire) qui oontient les JAR des PU, ainsi que le fichier appplication.xml
--puIncludes : liste des noms (séparées par une virgule) des PU à inclure. Permet de ne deployer qu'une sous partie des PU disponibles dans le application.xml
--puExcludes : liste des noms (séparées par une virgule) des PU à exclure. Permet de ne deployer qu'une sous partie des PU disponibles dans le application.xml
--whole : permet de deployer l'ensemble des PU en tant d'une application entière. Envoi toutes les PU d'un coup au manager. Le deploiement sera plus rapide. Mais il n'est ensuite plus possible de faire de déploiement PU par PU, il faut faire un undeploy de toute l'application pour pouvoir redeployer. A n'utiliser qu'en developpement.
```

Example d'usages : 

* Les JARs des PUs et le fichier XML ne sont pas contenus dans une archive
```
/app/in/bin/xap-operation-tool/deploy.sh --restartEmptyContainers -c /app/in/exploit/gsxap -f /app/in/livraisons/
```

* Les JARs des PUs et le fichier ```application.xml``` sont contenus dans une archive ```.zip```
```
/app/in/bin/xap-operation-tool/deploy.sh --restartEmptyContainers -c /app/in/exploit/gsxap -f /app/in/livraisons/current/b1134/capsin/xap/in-xap-app-1.2.0-SNAPSHOT-xa.zip
```

* Les JARs des PUs et le fichier ```application.xml``` sont contenus dans une archive ```.zip```, et cette archive est elle-même contenue dans une archive ```.tar.gz```
```
/app/in/bin/xap-operation-tool/deploy.sh --restartEmptyContainers -c /app/in/etc/sctinst/config/xap/deploy -f /app/in/livraisons/v1.2.0/in-delivery-1.2.0-20190326.171623-19-all.tar.gz:capsin/xap/in-xap-app-1.2.0-SNAPSHOT-xa.zip
```

