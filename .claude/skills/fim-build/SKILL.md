---
name: fim-build
description: Baut den FIM-Mod oder startet den Minecraft-Client. Nutzen, wenn der Mod gebaut, eine Jar erzeugt oder der Client zum Testen gestartet werden soll. Argument `run` startet den Client statt zu bauen.
disable-model-invocation: true
---

Argument: `$ARGUMENTS` — leer = bauen, `run` = Client starten.

## Ausführen

Immer den Wrapper `./gradlew` benutzen — ein System-Gradle bricht bei diesem Projekt ab
(`Cannot find module 'gradle-public-api-legacy'`).

- Bauen: `./gradlew build`
- Client starten: `./gradlew runClient` — **im Hintergrund** starten, das blockiert sonst.
  Spielverzeichnis ist `run/`.

Der erste Build nach einem Versionssprung dauert mehrere Minuten (Minecraft-Download).
Großzügiges Timeout setzen und im Hintergrund laufen lassen.

## Danach

Nach `build` liegt die Jar in `build/libs/` — Pfad und Dateigröße nennen.
