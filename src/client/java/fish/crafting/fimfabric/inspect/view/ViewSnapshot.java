package fish.crafting.fimfabric.inspect.view;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

/**
 * Everything the client is showing at one moment.
 *
 * @param label Short name of where it was taken, used to name the dump
 * @param fontsUsed Font -> the places inside {@code json} that use it
 */
public record ViewSnapshot(String label, JsonObject json, Map<String, Set<String>> fontsUsed) {
}
