# PWTrigger

Système d'alarme basé sur PhaseWorm/FDSNWS.

## Fonctionnement général

Le principe général de ce logiciel est d'interroger un serveur FDSNWS poue en extraire des évènements sismiques et déclencher le cas échéant un message d'alerte.
Le message d'alerte est envoyé au [système Alarme Précoce](https://github.com/IPGP/AlarmePrecoce).

Tel qu'il est déployé à l'Observatoire Volcanologique du Piton de La Fournaise, les évènements sismiques sont détectés par les algorithmes PhaseNet/PhaseWorm. Néanmoins, ce logiciel fonctionnera avec n'importe quel système proposant le Web Service FDSNWS.

PWTrigger va récupérer tous les évènements sismiques de magnitude supérieure à celle définie dans son fichier de configuration sur la fenêtre de temps définie aussi en configuration.
Si le nombre de séismes récupérés dépasse le seuil défini dans le fichier de configuration, alors une alarme est déclenchée.

## Installation

Pour fonctionner, l'application a besoin de :
* L'environnement de développement Java (https://www.java.com) et l'outil Apache Ant (https://ant.apache.org)

Par exemple sous Debian
```sh
apt-get install ant default-jdk
```
Une fois le projet cloné, la compilation/installation se fait avec la commande
```sh
ant build
```
Il est possible de l'installer manuellement en créant un dossier contenant le JAR et le dossier resources. Il faut aussi copier le script de lancement dans le dossier de base, au même niveau que le fichier Jar. Le dossier contiendra :

```sh
|-- pwtrigger.sh
`-- dist
    |-- PWTrigger.jar
    |-- lib
    `-- resources
        |-- log4j2.xml
        `-- pwtrigger.xml
```

### Création d'un fichier son pour l'alarme

Spéficier le nom du fichier son qui sera joué par Alarme Précoce.

Se référer ensuite à la documentation d'[Alarme Précoce](https://github.com/IPGP/AlarmePrecoce) pour la création du fichier audio.

### Lancement de l'applcation

Un script est proposé pour le lancement de l'application :

```sh
sh pwtrigger.sh
```
Penser à modifier les chemins d'accès.

Toute modification de la configuration ne nécessite pas un redémarrage de l'application.


### Configuration du démarrage automatique via systemd

Une fois l’application installée dans un dossier (à savoir l’archive jar PWTrigger.jar, le dossier resources ainsi que le fichier de configuration dûment rempli), on peut l’exécuter comme un service (daemon) grâce à systemd (sous Debian).

Celui-ci peut être trouvé dans le dossier resources et doit être configuré puis copié dans /etc/systemd/system/ afin qu’il soit automatiquement exécuté au démarrage.

Pour configurer le fichier, vous aurez besoin

* Du nom d’utilisateur qui exécutera l’application (par défaut sysop)
* De l’emplacement d’installation du script de démarrage de l’application

```sh
[Unit]
Description=PWTrigger
After=syslog.target
After=network.target

[Service]
Type=simple
User=sysop
Group=sysop
WorkingDirectory=/opt/PWTrigger
ExecStart=/bin/bash /opt/PWTrigger/pwtrigger.sh
Restart=on-failure
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=/var/log/pwtrigger.log
Environment=USER=sysop HOME=/home/sysop
#StandardOutput=tty

[Install]
WantedBy=multi-user.target
```

Activer ensuite le service :

```sh
systemctl daemon-reload
systemctl enable pwtrigger.service
```

Le service sera démarré automatiquement au prochain démarrage. Pour démarrer manuellement le service, on peut utiliser :

```sh
systemctl start pwtrigger.service
```

De même avec stop, restart ou status.

Pour accéder aux journaux de l’application, utiliser la commande :

```sh
journalctl -u pwtrigger.service
```
## Configuration

Pour configurer le logiciel suivre la [procédure détaillée](CONFIGURATION.md).
