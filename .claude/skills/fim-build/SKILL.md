---
name: fim-build
description: Baut eine FIM-Version oder startet ihren Minecraft-Client. Nutzen, wenn der Mod gebaut, eine Jar erzeugt oder der Client zum Testen gestartet werden soll. Argument ist die Zielversion (Default 26.2-fabric).
disable-model-invocation: true
---

Argument: `$ARGUMENTS` — Zielversion und optional `run`. Beispiele: `26.2`, `1.21.6 run`, leer = `26.2-fabric` bauen.

## Version auflösen

Gültige Projekte: `1.21.4-fabric`, `1.21.5-fabric`, `1.21.6-fabric`, `26.2-fabric`.
Ein Argument ohne `-fabric` ergänzen. Ohne Argument `26.2-fabric` verwenden.

## Ausführen

Immer den Wrapper `./gradlew` benutzen — ein System-Gradle bricht bei diesem Projekt ab
(`Cannot find module 'gradle-public-api-legacy'`).

- Bauen: `./gradlew :<version>:build`
- Client starten: `./gradlew :<version>:runClient` — **im Hintergrund** starten, das blockiert sonst.
  Alle Versionen teilen sich `run/` als Spielverzeichnis.

Builds dauern beim ersten Mal mehrere Minuten (Minecraft-Download, Remapping, Preprocessing).
Großzügiges Timeout setzen und im Hintergrund laufen lassen.

## Danach

Nach `build` liegt die Jar in `jars/` — Pfad und Dateigröße nennen.
Bei Compile-Fehlern in `versions/<version>/build/preprocessed/...`: das ist **generierter** Code. Die
Ursache steht im Root-`src/` oder in einem Override unter `versions/<version>/src/` — dort korrigieren,
niemals in `build/`.
