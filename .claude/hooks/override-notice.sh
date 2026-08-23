#!/usr/bin/env bash
# PostToolUse (Write|Edit): meldet Edits an versionsspezifischen Overrides unter versions/<mc>/src/.
# Solche Dateien ersetzen den preprozessierten Code nur für diese eine Minecraft-Version —
# die Hauptquelle im Root-src/ bleibt unverändert.
set -u

file=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty')
[ -n "$file" ] || exit 0

case "$file" in
  */versions/*/src/*|versions/*/src/*) ;;
  *) exit 0 ;;
esac

version=$(printf '%s' "$file" | sed -E 's|.*versions/([^/]+)/src/.*|\1|')

jq -n --arg v "$version" '{
  systemMessage: ("FIM: Override für " + $v + " bearbeitet – Root-src/ ist davon unberührt."),
  hookSpecificOutput: {
    hookEventName: "PostToolUse",
    additionalContext: ("Die bearbeitete Datei ist ein versionsspezifischer Override unter versions/" + $v + "/src/. Die Änderung gilt ausschließlich für " + $v + " und wird von keiner anderen Version übernommen. Prüfe, ob die Hauptquelle im Root-src/ oder die Overrides der anderen Versionen nachgezogen werden müssen.")
  }
}'
