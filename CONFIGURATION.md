# Configuration de PWTrigger

## Configuration de la communication avec Alarme Précoce

Cette section définit les paramètres de l'Alarme Précoce.

* host : le nom d'hôte ou l'adresse IP de l'Alarme Précoce.
* port : le port UDP de l'Alarme Précoce.
* create_triggers : indique si PWTrigger peut déclencher une alarme ou non.
* priority : entier qui indique le niveau de priorité de l'alarme (1 est le niveau plus élevé).
* confirm_code : entier qui indique le code de confirmation à saisir par l'opérateur qui reçoit l'alarme.
* call_list : le nom de la liste d'appel à utiliser (définie côté Alarme Précoce)
* warning_message : le message audio à utiliser par Alarme Précoce.
* repeat : booléen (true ou false) définissant si la liste d'appel est répétée ou non.
* event_log_dir : le dossier où seront écris les logs (journaux) de PWTrigger.


```sh
  <triggers>
    <host>192.168.1.1</host>
    <port>4445</port>
    <create_triggers>true</create_triggers>
    <priority>1</priority>
    <confirm_code>11</confirm_code>
    <call_list>default</call_list>
    <warning_message>sismicite-revosima</warning_message>
    <repeat>true</repeat>
    <event_log_dir>/opt/PWTrigger/log</event_log_dir>
  </triggers>
```

## Configuration des paramètres de déclenchement d'alarme

Cette section définit plusieurs paramètres pour les déclenchements.

* time_window : indique la fenêtre temporelle exprimée en minutes considérée à partir de la date courante. Par exemple, la valeur 1440 indiquera qu'on a défini une fenêtre temporelle sur les 24 dernières heures.
* event_number : indique le nombre d'évènements dans la fenêtre temporelle qui vont déclencher une alarme.
* event_magnitude_min : indique la magnitude minimale nécessaire pour prendre en compte un évènement sismique.

```sh
  <alarm>
    <time_window>1440</time_window>
    <event_number>1</event_number>
    <event_magnitude_min>0.5</event_magnitude_min>
  </alarm>
```

## Configuration du serveur FDSNWS

La section network ne comprend qu’un paramètre :

* url. Correspond à l'URL de base du service FDSNWS. Exemple pour l'IRIS : https://service.iris.edu/fdsnws/event/1/

```sh
  <fdsnws>
    <url>http://192.168.1.2:8080/fdsnws/event/1/</url>
  </fdsnws>
```
