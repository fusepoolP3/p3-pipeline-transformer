package eu.fusepool.p3.transformer.pipeline.tests.sample;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.SyncTransformer;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.WritingEntity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.io.IOUtils;

/**
 * A simple transformer for junit tests. It expects text/plain and produces text/plain.
 *
 * @author Gabor
 */
public class SimpleTransformer implements SyncTransformer {

    @Override
    public Entity transform(HttpRequestEntity entity) throws IOException {
        final MimeType mimeType = entity.getType();
        final InputStream in = entity.getData();
        final Entity output = new WritingEntity() {
            @Override
            public MimeType getType() {
                return mimeType;
            }

            @Override
            public void writeData(OutputStream out) throws IOException {
                IOUtils.copy(in, out);
            }
        };

        return output;
    }

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        try {
            MimeType mimeType = new MimeType("text/plain");
            return Collections.singleton(mimeType);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Set<MimeType> getSupportedOutputFormats() {
        try {
            MimeType mimeType = new MimeType("text/plain");
            return Collections.singleton(mimeType);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean isLongRunning() {
        return false;
    }
}
