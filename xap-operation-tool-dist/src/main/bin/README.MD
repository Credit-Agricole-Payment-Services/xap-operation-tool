Usage
```
/app/in/bin/xap-operation-tool/deploy.sh --restartEmptyContainers -c /app/in/exploit/gsxap -f /app/in/livraisons/current/b1134/capsin/xap/in-xap-app-1.2.0-SNAPSHOT-xa.zip


/app/in/bin/xap-operation-tool/deploy.sh --restartEmptyContainers -c /app/in/etc/sctinst/config/xap/deploy -f /app/in/livraisons/v1.2.0/in-delivery-1.2.0-20190326.171623-19-all.tar.gz:capsin/xap/in-xap-app-1.2.0-SNAPSHOT-xa.zip

```

options :
```
-c : chemin vers le répertoire qui contient les descripteurs de déploiement JSON (nom de la pu suivi de ".json")
-f : chemin vers le fichier ZIP (ou le répertoire) qui oontient les JAR des PU, ainsi que le fichier appplication.xml
```
