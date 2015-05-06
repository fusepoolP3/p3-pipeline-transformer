package eu.fusepool.p3.transformer.pipeline.cache;

import eu.fusepool.p3.transformer.client.Transformer;
import java.util.Calendar;

/**
 *
 * @author Gabor
 */
public class CachedTransformer {

    final private Calendar created;
    final private Transformer transformer;

    public CachedTransformer(Transformer transformer) {
        this.created = Calendar.getInstance();
        this.transformer = transformer;
    }

    /**
     * Checks if the cached instance is older than an hour.
     *
     * @return
     */
    public Boolean isValid() {
        Calendar validDate = Calendar.getInstance();
        validDate.add(Calendar.HOUR_OF_DAY, -1);
        return validDate.before(created);
    }

    /**
     * Returns transformer instance.
     *
     * @return
     */
    public Transformer getTransformer() {
        return transformer;
    }
}
