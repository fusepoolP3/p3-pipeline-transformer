package eu.fusepool.p3.transformer.pipeline;

import eu.fusepool.p3.accept.util.AcceptPreference;
import eu.fusepool.p3.accept.util.AcceptPreference.AcceptHeaderEntry;
import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.SyncTransformer;
import eu.fusepool.p3.transformer.TransformerException;
import eu.fusepool.p3.transformer.client.Transformer;
import eu.fusepool.p3.transformer.client.TransformerClientImpl;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.InputStreamEntity;
import eu.fusepool.p3.transformer.pipeline.cache.Cache;
import eu.fusepool.p3.transformer.pipeline.util.HTTPClient;
import eu.fusepool.p3.transformer.pipeline.util.Utils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;
import javax.servlet.http.HttpServletResponse;
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
    private MimeType[] accept;

    public PipelineTransformer(String queryString, String acceptHeader) {
        // query string cannot be empty
        if (StringUtils.isEmpty(queryString)) {
            throw new TransformerException(HttpServletResponse.SC_BAD_REQUEST, "ERROR: Query string must not be empty! \nUsage: http://<pipeline_transformer_uri>/?config=<config_uri>");
        }

        // get query params from query string
        queryParams = Utils.getQueryParams(queryString);
        // query string must not be empty
        if (queryParams.isEmpty()) {
            throw new TransformerException(HttpServletResponse.SC_BAD_REQUEST, "ERROR: No valid query param found! \nUsage: http://<pipeline_transformer_uri>/?config=<config_uri>");
        }

        // parsing of accept header
        try {
            AcceptPreference ac = AcceptPreference.fromString(acceptHeader);
            Set<MimeType> mimeTypes = new HashSet<>();
            for (AcceptHeaderEntry header : ac.getEntries()) {
                mimeTypes.add(header.getMediaType());
            }
            accept = mimeTypes.toArray(new MimeType[mimeTypes.size()]);
        } catch (NullPointerException ex) {
            accept = null;
        }

        // processing config rdf
        String configURI = queryParams.get("config");
        // config URI
        if (StringUtils.isEmpty(configURI)) {
            throw new TransformerException(HttpServletResponse.SC_BAD_REQUEST, "ERROR: No config URI was found in query string! \nUsage: http://<pipeline_transformer_uri>/?config=<config_uri>");
        }

        // get config resource supplied URI
        List<String> transformerURIs = HTTPClient.getTransformers(configURI);

        // create new pipeline if it does not exist
        if (pipeline == null) {
            pipeline = new Pipeline();

            Transformer transformer;
            for (String transformerURI : transformerURIs) {
                // query param should not be empty
                if (StringUtils.isNotEmpty(transformerURI)) {
                    // get tranformer from cache
                    transformer = Cache.getTransformer(transformerURI);
                    // if not found in cache
                    if (transformer == null) {
                        // create transformer
                        transformer = new TransformerClientImpl(transformerURI);
                        // add to cache
                        Cache.setTransformer(transformerURI, transformer);
                    }
                    // add transformer to pipeline
                    pipeline.addTransformer(transformer);
                }
            }

            if (!pipeline.isEmpty()) {
                // set supported input and output formats of the pipeline transformer
                pipeline.setSupportedFormats();

                // validate pipeline
                pipeline.validate();
            } else {
                // the pipeline should contain at least on transformer
                throw new TransformerException(HttpServletResponse.SC_BAD_REQUEST, "ERROR: Supplied pipeline contains no transformer! \nUsage: http://<pipeline_transformer_uri>/?pipeline=<config_uri>");
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

        // get content location header
        final URI contentLocation = entity.getContentLocation();

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

            @Override
            public URI getContentLocation() {
                return contentLocation;
            }
        };

        // run pipeline
        final Entity output = pipeline.run(input, accept);

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
}
