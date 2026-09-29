# Redstone Visualizer

Mod Fabric **côté client**, pour Minecraft Java **1.21.11**, utilisable en solo ou sur un serveur. Il rend moins visibles les blocs d'un volume sélectionné, puis révèle temporairement ceux dont le `BlockState` change. La version 1.21.10 reste sur la branche `master`.

## Licence

Le code de Redstone Visualizer est distribué sous licence [MIT](LICENSE). Les dépendances et fichiers tiers conservent leurs licences respectives.

## Installation et lancement

- Installer Fabric Loader **0.19.5** pour Minecraft **1.21.11**, Fabric API **0.141.6+1.21.11** et MaLiLib **0.27.20**. Le JAR du mod embarque `conditional-mixin` 0.6.4, nécessaire à cette version de MaLiLib.
- Sodium est facultatif. L'adaptateur intégré cible exactement **Sodium 0.8.7+mc1.21.11** ; Sodium n'est ni embarqué ni requis par le JAR.
- Pour un meilleur rendu des blocs entités pris en charge, **[Better Block Entities](https://modrinth.com/mod/better-block-entities)** est recommandé : ce mod client améliore notamment leur effet de transparence et d'éclairage. Il est facultatif et nécessite Sodium.
- Copier `build/libs/redstone-visualizer-1.21.11-0.1.0.jar` dans le dossier `mods` du profil Fabric 1.21.11. Ne pas y mettre aussi le JAR 1.21.10.
- Pour développer : JDK **21**, Gradle Wrapper **9.5.0**, Loom **1.17.21** ; exécuter `./gradlew build` puis `./gradlew runClient` (`.\gradlew.bat` sous Windows). Mappings Yarn : **1.21.11+build.6**. Le client de développement utilise `run-1.21.11/`, séparé des sauvegardes du prototype 1.21.10.

## Utilisation

Dans un monde solo ou sur un serveur, **V** ouvre l'écran du mod. Viser un bloc puis capturer les coins 1 et 2, ou saisir leurs coordonnées. Les deux coins sont inclusifs et doivent être dans la même dimension. Le bouton **OFF/ON** active l'effet ; **R** le bascule aussi. Aucun mod ni paquet réseau spécifique n'est nécessaire côté serveur.

En solo, la zone et l'état sont enregistrés dans `redstone_visualizer.properties` à la racine de la sauvegarde. En multijoueur, ils restent sur le client dans `config/redstone_visualizer/servers/<empreinte>/redstone_visualizer.properties`, avec une empreinte SHA-256 de l'adresse afin de séparer les serveurs sans écrire leur adresse dans le nom du dossier. Un nouveau monde ou serveur démarre avec l'effet désactivé.

Le bouton **Settings** ouvre les réglages MaLiLib : opacité de base (0 % par défaut), durée active (12 ticks), fondu (4 ticks), liste blanche d'identifiants de blocs, seuil d'alerte (32 768 positions) et raccourcis. Les blocs hors zone gardent leur rendu habituel. L'effet est limité à la dimension de la zone.

Avec une autre version de Sodium, la visualisation reste désactivée et l'écran du mod affiche les versions détectée et validée. **Force for this session** active l'adaptateur uniquement jusqu'à la fermeture du client, avec l'état **FORCED — UNTESTED** ; ce mode peut être incomplet ou faire planter le client. Iris n'est pas pris en charge.

## État de validation

`./gradlew build` passe, avec les tests autonomes de zone, temporisation, logique de liste blanche, détection de la version Sodium et sauvegarde. `./gradlew runClient` ouvre un monde avec le renderer vanilla puis avec Sodium **0.8.7+mc1.21.11** sans erreur de mixin ; le JAR final n'embarque pas Sodium. L'utilisateur a validé le rendu des blocs vanilla hors block entities, la parité visuelle 1.21.10/1.21.11, les block entities à opacité intermédiaire (dont les surfaces End Portal et End Gateway), Sodium 0.8.7 et une vraie connexion multijoueur. Les mesures à 32 768 observateurs sont désormais disponibles ci-dessous, avec seulement 1 à 3 essais par configuration ; elles restent indicatives. Iris reste hors périmètre. `PLAN.md` décrit le contrat et les limites connues.

### Relevé de performance indicatif

Conditions : **Minecraft 1.21.11** en **1536 × 864**, sans limite FPS ni mod d'optimisation, sur un **Intel Core 5 120U sans GPU dédié**. La contraption est **Smallest Seamless 10x10 Cave Door**, dans un volume sélectionné de **7 408 blocs** avec une densité indiquée de **66 %**. La caméra est placée de façon à rendre toute la zone visible. Le temps de frame peut descendre vers 2 ms au repos ; le tableau retient uniquement les pics maximaux observés, pas les valeurs courantes, moyennes ou percentiles.

| État | Pic frame | Pic tick |
| --- | ---: | ---: |
| Porte inactive, mod désactivé | 15 ms | 4 ms |
| Porte inactive, mod activé | 15 ms | 4 ms |
| Porte activée, mod désactivé | 88 ms | 36 ms |
| Porte activée, mod activé | 66 ms | 38 ms |

Ce relevé est indicatif, pas un benchmark reproductible : les distances de rendu et de simulation, le nombre de répétitions et la méthode exacte de mesure n'ont pas été consignés. Dans ces conditions précises, les pics au repos sont identiques ; pendant l'activation, le pic frame passe de 88 à 66 ms et le pic tick de 36 à 38 ms. Ces valeurs ne se généralisent pas à d'autres configurations.

### Essai indicatif — 32 768 observateurs

Conditions : Minecraft **1.21.11**, Fabric API, MaLiLib et Redstone Visualizer ; **Intel Core 5 120U sans GPU dédié**, FPS non limités, VSync désactivée, distances de rendu/simulation **4/5 chunks**. Monde vide avec ticks aléatoires et apparition des créatures désactivés. Tous les **32³ observateurs** de la zone s'activent une fois ; environ **348 sont actifs simultanément en moyenne**. La séquence dure **9,4 s**. Chaque configuration a été mesurée **1 à 3 fois** ; les min/moy/max ci-dessous sont relevés sur la fenêtre de mesure de 10 s vers la fin de l'activation, pas agrégés entre essais.

| Contraption ON | Mod activé | Opacité | Temps par image min/moy/max (ms/frame) | Temps de tick min/moy/max (mspt) |
|---|---|---:|---:|---:|
| Non | Non | 100 % | 2/3/6 | 0/1/2 |
| Oui | Non | 100 % | 2/3/7 | 0/1/5 |
| Non | Oui | 0 % | 2/3/6 | 0/1/2 |
| Non | Oui | 50 % | 3/4/7 | 0/1/2 |
| Oui | Oui | 0 % | 3/6/18 | 0/1/6 |
| Oui | Oui | 50 % | 6/20/62 | 0/2/10 |

Cette série confirme un essai à 32 768 positions, mais le faible nombre de répétitions ne permet pas d'en tirer une performance générale. Une comparaison plus robuste demanderait une répétition automatisée de chaque configuration.
