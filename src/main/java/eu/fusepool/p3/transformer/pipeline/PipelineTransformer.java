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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
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
    private int index;

    public PipelineTransformer(String queryString, String acceptHeader) {
        // get query params from query string
        queryParams = getQueryParams(queryString);

        // query string must not be empty
        if (queryParams.isEmpty()) {
            throw new TransformerException(HttpServletResponse.SC_BAD_REQUEST, "ERROR: No valid query param found! \nUsage: http://<pipeline_transformer>/?t=<transformer_1>&...&t=<transformer_N>");
        }

        // parsing of accept header
        try {
            AcceptPreference ac = AcceptPreference.fromString(acceptHeader);
            Set<MimeType> mimeTypes = new HashSet<>();
            for (AcceptHeaderEntry header : ac.getEntries()) {
                mimeTypes.add(header.getMediaType());
            }
            accept = mimeTypes.toArray(new MimeType[mimeTypes.size()]);
            System.out.println("---------------------------");
            for (MimeType accept1 : accept) {
                System.out.println(accept1.toString());
            }
        } catch (NullPointerException ex) {
            accept = null;
        }

        // create new pipeline if it does not exist
        if (pipeline == null) {
            pipeline = new Pipeline();

            Transformer transformer;
            for (int i = 1; i <= index; i++) {
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
                pipeline.validate();
            } else {
                // the pipeline should contain at least on transformer
                throw new TransformerException(HttpServletResponse.SC_BAD_REQUEST, "ERROR: Query string contains no transformer! \nUsage: http://<pipeline_transformer>/?t=<transformer_1>&...&t=<transformer_N>");
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

    /**
     * Get query parameters from a query string.
     *
     * @param queryString the query string
     * @return HashMap containing the query parameters
     */
    private Map<String, String> getQueryParams(String queryString) {
        try {
            Map<String, String> temp = new HashMap<>();
            // query string should not be empty or blank
            if (StringUtils.isNotBlank(queryString)) {
                String[] params = queryString.split("&");
                String[] param;
                index = 0;
                for (String item : params) {
                    param = item.split("=", 2);
                    param[0] += param[0].equals("t") ? ++index : "";
                    temp.put(param[0], param[1]);
                }
            }
            return temp;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new TransformerException(HttpServletResponse.SC_BAD_REQUEST, "ERROR: Failed to parse query string!\nUsage: http://<pipeline_transformer>/?t=<transformer_1>&...&t=<transformer_N>");
        }
    }
}
