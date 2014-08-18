package eu.fusepoolp3.p3pipelinetrasformer;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.SyncExtractor;
import eu.fusepool.p3.transformer.client.Transformer;
import eu.fusepool.p3.transformer.client.TransformerClientImpl;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.InputStreamEntity;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Gabor
 */
public class PipelineTransformer implements SyncExtractor {

    @Override
    public Entity extract(HttpRequestEntity entity) throws IOException {
        // get mimetype of content
        final MimeType mimeType = entity.getType();
        // convert content to byte array
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(entity.getData(), baos);
        final byte[] bytes = baos.toByteArray();
        // create Entity from content
        final Entity input = new InputStreamEntity() {

            @Override
            public MimeType getType() {
                return mimeType;
            }

            @Override
            public InputStream getData() throws IOException {
                return new ByteArrayInputStream(bytes);
            }
        };
        // get query string
        final String queryString = entity.getRequest().getQueryString();
        
        HashMap<String, String> queryParams = new HashMap<>();
        // process query string
        if (queryString != null) {
            String[] params = queryString.split("&");
            String[] param;
            for (int i = 0; i < params.length; i++) {
                param = params[i].split("=", 2);
                queryParams.put(param[0], param[1]);
            }
        }
        // get uri of config file
        String uriString = queryParams.get("uri");
        // config uri must be supplied
        if (uriString == null) {
            throw new RuntimeException("No list of transformers was supplied!");
        }
        // create pipeline
        Pipeline pipeline = new Pipeline();
        Transformer transformer;
        URI uri;
        // check if config file uri is valid
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            throw new RuntimeException("URI syntax error!", e);
        }
        // read config file
        try (BufferedReader in = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.isEmpty()) {
                    // create transformer
                    transformer = new TransformerClientImpl(line);
                    // add transformer to pipeline
                    pipeline.addTransformer(transformer);
                }
            }
        }
        
        // set supported input and output formats of the pipeline transformer
        pipeline.setSupportedFormats();
        
        // validate pipeline
        if(!pipeline.validate()){
           throw new RuntimeException("Incompatible transformers!"); 
        }

        // run pipeline
        final Entity output = pipeline.run(input);
      
        return output;
    }

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        try {
            // TODO return supported input formats of first transformer 
            MimeType mimeType = new MimeType("text/plain;charset=UTF-8");
            return Collections.singleton(mimeType);
        } catch (MimeTypeParseException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Set<MimeType> getSupportedOutputFormats() {
        try {
            // TODO return supported input formats of last transformer
            MimeType mimeType = new MimeType("text/plain;charset=UTF-8");
            return Collections.singleton(mimeType);
        } catch (MimeTypeParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isLongRunning() {
        return false;
    }
}
