package fish.crafting.fimfabric.ai;

import fish.crafting.fimfabric.client.FIMModClient;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Talks to AI command line tools that are installed on this machine, like Claude Code or the
 * OpenAI Codex CLI. Nothing here knows about items: it takes a prompt and gives back text, so any
 * feature can use it.
 *
 * <pre>{@code
 * List<LocalAi.Backend> available = LocalAi.available();
 * LocalAi.ask(available.getFirst(), "Rewrite this: ...").thenAccept(text -> ...);
 * }</pre>
 */
public final class LocalAi {

    private static final long TIMEOUT_SECONDS = 180;

    private LocalAi(){}

    public enum Backend {
        CLAUDE_CODE("Claude Code", "claude", "-p"),
        CODEX("OpenAI Codex", "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-");

        private final String label;
        private final String executable;
        private final List<String> arguments;

        Backend(String label, String executable, String... arguments){
            this.label = label;
            this.executable = executable;
            this.arguments = List.of(arguments);
        }

        public String label(){
            return label;
        }
    }

    /**
     * The tools that can be found on the PATH. Looks at the file system only, so it is cheap enough
     * to call when a screen opens.
     */
    public static @NotNull List<Backend> available(){
        List<Backend> found = new ArrayList<>();
        for (Backend backend : Backend.values()) {
            if(find(backend.executable) != null) found.add(backend);
        }
        return found;
    }

    /**
     * Sends the prompt through stdin, so it never has to be escaped for a shell, and answers with
     * what the tool printed. Runs off the render thread, complete the result back on it yourself.
     *
     * The tool starts in an empty temporary directory so it has no project to look at or change.
     */
    public static @NotNull CompletableFuture<String> ask(@NotNull Backend backend, @NotNull String prompt){
        return CompletableFuture.supplyAsync(() -> run(backend, prompt), Util.backgroundExecutor());
    }

    private static String run(Backend backend, String prompt){
        Path executable = find(backend.executable);
        if(executable == null) throw new IllegalStateException(backend.label + " was not found on the PATH");

        Path directory = null;
        Process process = null;
        try {
            directory = Files.createTempDirectory("kalypzo-ai");

            List<String> command = new ArrayList<>();
            //Windows runs .cmd shims through the command interpreter, they are no executables
            String name = executable.getFileName().toString().toLowerCase(Locale.ROOT);
            if(name.endsWith(".cmd") || name.endsWith(".bat")){
                command.add("cmd.exe");
                command.add("/c");
            }
            command.add(executable.toString());
            command.addAll(backend.arguments);

            process = new ProcessBuilder(command)
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .start();

            try (var stdin = process.getOutputStream()) {
                stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
            }

            //Reading blocks until the tool is done, so the timeout has to come from the outside
            Process running = process;
            CompletableFuture.delayedExecutor(TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(running::destroyForcibly);

            InputStream stdout = process.getInputStream();
            byte[] output = stdout.readAllBytes();

            process.waitFor();

            String text = new String(output, StandardCharsets.UTF_8).strip();
            if(process.exitValue() != 0){ // also what a timeout looks like
                throw new IllegalStateException(backend.label + " failed: " + shorten(text));
            }
            return text;
        } catch (IOException e) {
            throw new IllegalStateException("Could not run " + backend.label + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } finally {
            if(process != null && process.isAlive()) process.destroyForcibly();
            if(directory != null){
                try {
                    Files.deleteIfExists(directory);
                } catch (IOException e) {
                    FIMModClient.LOGGER.debug("Could not remove {}", directory, e);
                }
            }
        }
    }

    private static String shorten(String text){
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }

    /**
     * Looks the way a shell would, including the extensions Windows tries.
     */
    private static Path find(String executable){
        String path = System.getenv("PATH");
        if(path == null) return null;

        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        String[] extensions = windows ? new String[]{".exe", ".cmd", ".bat"} : new String[]{""};

        for (String directory : path.split(java.io.File.pathSeparator)) {
            if(directory.isBlank()) continue;

            for (String extension : extensions) {
                try {
                    Path candidate = Path.of(directory, executable + extension);
                    if(Files.isRegularFile(candidate)) return candidate;
                } catch (RuntimeException ignored) {
                    //A PATH entry that is not a valid path
                }
            }
        }
        return null;
    }

}
