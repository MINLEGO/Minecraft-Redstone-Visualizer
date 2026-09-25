# Redstone Visualizer

Mod Fabric **côté client**, pour Minecraft Java **1.21.10** en solo. Il rend moins visibles les blocs d'un volume sélectionné, puis révèle temporairement ceux dont le `BlockState` change.

## Installation et lancement

- Installer Fabric Loader **0.19.5** pour Minecraft **1.21.10**, Fabric API **0.138.4+1.21.10** et MaLiLib **0.26.8**.
- Copier `build/libs/redstone-visualizer-0.1.0.jar` dans le dossier `mods` du profil Fabric.
- Pour développer : JDK **21**, Gradle Wrapper **9.5.0**, Loom **1.17.21** ; exécuter `./gradlew build` puis `./gradlew runClient` (`.\gradlew.bat` sous Windows). Mappings Yarn : **1.21.10+build.2**.

## Utilisation

Dans un monde solo, **V** ouvre l'écran du mod. Viser un bloc puis capturer les coins 1 et 2, ou saisir leurs coordonnées. Les deux coins sont inclusifs et doivent être dans la même dimension. Le bouton **OFF/ON** active l'effet ; **R** le bascule aussi. La zone et l'état sont enregistrés dans `redstone_visualizer.properties` à la racine de la sauvegarde. Une nouvelle sauvegarde démarre avec l'effet désactivé.

Le bouton **Settings** ouvre les réglages MaLiLib : opacité de base (0 % par défaut), durée active (12 ticks), fondu (4 ticks), liste blanche d'identifiants de blocs, seuil d'alerte (32 768 positions) et raccourcis. Les blocs hors zone gardent leur rendu habituel. L'effet est limité à la dimension de la zone.

## État de validation

`./gradlew build` passe, y compris les tests ciblés de la logique de zone, de temporisation, de liste blanche et de sauvegarde. Un essai en monde solo a confirmé qu'une zone à 0 % masque les blocs, que les blocs d'une horloge redstone réapparaissent brièvement lors de leurs changements d'état, que leurs faces contre les blocs masqués sont visibles et que les blocs inactifs deviennent semi-transparents à 50 %. La transparence de chaque famille de blocs n'a pas été contrôlée visuellement en jeu. Le rendu des block entities est masqué à 0 %, mais leur opacité intermédiaire (par exemple un coffre à 50 %) reste à implémenter et valider. Les performances à 32 768 positions, Sodium et Iris ne sont pas validés. Ce JAR est donc un **prototype**, pas encore la livraison conforme à tous les critères de `PLAN.md`.
