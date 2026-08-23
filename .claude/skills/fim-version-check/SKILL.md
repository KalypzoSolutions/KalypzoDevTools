---
name: fim-version-check
description: Prüft nach einer Code-Änderung, ob das Multi-Version-Setup konsistent bleibt — Preprocessor-Direktiven, Overrides unter versions/*/src und die Yarn-zu-Mojang-Mapping-Datei. Nutzen, nachdem im Root-src/ oder in einem Version-Override editiert wurde.
---

Prüft, ob eine Änderung alle vier Versionen (`1.21.4-fabric`, `1.21.5-fabric`, `1.21.6-fabric`, `26.2-fabric`)
sauber trägt. Kernversion ist die in `versions/mainProject` genannte (aktuell `1.21.6-fabric`), deren
Quellcode im Root-`src/` liegt.

## Ablauf

1. **Geänderte Dateien ermitteln**: `git status --porcelain` und `git diff --stat`.

2. **Overrides prüfen.** Für jede geänderte Datei unter `src/` nachsehen, ob es gleichnamige Overrides gibt:
   ```bash
   git ls-files 'versions/*/src' | grep '<Dateiname>'
   ```
   Existiert ein Override, bekommt diese Version die Änderung **nicht** automatisch. Bewerten, ob der
   Override nachgezogen werden muss, und das melden.

3. **Umgekehrte Richtung.** Wurde eine Datei unter `versions/<v>/src/` geändert, prüfen, ob dieselbe Logik
   im Root-`src/` (und in den Overrides der anderen Versionen) fehlt.

4. **Preprocessor-Direktiven prüfen.** In geänderten Dateien nach `//#if` / `//#else` / `//#endif` suchen:
   - Jeder `//#if`-Block muss ein `//#endif` haben.
   - Inaktive Zweige sind mit `//$$` auskommentiert; aktive nicht. Neu eingefügte Zeilen in einem inaktiven
     Zweig brauchen also ebenfalls `//$$`.
   - MC-Konstanten: `12104`, `12105`, `12106`, `260200`.
   - Neue API-Nutzung, die es in älteren Versionen nicht gibt, braucht eine Weiche oder einen Override.

5. **Mapping-Datei prüfen.** Werden im Root-`src/` (Yarn-Namen, 1.21.6) Minecraft-Klassen, -Methoden oder
   -Felder neu verwendet, die in 26.2 anders heißen, muss ein Eintrag in
   `versions/mapping-1.21.6-26.2.txt` existieren. Format und Beispiele stehen im Kopf der Datei.

6. **Bauen.** Zum Bestätigen `/fim-build <version>` für die betroffenen Versionen laufen lassen —
   mindestens `26.2-fabric` und die Kernversion.

## Ausgabe

Knappe Liste pro Version: in Ordnung / offener Punkt mit konkretem Dateipfad. Nichts stillschweigend
korrigieren, was der Nutzer nicht angefragt hat — Befunde melden und nachfragen.
