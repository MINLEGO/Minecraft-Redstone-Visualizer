# Redstone Visualizer

A client-side Fabric mod for Minecraft Java **1.21.11**, for singleplayer and multiplayer. It reduces the visibility of blocks in a selected volume, then temporarily reveals blocks whose `BlockState` changes. Version 1.21.10 remains on the `master` branch.

## License

Redstone Visualizer is distributed under the [MIT License](LICENSE). Third-party dependencies and files remain under their respective licenses.

## Installation and launch

- Install Fabric Loader **0.19.5** for Minecraft **1.21.11**, Fabric API **0.141.6+1.21.11**, and MaLiLib **0.27.20**. The mod JAR bundles `conditional-mixin` 0.6.4, which this MaLiLib version requires.
- Sodium is optional. The built-in adapter targets exactly **Sodium 0.8.7+mc1.21.11**; Sodium is neither bundled nor required by the JAR.
- For improved rendering of supported block entities, **[Better Block Entities](https://modrinth.com/mod/better-block-entities)** is recommended. This client-side mod improves their transparency and lighting effects. It is optional and requires Sodium.
- Copy `build/libs/redstone-visualizer-1.21.11-0.1.0.jar` into the `mods` folder of your Fabric 1.21.11 profile. Do not also put the 1.21.10 JAR there.
- For development: JDK **21**, Gradle Wrapper **9.5.0**, Loom **1.17.21**; run `./gradlew build` followed by `./gradlew runClient` (`.\gradlew.bat` on Windows). Yarn mappings: **1.21.11+build.6**. The development client uses `run-1.21.11/`, separate from the 1.21.10 prototype's saves.

## Usage

In a singleplayer world or on a server, press **V** to open the mod screen. Aim at a block and capture corners 1 and 2, or enter their coordinates. The corners are inclusive and must be in the same dimension. The **OFF/ON** button enables the effect; **R** also toggles it. No server-side mod or specific network packet is required.

In singleplayer, the zone and state are saved in `redstone_visualizer.properties` at the root of the save. In multiplayer, they stay on the client in `config/redstone_visualizer/servers/<fingerprint>/redstone_visualizer.properties`. The fingerprint is a SHA-256 hash of the address, separating servers without writing their addresses into folder names. New worlds and servers start with the effect disabled.

The **Settings** button opens MaLiLib settings: base opacity (0% by default), active duration (12 ticks), fade duration (4 ticks), a block ID whitelist, alert threshold (32,768 positions), and keybinds. Blocks outside the selected zone keep their usual rendering. The effect is limited to the zone's dimension.

With another Sodium version, the visualization stays disabled and the mod screen shows the detected and validated versions. **Force for this session** enables the adapter only until the client closes, with the status **FORCED — UNTESTED**; this mode may be incomplete or crash the client. Iris is not supported.

## Validation status

`./gradlew build` passes, including standalone tests for zone handling, timing, whitelist logic, Sodium version detection, and saving. `./gradlew runClient` opens a world with the vanilla renderer and with Sodium **0.8.7+mc1.21.11**, without mixin errors; the final JAR does not bundle Sodium. User testing has covered vanilla block rendering outside block entities, visual parity between 1.21.10 and 1.21.11, block entities at intermediate opacity (including End Portal and End Gateway surfaces), Sodium 0.8.7, and a real multiplayer connection. The 32,768-observer measurements below are available, with only 1 to 3 runs per configuration; they are indicative. Iris remains out of scope. `PLAN.md` describes the contract and known limitations.

### Indicative performance record

Conditions: **Minecraft 1.21.11** at **1536 × 864**, with no FPS cap or optimization mods, on an **Intel Core 5 120U with no dedicated GPU**. The contraption is **Smallest Seamless 10x10 Cave Door**, inside a selected volume of **7,408 blocks** with a stated density of **66%**. The camera is positioned so the entire area is visible. Frame time can drop to around 2 ms at idle; the table reports only the maximum observed peaks, not current, average, or percentile values.

| State | Frame peak | Tick peak |
| --- | ---: | ---: |
| Door inactive, mod disabled | 15 ms | 4 ms |
| Door inactive, mod enabled | 15 ms | 4 ms |
| Door active, mod disabled | 88 ms | 36 ms |
| Door active, mod enabled | 66 ms | 38 ms |

This record is indicative, not a reproducible benchmark: render and simulation distances, repetition count, and the exact measurement method were not recorded. Under these specific conditions, idle peaks are identical; during activation, the frame peak goes from 88 to 66 ms and the tick peak from 36 to 38 ms. These values do not generalize to other setups.

### Indicative test — 32,768 observers

Conditions: Minecraft **1.21.11**, Fabric API, MaLiLib, and Redstone Visualizer; **Intel Core 5 120U with no dedicated GPU**, uncapped FPS, VSync disabled, render/simulation distances of **4/5 chunks**. Random ticks and mob spawning are disabled in an empty world. All **32³ observers** in the selected zone activate once; about **348 are active simultaneously on average**. The sequence lasts **9.4 s**. Each configuration was measured **1 to 3 times**; the min/avg/max values below come from the 10-second measurement window near the end of activation and are not aggregated across runs.

| Contraption ON | Mod enabled | Opacity | Frame time min/avg/max (ms/frame) | Tick time min/avg/max (mspt) |
|---|---|---:|---:|---:|
| No | No | 100% | 2/3/6 | 0/1/2 |
| Yes | No | 100% | 2/3/7 | 0/1/5 |
| No | Yes | 0% | 2/3/6 | 0/1/2 |
| No | Yes | 50% | 3/4/7 | 0/1/2 |
| Yes | Yes | 0% | 3/6/18 | 0/1/6 |
| Yes | Yes | 50% | 6/20/62 | 0/2/10 |

This series confirms a test at 32,768 positions, but the small number of runs does not support general performance conclusions. A more robust comparison would require automated repetition of each configuration.
