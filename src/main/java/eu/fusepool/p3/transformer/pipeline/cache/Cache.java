package eu.fusepool.p3.transformer.pipeline.cache;

import eu.fusepool.p3.transformer.client.Transformer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Gabor
 */
public class Cache {

    private static final Map<String, CachedTransformer> transformerCache = new HashMap<>();

    /**
     * Get a transformer from the cache.
     *
     * @param uri
     * @return
     */
    public static synchronized Transformer getTransformer(String uri) {
        CachedTransformer temp = transformerCache.get(uri);
        if (temp != null) {
            if (temp.isValid()) {
                return temp.getTransformer();
            }
        }
        return null;
    }

    /**
     * Add a new transformer to the cache.
     *
     * @param uri
     * @param transformer
     */
    public static synchronized void setTransformer(String uri, Transformer transformer) {
        transformerCache.put(uri, new CachedTransformer(transformer));
    }
}
