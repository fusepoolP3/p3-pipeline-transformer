/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepoolp3.p3pipelinetrasformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;

/**
 *
 * @author Gabor
 */
public class Pipeline {

    final private Set<MimeType> supportedInputFormats;
    final private Set<MimeType> supportedOutputFormats;
    private Map<Integer, Transformer> transformers;
    private int index;

    public Pipeline(Set<MimeType> _supportedInputFormats, Set<MimeType> _supportedOutputFormats) {
        supportedInputFormats = _supportedInputFormats;
        supportedOutputFormats = _supportedOutputFormats;

        transformers = new HashMap<>();
        index = 0;
    }

    public void addTransformer(Transformer transformer) {
        index = transformers.size();
        transformers.put(index, transformer);
    }

    public String run(String data) {
        Transformer t;
        for (int i = 0; i < index + 1; i++) {
            t = transformers.get(i);
            if (t == null) {
                throw new RuntimeException("Transformer cannot be null!");
            }
            data = t.run(data, getNextAcceptedMediaTypes(i));
        }
        return data;
    }

    public Boolean Validate() {
        Transformer t1, t2;

        for (int i = 0; i < index + 1; i++) {
            t1 = transformers.get(i);
            t2 = transformers.get(i + 1);
            // t1 cannot be null
            if (t1 == null) {
                throw new RuntimeException("Transformer cannot be null!");
            }
            if (t2 != null) {
                if (!t1.isCompatible(t2)) {
                    return false;
                }
            } else {
                // if t2 null there is no more transformer in the pipeline
                if (!this.isCompatible(t1)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getNextAcceptedMediaTypes(int key) {
        // get the next transformer
        Transformer t = transformers.get(key + 1);

        if (t != null) {
            // t is not null, t is the next transformer in the pipeline,
            // return the accepted input formats of the next transformer
            return getSupportedFormatsAsString(t.supportedInputFormats);
        } else {
            // if t is null, there is no more transformer in the pipeline, 
            // return the output accepted formats of the pipeline transformer itself
            return getSupportedFormatsAsString(supportedOutputFormats);
        }
    }

    private String getSupportedFormatsAsString(Set<MimeType> supportedFormats) {
        String result = "";
        int temp = 0;

        for (MimeType m : supportedFormats) {
            if (temp == 0) {
                result += m.toString();
            } else {
                result += ", " + m.toString();
            }
            temp++;
        }

        return result;
    }

    private Boolean isCompatible(Transformer t) {
        for (MimeType m : supportedOutputFormats) {
            for (MimeType m2 : t.supportedOutputFormats) {
                if (m.match(m2)) {
                    return true;
                }
            }
        }
        return false;
    }
}
