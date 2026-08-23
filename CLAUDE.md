# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Fabric-Client-Mod, der Minecraft an das IntelliJ-Plugin FIM anbindet. Fork von `FishIntegratedMinecraft/FIM-Fabric`.

## Build & Run

```bash
./gradlew :26.2-fabric:build
```

Tasks existieren pro Version (`1.21.4-fabric`, `1.21.5-fabric`, `1.21.6-fabric`, `26.2-fabric`):
`:<version>:build`, `:<version>:runClient`, `:<version>:runDatagen`. Alle Versionen teilen sich `run/`
als Spielverzeichnis. `build` hängt `copyJar` an, die fertige Jar landet in `jars/`.

Ein System-Gradle taugt hier nicht (Arch-Paket 9.7 bricht mit
`Cannot find module 'gradle-public-api-legacy'` ab) — immer den Wrapper benutzen. Alternativ die
Run-Configs in `.idea/runConfigurations/`.

Es gibt keine Tests.

## Multi-Version-Layout (ReplayMod-Preprocessor)

- `versions/mainProject` legt die Kernversion fest — aktuell `1.21.6-fabric`. Deren Quellcode liegt im
  **Root-`src/`**, nicht unter `versions/1.21.6-fabric/`.
- `versions/<version>/src/` enthält nur **Overrides**, die den preprozessierten Code ersetzen. Alles andere
  wird beim Build nach `versions/<version>/build/preprocessed/` generiert.
- Gemeinsame Änderungen gehören immer in `src/`. Eine Änderung nur in `versions/<version>/src/` gilt
  ausschließlich für diese MC-Version und wird von keiner anderen übernommen.
- Versionsweichen im Root-`src/` per Preprocessor-Direktiven, MC-Version als 5-/6-stellige Zahl
  (`12106` = 1.21.6, `260200` = 26.2):
  ```java
  //#if MC>=260200
  //$$ neuerCode();
  //#else
  alterCode();
  //#endif
  ```
- Kernversion wechseln: `:<version>:setCoreVersion` (schreibt die Quellen um). Die aktuelle Kernversion
  hat diesen Task nicht.
- `versions/mapping-1.21.6-26.2.txt` überbrückt Yarn-Namen (1.21.6) auf Mojang-Namen (26.2), wo die
  automatische Ableitung nicht reicht. Neue Umbenennungen dort ergänzen.

## Versions-Besonderheiten

- **< 26**: obfuskiert → Yarn-Mappings, Plugin `fabric-loom-remap`, Java 21, Ship-Jar aus `remapJar`.
- **26.2**: unobfuskiert → keine Mappings, Plugin `fabric-loom`, **Java 25**, Ship-Jar aus `jar`.
- Neue MC-Version hinzufügen heißt: Node in `root.gradle.kts`, Eintrag in `settings.gradle.kts` und in
  `build.gradle.kts` die Blöcke `loaderVersion`, `fabricAPI`, `yarnVersion` und `supportedVersionRange`.
- `mod_version` steht in `gradle.properties`; `fabric.mod.json` wird beim Build mit Version, MC-Range und
  Loader-Version befüllt (`${version}` etc. nicht durch feste Werte ersetzen).

## Konventionen

- Lombok ist aktiv (`@Getter`, `@Setter`, `@RequiredArgsConstructor` …) und wird breit genutzt.
- Nahezu der gesamte Code liegt im `client`-SourceSet (`src/client/java`); `src/main` enthält fast nur
  Ressourcen und den Mod-Einstiegspunkt.
- Entwickelt wird direkt auf `main`, ohne Feature-Branches oder PRs.
- Die älteren Versionen (1.21.4–1.21.6) sind Altlasten; **26.2 ist das primäre Ziel** für neue Arbeit.
