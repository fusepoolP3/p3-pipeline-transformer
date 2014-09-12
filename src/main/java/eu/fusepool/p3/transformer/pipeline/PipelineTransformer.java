package eu.fusepool.p3.transformer.pipeline;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.SyncTransformer;
import eu.fusepool.p3.transformer.client.Transformer;
import eu.fusepool.p3.transformer.client.TransformerClientImpl;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.InputStreamEntity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * This class represents a pipeline transformer.
 * 
 * @author Gabor
 */
public class PipelineTransformer implements SyncTransformer {

    private Map<String, String> queryParams;
    private Pipeline pipeline;

    public PipelineTransformer(String queryString) {
        // get query params from query string
        queryParams = getQueryParams(queryString);

        // query string must not be empty
        if (queryParams.isEmpty()) {
            throw new RuntimeException("Query string must not be empty!");
        }

        // create new pipeline if it does not exist
        if (pipeline == null) {
            pipeline = new Pipeline();

            Transformer transformer;
            for (int i = 1; i <= queryParams.size(); i++) {
                // get transformer URI from query params
                String transformerURI = queryParams.get("t" + i);
                // query param should not be empty or blank
                if (StringUtils.isNotBlank(transformerURI)) {
                    try {
                        // decode transformer URI
                        transformerURI = URLDecoder.decode(transformerURI, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    // create transformer
                    transformer = new TransformerClientImpl(transformerURI);
                    // add transformer to pipeline
                    pipeline.addTransformer(transformer);
                }
            }

            if (!pipeline.isEmpty()) {
                // set supported input and output formats of the pipeline transformer
                pipeline.setSupportedFormats();

                // validate pipeline
                if (!pipeline.isValid()) {
                    throw new RuntimeException("Incompatible transformers!");
                }
            } else {
                // the pipeline should contain at least on transformer
                throw new RuntimeException("Pipeline contains no transformer!");
            }
        }
    }

    @Override
    public Entity transform(HttpRequestEntity entity) throws IOException {
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

        // run pipeline
        final Entity output = pipeline.run(input);

        return output;
    }

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        return pipeline.getSupportedInputFormats();
    }

    @Override
    public Set<MimeType> getSupportedOutputFormats() {
        return pipeline.getSupportedOutputFormats();
    }

    @Override
    public boolean isLongRunning() {
        return false;
    }

    /**
     * Get query parameters from a query string.
     * 
     * @param queryString the query string
     * @return HashMap containing the query parameters
     */
    private Map<String, String> getQueryParams(String queryString) {
        Map<String, String> temp = new HashMap<>();
        // query string should not be empty or blank
        if (StringUtils.isNotBlank(queryString)) {
            String[] params = queryString.split("&");
            String[] param;
            for (int i = 0; i < params.length; i++) {
                param = params[i].split("=", 2);
                temp.put(param[0], param[1]);
            }
        }
        return temp;
    }
}
