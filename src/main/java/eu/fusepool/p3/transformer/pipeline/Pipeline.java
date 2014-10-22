package eu.fusepool.p3.transformer.pipeline;

import eu.fusepool.p3.transformer.client.Transformer;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.WritingEntity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;
import org.apache.commons.io.IOUtils;

/**
 * This class represents a pipeline for applying transformers in sequence.
 *
 * @author Gabor
 */
public class Pipeline {

    private Set<MimeType> supportedInputFormats;
    private Set<MimeType> supportedOutputFormats;
    private Map<Integer, Transformer> transformers;

    public Pipeline() {
        transformers = new HashMap<>();
    }

    /**
     * Checks if there is any transformer in the pipeline.
     *
     * @return
     */
    public Boolean isEmpty() {
        return transformers.isEmpty();
    }

    /**
     * Add new transformer to the pipeline.
     *
     * @param transformer
     */
    public void addTransformer(Transformer transformer) {
        transformers.put(transformers.size(), transformer);
    }

    /**
     * Get supported input formats of the pipeline.
     *
     * @return
     */
    public Set<MimeType> getSupportedInputFormats() {
        return supportedInputFormats;
    }

    /**
     * Get supported output formats of the pipeline.
     *
     * @return
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
        supportedOutputFormats = transformers.get(transformers.size() - 1).getSupportedOutputFormats();
    }

    /**
     * It runs the pipeline by invoking the transform method of each transformer in the pipe in order, and supplying the output of each transformer to the next. The first transformer gets the Entity that was originally posted to the pipeline transformer. The output of the last transformer in the pipe is the output of the pipeline.
     *
     * @param data the original Entity posted to the pipeline transformer
     * @param accept the accept header for the last transformer
     * @return the Entity that is the output of the last transformer in the pipe
     * @throws java.io.IOException
     */
    public Entity run(Entity data, MimeType accept) throws IOException {
        final URI contentLocation = data.getContentLocation();
        Transformer current, next;
        MimeType[] acceptedMediaTypes;
        for (int i = 0; i < transformers.size(); i++) {
            // get the current transformer in the pipeline
            current = transformers.get(i);
            // get the next transformer in the pipeline
            next = transformers.get(i + 1);

            if (next != null) {
                // next is not null, set the accepted input formats of the next transformer
                acceptedMediaTypes = next.getSupportedInputFormats().toArray(new MimeType[next.getSupportedInputFormats().size()]);
            } else {
                // if next is null, there is no more transformer in the pipeline
                if (accept != null) {
                    // if accept is not null, set accepted mediatype explictly
                    acceptedMediaTypes = new MimeType[]{accept};
                } else {
                    // otherwise use the accepted mediatypes from the last transformer
                    acceptedMediaTypes = supportedOutputFormats.toArray(new MimeType[supportedOutputFormats.size()]);
                }
            }

            // call transfromer transform method
            data = current.transform(data, acceptedMediaTypes);

            final MimeType mimeType = data.getType();
            final InputStream inputStream = data.getData();

            data = new WritingEntity() {
                @Override
                public MimeType getType() {
                    return mimeType;
                }

                @Override
                public void writeData(OutputStream outputStream) throws IOException {
                    IOUtils.copy(inputStream, outputStream);
                }

                @Override
                public URI getContentLocation() {
                    return contentLocation;
                }
            };
        }
        return data;
    }

    /**
     * It validates the pipeline based on the supported media types of the transformers in the pipe. It takes the supported output formats of a transformer and compares it with the supported input formats of the next, and so on. A pipeline is valid if all the transformers support at least one of the output media types of the previous transformer in the pipe as input type.
     *
     * @return true, if the pipeline is valid, otherwise return false
     */
    public Boolean isValid() {
        Boolean valid;
        Transformer current, next;
        // if there is more than one transformer in the pipeline
        if (transformers.size() > 1) {
            for (int i = 0; i < transformers.size(); i++) {
                valid = false;
                current = transformers.get(i);
                next = transformers.get(i + 1);

                // if next is null, current is the last transformer in the pipeline
                if (next == null) {
                    break;
                }
                // loop through the supported output formats of current and check if
                // next valid any of them
                for (MimeType mimeType : current.getSupportedOutputFormats()) {
                    if (next.accepts(mimeType)) {
                        valid = true;
                    }
                }
                // if no output format of current is accepted by next as input format
                if (!valid) {
                    throw new RuntimeException("Incompatible transformers found in pipeline!");
                }
            }
        }
        return true;
    }
}
