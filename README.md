# Redstone Visualizer

Mod Fabric **côté client**, pour Minecraft Java **1.21.11**, utilisable en solo ou sur un serveur. Il rend moins visibles les blocs d'un volume sélectionné, puis révèle temporairement ceux dont le `BlockState` change. La version 1.21.10 reste sur la branche `master`.

## Installation et lancement

- Installer Fabric Loader **0.19.5** pour Minecraft **1.21.11**, Fabric API **0.141.6+1.21.11** et MaLiLib **0.27.20**. Le JAR du mod embarque `conditional-mixin` 0.6.4, nécessaire à cette version de MaLiLib.
- Copier `build/libs/redstone-visualizer-1.21.11-0.1.0.jar` dans le dossier `mods` du profil Fabric 1.21.11. Ne pas y mettre aussi le JAR 1.21.10.
- Pour développer : JDK **21**, Gradle Wrapper **9.5.0**, Loom **1.17.21** ; exécuter `./gradlew build` puis `./gradlew runClient` (`.\gradlew.bat` sous Windows). Mappings Yarn : **1.21.11+build.6**. Le client de développement utilise `run-1.21.11/`, séparé des sauvegardes du prototype 1.21.10.

## Utilisation

Dans un monde solo ou sur un serveur, **V** ouvre l'écran du mod. Viser un bloc puis capturer les coins 1 et 2, ou saisir leurs coordonnées. Les deux coins sont inclusifs et doivent être dans la même dimension. Le bouton **OFF/ON** active l'effet ; **R** le bascule aussi. Aucun mod ni paquet réseau spécifique n'est nécessaire côté serveur.

En solo, la zone et l'état sont enregistrés dans `redstone_visualizer.properties` à la racine de la sauvegarde. En multijoueur, ils restent sur le client dans `config/redstone_visualizer/servers/<empreinte>/redstone_visualizer.properties`, avec une empreinte SHA-256 de l'adresse afin de séparer les serveurs sans écrire leur adresse dans le nom du dossier. Un nouveau monde ou serveur démarre avec l'effet désactivé.

Le bouton **Settings** ouvre les réglages MaLiLib : opacité de base (0 % par défaut), durée active (12 ticks), fondu (4 ticks), liste blanche d'identifiants de blocs, seuil d'alerte (32 768 positions) et raccourcis. Les blocs hors zone gardent leur rendu habituel. L'effet est limité à la dimension de la zone.

## État de validation

`./gradlew build` passe, avec les tests autonomes de zone, temporisation, logique de liste blanche et sauvegarde. `./gradlew runClient` charge Minecraft 1.21.11, Fabric API, MaLiLib et le mod ; un monde solo s'ouvre et le terrain se rend sans erreur de mixin. L'utilisateur a validé le rendu des blocs vanilla hors block entities ainsi que la parité visuelle entre 1.21.10 et 1.21.11. Le rendu des block entities à opacité intermédiaire, les performances à 32 768 positions, Sodium et Iris restent hors de la validation du prototype. Le support multijoueur est couvert par le build et les tests de persistance, mais demande encore un essai dans une vraie connexion serveur. `PLAN.md` décrit le contrat et ses critères complets.

### Relevé de performance indicatif

Conditions : **Minecraft 1.21.11** en **1536 × 864**, sans limite FPS ni mod d'optimisation, sur un **Intel Core 5 120U sans GPU dédié**. La contraption est **Smallest Seamless 10x10 Cave Door**, dans un volume sélectionné de **7 408 blocs** avec une densité indiquée de **66 %**. La caméra est placée de façon à rendre toute la zone visible. Le temps de frame peut descendre vers 2 ms au repos ; le tableau retient uniquement les pics maximaux observés, pas les valeurs courantes, moyennes ou percentiles.

| État | Pic frame | Pic tick |
| --- | ---: | ---: |
| Porte inactive, mod désactivé | 15 ms | 4 ms |
| Porte inactive, mod activé | 15 ms | 4 ms |
| Porte activée, mod désactivé | 88 ms | 36 ms |
| Porte activée, mod activé | 66 ms | 38 ms |

Ce relevé est indicatif, pas un benchmark reproductible : les distances de rendu et de simulation, le nombre de répétitions et la méthode exacte de mesure n'ont pas été consignés. Dans ces conditions précises, il montre néanmoins que l'impact observé du mod sur les performances est extrêmement faible : les pics au repos sont identiques ; pendant l'activation, le pic frame passe de 88 à 66 ms et le pic tick de 36 à 38 ms. Une seule capture de maxima sur 7 408 blocs ne valide pas encore le critère de 32 768 positions et ne permet pas de généraliser le résultat à d'autres configurations.
