# Redstone Visualizer

Mod Fabric **côté client**, pour Minecraft Java **1.21.11** en solo. Il rend moins visibles les blocs d'un volume sélectionné, puis révèle temporairement ceux dont le `BlockState` change. La version 1.21.10 reste sur la branche `master`.

## Installation et lancement

- Installer Fabric Loader **0.19.5** pour Minecraft **1.21.11**, Fabric API **0.141.6+1.21.11** et MaLiLib **0.27.20**. Le JAR du mod embarque `conditional-mixin` 0.6.4, nécessaire à cette version de MaLiLib.
- Copier `build/libs/redstone-visualizer-1.21.11-0.1.0.jar` dans le dossier `mods` du profil Fabric 1.21.11. Ne pas y mettre aussi le JAR 1.21.10.
- Pour développer : JDK **21**, Gradle Wrapper **9.5.0**, Loom **1.17.21** ; exécuter `./gradlew build` puis `./gradlew runClient` (`.\gradlew.bat` sous Windows). Mappings Yarn : **1.21.11+build.6**. Le client de développement utilise `run-1.21.11/`, séparé des sauvegardes du prototype 1.21.10.

## Utilisation

Dans un monde solo, **V** ouvre l'écran du mod. Viser un bloc puis capturer les coins 1 et 2, ou saisir leurs coordonnées. Les deux coins sont inclusifs et doivent être dans la même dimension. Le bouton **OFF/ON** active l'effet ; **R** le bascule aussi. La zone et l'état sont enregistrés dans `redstone_visualizer.properties` à la racine de la sauvegarde. Une nouvelle sauvegarde démarre avec l'effet désactivé.

Le bouton **Settings** ouvre les réglages MaLiLib : opacité de base (0 % par défaut), durée active (12 ticks), fondu (4 ticks), liste blanche d'identifiants de blocs, seuil d'alerte (32 768 positions) et raccourcis. Les blocs hors zone gardent leur rendu habituel. L'effet est limité à la dimension de la zone.

## État de validation

`./gradlew build` passe, avec les tests autonomes de zone, temporisation, logique de liste blanche et sauvegarde. `./gradlew runClient` charge Minecraft 1.21.11, Fabric API, MaLiLib et le mod ; un monde solo s'ouvre et le terrain se rend sans erreur de mixin. L'éditeur de whitelist et les effets à 0 %/50 % n'ont pas encore été retestés visuellement sur 1.21.11. Le rendu des block entities à opacité intermédiaire, la couverture de chaque famille de blocs, les performances à 32 768 positions, Sodium et Iris restent hors de la validation du prototype. `PLAN.md` décrit la cible initiale 1.21.10 et ses critères complets.
