# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Fabric-Client-Mod, der Minecraft an das IntelliJ-Plugin FIM anbindet. Fork von `FishIntegratedMinecraft/FIM-Fabric`.

## Build & Run

```bash
./gradlew build       # Jar landet in build/libs/
./gradlew runClient   # Spielverzeichnis ist run/
./gradlew runDatagen
```

Ein System-Gradle taugt hier nicht (Arch-Paket 9.7 bricht mit
`Cannot find module 'gradle-public-api-legacy'` ab) — immer den Wrapper benutzen. Alternativ die
Run-Configs, die Loom beim Gradle-Sync in `.idea/runConfigurations/` erzeugt.

Es gibt keine Tests.

## Minecraft-Version

- Der Mod zielt auf genau **eine** Minecraft-Version, immer die neueste. Es gibt kein Multi-Versioning,
  keine Kompatibilitäts-Shims und keine Versionsweichen — und es sollen auch keine eingeführt werden.
- Minecraft ist unobfuskiert: Plugin `fabric-loom` (nicht `fabric-loom-remap`), Mojang-Namen, keine
  Mappings, **Java 25**.
- Minecraft-, Loader-, Fabric-API- und Lombok-Version stehen in `gradle/libs.versions.toml`.
- Versionssprung heißt: Versionen im Katalog anheben (siehe https://fabricmc.net/develop), Compile-Fehler
  und Mixin-Ziele nachziehen, `runClient` prüfen. Alter Code für die Vorgängerversion fliegt raus.
- `mod_version` steht in `gradle.properties`; `fabric.mod.json` wird beim Build mit Version, MC-Version,
  Loader- und Java-Version befüllt (`${version}` etc. nicht durch feste Werte ersetzen).

## Konventionen

- Lombok ist aktiv (`@Getter`, `@Setter`, `@RequiredArgsConstructor` …) und wird breit genutzt.
- Nahezu der gesamte Code liegt im `client`-SourceSet (`src/client/java`); `src/main` enthält fast nur
  Ressourcen und den Mod-Einstiegspunkt.
- Mixins heißen nach ihrer Zielklasse (`GuiMixin` → `Gui`) und stehen in `fim.client.mixins.json`. Dort gilt
  `defaultRequire: 1`, ein nicht greifender Inject bricht also den Start ab.
- Entwickelt wird direkt auf `main`, ohne Feature-Branches oder PRs.
