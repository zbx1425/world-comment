package cn.zbx1425.worldcomment;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Hierarchical environment-variable config source, modeled after the Rust config crate's
 * Environment source. Variables prefixed with {@link #PREFIX} are split into path segments
 * by {@link #SEPARATOR}; each UPPER_SNAKE segment maps to a lowerCamel JSON key, numeric
 * segments index into arrays, and scalar values are parsed as boolean / integer / decimal
 * when possible. The resulting overlay is deep-merged over the config file, env winning.
 */
public class ConfigEnvironment {

    public static final String PREFIX = "SUBNOTEICA_";
    public static final String SEPARATOR = "__";

    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("\\d+");

    /**
     * Returns a deep merge of {@code base} (config file JSON) with the environment-derived
     * overlay, environment taking precedence. {@code base} is not modified.
     */
    public static JsonObject mergeWithEnvironment(JsonObject base) {
        return mergeWithEnvironment(base, System.getenv());
    }

    static JsonObject mergeWithEnvironment(JsonObject base, Map<String, String> env) {
        JsonObject overlay = new JsonObject();
        List<String> appliedKeys = new ArrayList<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (!entry.getKey().startsWith(PREFIX)) continue;
            String path = entry.getKey().substring(PREFIX.length());
            if (path.isEmpty()) continue;
            try {
                JsonElement newRoot = setPath(overlay, path.split(SEPARATOR), 0, parseScalar(entry.getValue()));
                if (newRoot.isJsonObject()) {
                    overlay = newRoot.getAsJsonObject();
                    appliedKeys.add(entry.getKey());
                } else {
                    Main.LOGGER.warn("Ignoring config environment variable {}: array index at root level", entry.getKey());
                }
            } catch (Exception ex) {
                Main.LOGGER.warn("Ignoring malformed config environment variable {}", entry.getKey(), ex);
            }
        }
        if (!appliedKeys.isEmpty()) {
            Main.LOGGER.info("Applied config override(s) from environment: {}", String.join(", ", appliedKeys));
        }
        return deepMerge(base, overlay).getAsJsonObject();
    }

    private static JsonElement setPath(JsonElement node, String[] segments, int index, JsonElement value) {
        if (index == segments.length) return value;
        String segment = segments[index];
        if (NUMERIC_SEGMENT.matcher(segment).matches()) {
            JsonArray array = node != null && node.isJsonArray() ? node.getAsJsonArray() : new JsonArray();
            int i = Integer.parseInt(segment);
            while (array.size() <= i) array.add(JsonNull.INSTANCE);
            JsonElement child = array.get(i);
            array.set(i, setPath(child.isJsonNull() ? null : child, segments, index + 1, value));
            return array;
        } else {
            JsonObject object = node != null && node.isJsonObject() ? node.getAsJsonObject() : new JsonObject();
            String key = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, segment);
            JsonElement child = object.get(key);
            object.add(key, setPath(child == null || child.isJsonNull() ? null : child, segments, index + 1, value));
            return object;
        }
    }

    private static JsonElement parseScalar(String raw) {
        if (raw.equalsIgnoreCase("true")) return new JsonPrimitive(true);
        if (raw.equalsIgnoreCase("false")) return new JsonPrimitive(false);
        try {
            return new JsonPrimitive(Long.parseLong(raw));
        } catch (NumberFormatException ignored) {
        }
        if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
            try {
                return new JsonPrimitive(Double.parseDouble(raw));
            } catch (NumberFormatException ignored) {
            }
        }
        return new JsonPrimitive(raw);
    }

    private static JsonElement deepMerge(JsonElement base, JsonElement overlay) {
        if (overlay == null || overlay.isJsonNull()) {
            return base != null ? base : JsonNull.INSTANCE;
        }
        if (base != null && base.isJsonObject() && overlay.isJsonObject()) {
            JsonObject result = base.getAsJsonObject().deepCopy();
            for (Map.Entry<String, JsonElement> entry : overlay.getAsJsonObject().entrySet()) {
                result.add(entry.getKey(), deepMerge(result.get(entry.getKey()), entry.getValue()));
            }
            return result;
        }
        if (base != null && base.isJsonArray() && overlay.isJsonArray()) {
            JsonArray baseArray = base.getAsJsonArray();
            JsonArray overlayArray = overlay.getAsJsonArray();
            JsonArray result = baseArray.deepCopy();
            for (int i = 0; i < overlayArray.size(); i++) {
                JsonElement overlayItem = overlayArray.get(i);
                if (overlayItem.isJsonNull()) continue; // gap filler from setPath
                if (i < result.size()) {
                    result.set(i, deepMerge(result.get(i), overlayItem));
                } else {
                    result.add(overlayItem);
                }
            }
            return result;
        }
        return overlay;
    }
}
