package fish.crafting.fimfabric.inspect.dump;

import com.mojang.blaze3d.platform.NativeImage;
import fish.crafting.fimfabric.client.FIMModClient;
import fish.crafting.fimfabric.inspect.pack.PackInspector;
import fish.crafting.fimfabric.inspect.pack.PackReport;
import fish.crafting.fimfabric.inspect.view.ViewCollector;
import fish.crafting.fimfabric.inspect.view.ViewSnapshot;
import fish.crafting.fimfabric.rendering.InformationFeedManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class Dumper {

    private static boolean viewRequested = false;

    /**
     * The dump is taken from the next frame, which FIM stays out of, so neither the snapshot nor the
     * screenshot shows the menu the request came from.
     */
    public static void requestViewDump(){
        viewRequested = true;
    }

    /**
     * Called while a frame is put together.
     *
     * @return Whether this frame is the one being dumped and has to stay free of FIM's interface
     */
    public static boolean captureFrame(){
        if(!viewRequested) return false;
        viewRequested = false;

        Minecraft mc = Minecraft.getInstance();
        try{
            ViewSnapshot snapshot = ViewCollector.collect();
            //Runs ahead of the next frame, while the render target still holds this one
            mc.schedule(() -> Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image ->
                    CompletableFuture.runAsync(() -> writeView(snapshot, image), Util.ioPool())));
        }catch (Exception e){
            failed(e);
        }

        return true;
    }

    private static void writeView(ViewSnapshot snapshot, NativeImage screenshot){
        try(screenshot){
            PackReport report = PackInspector.inspect(Minecraft.getInstance().getResourceManager());

            DumpWriter writer = DumpWriter.create(snapshot.label());
            writer.writeView(snapshot);
            writer.writeScreenshot(screenshot);
            writer.writePacks(report);
            writer.writeReadme(snapshot, report);

            done("Dumped view", writer.directory());
        }catch (Exception e){
            failed(e);
        }
    }

    /**
     * Only the fonts and shaders, for when the report has been put together already.
     */
    public static void dumpPacks(PackReport report, String label){
        CompletableFuture.runAsync(() -> {
            try{
                DumpWriter writer = DumpWriter.create(label + "_packs");
                writer.writePacks(report);
                writer.writeReadme(null, report);

                done("Dumped pack sources", writer.directory());
            }catch (Exception e){
                failed(e);
            }
        }, Util.ioPool());
    }

    private static void done(String what, Path directory){
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.keyboardHandler.setClipboard(directory.toAbsolutePath().toString());
            InformationFeedManager.success(what + " to " + DumpWriter.ROOT + "/" + directory.getFileName() + ", path copied", true);
        });
    }

    private static void failed(Exception e){
        FIMModClient.LOGGER.error("Dump failed", e);
        Minecraft.getInstance().execute(() -> InformationFeedManager.error("Dump failed: " + e, true));
    }

}
