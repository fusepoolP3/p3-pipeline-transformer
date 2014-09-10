package eu.fusepoolp3.p3pipelinetrasformer;

import eu.fusepool.p3.transformer.client.Transformer;
import eu.fusepool.p3.transformer.commons.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;

/**
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

    public Boolean isEmpty(){
        return transformers.isEmpty();
    }
    
    public void addTransformer(Transformer transformer) {
        index = transformers.size();
        transformers.put(index, transformer);
    }

    public Set<MimeType> getSupportedInputFormats() {
        return supportedInputFormats;
    }

    public Set<MimeType> getSupportedOutputFormats() {
        return supportedOutputFormats;
    }

    public void setSupportedFormats() {
        // set supported input formats of the pipeline to the supported input formats
        // of the first transformer in the pipeline
        supportedInputFormats = transformers.get(0).getSupportedInputFormats();
        // set supported output formats of the pipeline to the supported output formats
        // of the last transformer in the pipeline
        supportedOutputFormats = transformers.get(index).getSupportedOutputFormats();
    }

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
