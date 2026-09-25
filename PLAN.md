# Plan de développement — Redstone Visualizer

Statut : prototype implémenté ; rendu vanilla hors block entities et parité 1.21.10/1.21.11 validés par l'utilisateur.
Cible : Minecraft Java **1.21.10 et 1.21.11**, **Fabric**, mod installé uniquement côté client, usage **solo ou multijoueur**.

## 1. Résultat attendu

Le joueur sélectionne un volume entre deux blocs. Dans ce volume, les blocs qui n'ont pas changé récemment de `BlockState` prennent une opacité réglable. Chaque changement d'état rend le bloc entièrement visible pendant une durée réglable, puis son apparence revient progressivement à l'opacité de base. Une liste de types de blocs reste toujours visible. Le monde, ses collisions, sa lumière et sa redstone ne sont pas modifiés.

### Contrat de comportement

| Sujet | Règle retenue |
| --- | --- |
| Zone | Deux coins inclusifs, capturés depuis le bloc visé dans le menu, avec coordonnées entières éditables. Les deux coins doivent appartenir à la même dimension. |
| Sauvegarde | Une zone par monde solo ou serveur, liée à la dimension où elle a été définie. Une nouvelle sélection dans une autre dimension remplace l'ancienne. L'effet est inactif dans les autres dimensions. En multijoueur, les données restent locales au client et sont séparées par une empreinte de l'adresse du serveur. |
| Activation | OFF lors de la première utilisation. Le dernier état ON/OFF est ensuite enregistré par monde ou serveur. Sans deux coins valides, l'effet reste inactif. Menu et raccourci configurable pour basculer l'effet. |
| Opacité de base | Réglage de 0 à 100 %, à **0 %** initialement, appliqué à tous les blocs de la zone hors whitelist qui ne sont pas récemment actifs. Hors zone, rendu Minecraft habituel. |
| Update | Changement réel de `BlockState` à une position déjà chargée, y compris le remplacement par un autre bloc. Une notification de voisin, un tick planifié, une modification d'inventaire ou l'ouverture d'un coffre sans changement de `BlockState` ne comptent pas. |
| Durée | **12 ticks de jeu** initialement. Le bloc est pleinement visible pendant les 8 premiers, puis revient à l'opacité de base sur les **4 derniers ticks**. Les deux durées sont configurables ; le fondu peut être désactivé. Un nouveau changement relance la durée. Le compteur suit le serveur intégré en solo et les ticks client en multijoueur. |
| Whitelist | Liste modifiable d'identifiants de blocs, vide initialement. Par exemple `minecraft:redstone_lamp` rend ce type toujours visible, quel que soit son état. Pas de tags dans la première version. |
| Chargement | L'arrivée dans un monde et le chargement/rechargement d'un chunk ne créent pas de fausses updates. L'historique temporaire est vidé au changement de monde, de zone et à la réactivation de l'effet. |
| Destruction | Si un changement produit de l'air, aucun fantôme de l'ancien bloc n'est dessiné. |
| Couverture | Tous les blocs vanilla 1.21.10 et 1.21.11, dont modèles solides/découpés/translucides, fluides et blocs avec rendu de block entity. Les faces internes d'un bloc récent ou en whitelist doivent être visibles. Blocs ajoutés par d'autres mods : prise en charge au mieux. |
| Frontière | Seuls les blocs dont la position est dans la zone changent de rendu. Un mur hors zone reste normal. Entités, particules et objets tenus ne font pas partie du masque. |
| Taille | Alerte au-delà de **32 768 positions** initialement, seuil configurable, sans plafond bloquant. |
| Compatibilité | Minecraft/Fabric sans shader est la cible obligatoire initiale. Le serveur n'a pas besoin du mod et aucun paquet spécifique n'est envoyé. Sodium et Iris sont évalués séparément après la version fonctionnelle ; aucune compatibilité implicite n'est annoncée. |

À l'état ON mais sans zone valide, l'interface indique le ou les coins manquants et le monde conserve son rendu normal. À l'état OFF, le rendu et les interactions sont ceux de Minecraft, même si la zone et les réglages restent enregistrés.

## 2. Choix techniques et points à vérifier

1. **Socle.** Partir d'un projet Fabric minimal pour 1.21.10 avec Java 21, Fabric Loom, les mappings et dépendances compatibles. MaLiLib fournit les réglages, leur écran et les raccourcis quand ses API 1.21.10 le permettent. Ne pas ajouter de seconde bibliothèque de configuration. Épingler les versions utilisées dans Gradle et `fabric.mod.json` après vérification de la résolution réelle.
2. **État par monde ou serveur.** Garder les préférences générales dans MaLiLib et une petite donnée locale par monde ou serveur pour la zone, sa dimension et l'état ON/OFF. Une sauvegarde solo utilise son dossier ; un serveur utilise une empreinte locale de son adresse. Ne jamais modifier les données de gameplay ni envoyer ces réglages au serveur. Valider les identifiants de blocs, les bornes des réglages et les coordonnées à la lecture.
3. **Détection.** Trouver le chemin commun où le client applique un changement de `BlockState`, comparer ancien et nouvel état et enregistrer une seule date par position. Vérifier les paquets de mise à jour individuels et groupés, le chargement initial des chunks, les changements de dimension et les remplacements de blocs. Employer un hook ciblé seulement si Fabric/MaLiLib n'exposent pas d'événement adapté.
4. **Horloge.** Utiliser les ticks du serveur intégré en solo, avec pause respectée, et les ticks client en multijoueur. Vérifier en jeu les frontières de thread entre monde client et rendu. Le temps réel ou le nombre d'images ne doivent pas raccourcir ou rallonger les 12 ticks.
5. **Rendu.** Un simple overlay ne peut pas rendre transparent un bloc opaque déjà dessiné par Minecraft. Il faudra contrôler le dessin normal des blocs de la zone et les redessiner avec l'alpha voulu. Le point d'injection précis reste à choisir après un prototype mesuré. Les fluides et les block entities utilisent des chemins distincts : les traiter explicitement, sans supposer que le traitement du terrain les couvre.

Règle d'alpha pour un bloc hors whitelist, avec `a` l'opacité de base, `X` la durée totale et `F` la durée du fondu : après une update, alpha vaut 100 % jusqu'à `X - F`, décroît de 100 % vers `a` pendant `F`, puis reste à `a`. Si `F = 0`, le retour est immédiat après `X`. Le fondu visuel peut interpoler entre deux ticks, mais le délai est compté en ticks de jeu. Les blocs en whitelist restent à 100 %. Une nouvelle update remplace la date précédente.

## 3. Étapes d'implémentation et critères de sortie

### Étape A — Socle exécutable

- Créer le projet Fabric 1.21.10 le plus réduit possible, lancer le client de développement et produire un JAR installable.
- Brancher MaLiLib et vérifier en jeu que son menu, une option persistante et un raccourci fonctionnent.
- Documenter les versions exactes du JDK, de Fabric Loader, de Fabric API si utilisée, de Loom et de MaLiLib.

**Sortie :** le mod se charge en solo sans effet de rendu, les réglages sont accessibles, le build passe.

### Étape B — Prototype de rendu, avant le reste de l'interface

- Sur une petite zone codée pour l'essai, supprimer ou remplacer le rendu normal des blocs choisis ; vérifier 0 %, 50 % et 100 % d'opacité.
- Dessiner un bloc actif avec son modèle complet, y compris ses faces normalement cachées par ses voisins. Vérifier profondeur, occlusion par les blocs hors zone, lumière et tri des faces translucides.
- Tester au minimum pierre, poussière redstone, lampe, verre, eau et coffre. Mesurer le coût d'une zone de 32 768 positions.
- Choisir ensuite le point d'intégration définitif : filtrage du terrain au niveau des sections/chunks et passage de rendu contrôlé, ou une autre solution prouvée par le prototype. Éviter une reconstruction complète du volume à chaque image ou chaque tick si le résultat mesuré ne tient pas.

**Sortie :** démonstration en jeu de la transparence réelle, pas une superposition visuelle. Si l'alpha des blocs vanilla ou les faces internes ne sont pas réalisables proprement avec ce chemin, revoir l'architecture avant de développer l'interface ; ne pas déclarer la fonctionnalité terminée.

### Étape C — Updates et temporisation

- Observer uniquement les transitions réelles de `BlockState`, enregistrer `position → dernier tick`, relancer le délai lors d'une nouvelle transition et purger les positions expirées.
- Exclure la réception initiale des chunks et vider l'historique dans les cas définis au contrat.
- Appliquer les 12 ticks et le fondu de 4 ticks ; vérifier qu'une pause fige le compte et que des ralentissements FPS ne le changent pas.
- Vérifier au moins poussière, répéteur, torche, observateur, piston et lampe. Vérifier qu'ouvrir un coffre seul ou modifier son inventaire ne déclenche rien.

**Sortie :** chronologie correcte à la position du bloc, sans faux positifs de chargement.

### Étape D — Tous les rendus vanilla

- Couvrir les modèles solides, découpés et translucides, puis les fluides et les block entities. Conserver le rendu complet des blocs actifs et en whitelist.
- Traiter la mise à jour du rendu lorsque changent zone, opacité, whitelist, état ON/OFF ou état du bloc. Mettre en cache par section ce qui peut l'être ; ne recalculer que les parties affectées.
- Tester une représentation de chaque famille de rendu vanilla, puis exécuter un parcours automatique des types de blocs vanilla pour repérer les erreurs ou crashs. Une inspection visuelle reste nécessaire pour la transparence réelle.

**Sortie :** couverture vanilla conforme au contrat à 0 %, 50 % et 100 %, sans modifier la mécanique du monde.

### Étape E — Interface et sauvegarde

- Écran MaLiLib : activer/désactiver, enregistrer coin 1/coin 2 depuis le bloc visé, éditer les coordonnées, voir dimension et taille de la zone, régler opacité, durée, fondu, whitelist, seuil d'alerte et raccourci.
- Montrer les erreurs utiles : coin manquant, coins de dimensions différentes, identifiant de bloc invalide, fondu supérieur à la durée, zone dépassant le seuil d'alerte. L'alerte de taille n'empêche pas l'activation.
- Enregistrer une seule zone liée à sa dimension et le dernier état ON/OFF par monde ou serveur. Confirmer qu'une nouvelle installation démarre OFF et que la sauvegarde ne transforme pas un chargement en update.

**Sortie :** l'ensemble du scénario peut être réalisé dans le jeu sans édition manuelle de fichiers.

### Étape F — Vérification finale et livraison

- Exécuter les tests ciblés de la logique de zone, de whitelist, de temporisation/fondu et de lecture/écriture de configuration. Garder peu de tests mais couvrir les erreurs qui changeraient le comportement visible.
- En jeu, couvrir : première activation, coin manquant, autre dimension, recharge du monde/chunk, ON/OFF persistant, réactivation, pause, update répétée, destruction, liste blanche, zone au-delà du seuil, connexion et reconnexion à deux serveurs distincts.
- Comparer sur la même machine les temps de frame, pics de reconstruction de chunks et mémoire avec effet OFF/ON, sur une petite zone puis à 32 768 positions. Noter les résultats et corriger tout gel ou coût qui croît sans borne.
- Tester les blocs moddés disponibles au mieux. Tester Sodium puis Iris séparément et consigner ce qui fonctionne, échoue ou reste non validé ; ne pas dégrader silencieusement la cible Fabric sans shader.
- Produire le JAR, un README court (installation, MaLiLib requis, usage, réglages, limites vérifiées) et les résultats de validation.

**Sortie :** build reproductible, comportement contractuel vérifié en jeu et limites annoncées à partir de tests réels.

## 4. Ordre de priorité et risques

Le **prototype de rendu est le premier jalon risqué**. Minecraft dessine le terrain, les fluides et les block entities par des voies différentes. L'opacité à 50 % et la visibilité des faces internes nécessitent plus qu'un filtre d'updates. La validation sur pierre + redstone + eau + coffre évite de construire une interface complète autour d'un rendu qui ne satisferait pas le contrat.

Le second risque est la performance : à 0 % de base, peu de blocs peuvent nécessiter un dessin supplémentaire ; à 50 %, tout le volume peut devenir translucide. Mesurer et adapter le cache/rendu avant de considérer « tous les blocs » comme terminé.

Le troisième risque est la compatibilité avec les autres moteurs de rendu. Sodium et Iris restent hors du critère de livraison initial tant qu'ils n'ont pas été testés sur leurs versions exactes. Le multijoueur reste entièrement côté client et n'ajoute aucune logique réseau propre au mod.

## 5. Références de départ

- [Fabric : structure d'un projet et entrée client](https://docs.fabricmc.net/develop/getting-started/project-structure)
- [Fabric : rendu dans le monde pour 1.21.10](https://github.com/FabricMC/fabric-docs/blob/main/versions/1.21.10/develop/rendering/world.md)
- [Fabric : rendu des block entities pour 1.21.10](https://github.com/FabricMC/fabric-docs/blob/main/versions/1.21.10/develop/blocks/block-entity-renderer.md)
- [MaLiLib : version Fabric compatible 1.21.10](https://modrinth.com/mod/malilib/version/0.26.8)
- [Fabric Wiki : Java requis pour les versions depuis 1.20.5](https://wiki.fabricmc.net/tutorial:setup)

Les API et versions précises sont épinglées séparément dans les branches 1.21.10 et 1.21.11. Ce document distingue les validations utilisateur des chemins qui restent à tester en jeu.
