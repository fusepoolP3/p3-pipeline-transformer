package eu.fusepool.p3.transformer.pipeline;

import eu.fusepool.p3.transformer.client.Transformer;
import eu.fusepool.p3.transformer.commons.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;

/**
 * This class represents a pipeline for applying transformers in sequence.
 * 
 * @author Gabor
 */
public class Pipeline {

    private Set<MimeType> supportedInputFormats;
    private Set<MimeType> supportedOutputFormats;
    private Map<Integer, Transformer> transformers;
    private int index;

    public Pipeline() {
        transformers = new HashMap<>();
        index = 0;
    }

    /**
     * Checks if there is any transformer in the pipeline.
     * @return 
     */
    public Boolean isEmpty() {
        return transformers.isEmpty();
    }

    public void addTransformer(Transformer transformer) {
        index = transformers.size();
        transformers.put(index, transformer);
    }

    /**
     * Get supported input formats of the pipeline.
     */
    public Set<MimeType> getSupportedInputFormats() {
        return supportedInputFormats;
    }

    /**
     * Get supported output formats of the pipeline.
     */
    public Set<MimeType> getSupportedOutputFormats() {
        return supportedOutputFormats;
    }

    /**
     * Sets the supported input and output formats of the pipeline.
     */
    public void setSupportedFormats() {
        // set supported input formats of the pipeline to the supported input formats
        // of the first transformer in the pipeline
        supportedInputFormats = transformers.get(0).getSupportedInputFormats();
        // set supported output formats of the pipeline to the supported output formats
        // of the last transformer in the pipeline
        supportedOutputFormats = transformers.get(index).getSupportedOutputFormats();
    }

    /**
     * It runs the pipeline by invoking the transform method of each transformer
     * in the pipe in order, and supplying the output of each transformer to the
     * next. The first transformer gets the Entity that was originally posted to
     * the pipeline transformer. The output of the last transformer in the pipe
     * is the output of the pipeline.
     *
     * @param data original Entity posted to the pipeline transformer
     * @return the Entity that is the output of the last transformer in the pipe
     */
    public Entity run(Entity data) {
        Transformer t;
        for (int i = 0; i < index + 1; i++) {
            t = transformers.get(i);
            if (t == null) {
                throw new RuntimeException("Transformer cannot be null!");
            }
            data = t.transform(data, getAcceptedMediaTypes(i));
        }
        return data;
    }

    /**
     * It validates the pipeline based on the supported media types of the
     * transformers in the pipe. It takes the supported output formats of a
     * transformer and compares it with the supported input formats of the next,
     * and so on. A pipeline is valid if all the transformers support at least
     * one of the output media types of the previous transformer in the pipe as
     * input type.
     *
     * @return true, if the pipeline is valid, otherwise return false
     */
    public Boolean isValid() {
        Boolean accepts;
        Transformer t1, t2;
        // if there is more than one transformer in the pipeline
        if (index > 0) {
            for (int i = 0; i < index; i++) {
                accepts = false;
                t1 = transformers.get(i);
                t2 = transformers.get(i + 1);
                // t1, t2 cannot be null if there is more than one transformer
                // in the pipeline
                if (t1 == null || t2 == null) {
                    throw new RuntimeException("Transformer cannot be null!");
                }
                // loop through the supported output formats of t1 and check if
                // t2 accepts any of them
                for (MimeType mimeType : t1.getSupportedOutputFormats()) {
                    if (t2.accepts(mimeType)) {
                        accepts = true;
                    }
                }
                // if no output format of t1 is accepted by t2 as input format
                if (!accepts) {
                    throw new RuntimeException("Incompatible transformers found in pipeline!");
                }
            }
        }
        return true;
    }

    /**
     * Get accepted media types of the next transformer.
     * 
     * @param key index of the current transformer
     * @return the array of the supported MimyTypes of the next transformer
     */
    private MimeType[] getAcceptedMediaTypes(int key) {
        // get the next transformer
        Transformer t = transformers.get(key + 1);

        if (t != null) {
            // t is not null, t is the next transformer in the pipeline,
            // return the accepted input formats of the next transformer
            return t.getSupportedInputFormats().toArray(new MimeType[t.getSupportedInputFormats().size()]);
        } else {
            // if t is null, there is no more transformer in the pipeline, 
            // return the output accepted formats of the pipeline transformer itself
            return supportedOutputFormats.toArray(new MimeType[supportedOutputFormats.size()]);
        }
    }
}
