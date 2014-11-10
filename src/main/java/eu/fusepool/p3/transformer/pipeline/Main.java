package eu.fusepool.p3.transformer.pipeline;

import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerException;
import eu.fusepool.p3.transformer.TransformerFactory;
import eu.fusepool.p3.transformer.server.TransformerServer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.wymiwyg.commons.util.arguments.ArgumentHandler;

public class Main {

    public static void main(String[] args) throws Exception {
        Arguments arguments = ArgumentHandler.readArguments(Arguments.class, args);
        if (arguments != null) {
            start(arguments);
        }
    }

    /**
     * Starts the transformer server.
     *
     * @param arguments contains the port on which the server will listen
     * @throws Exception
     */
    private static void start(Arguments arguments) throws Exception {
        TransformerServer server = new TransformerServer(arguments.getPort(), true);

        // Map for caching pipeline transformers based on the query string
        final Map<String, PipelineTransformer> pipelines = new HashMap<>();

        server.start(
                new TransformerFactory() {
                    @Override
                    public Transformer getTransformer(HttpServletRequest request) {
                        if (StringUtils.isNotEmpty(request.getQueryString())) {
                            String key = request.getQueryString() + request.getHeader("Accept");
                            PipelineTransformer pipelineTransformer = pipelines.get(key);
                            // if pipeline transformer is not found in cache
                            if (pipelineTransformer == null) {
                                // create a new  pipeline transformer
                                pipelineTransformer = new PipelineTransformer(request.getQueryString(), request.getHeader("Accept"));
                                // put the pipeline transformer in the cache
                                pipelines.put(request.getQueryString(), pipelineTransformer);
                            }

                            return pipelineTransformer;
                        } else {
                            throw new TransformerException(HttpServletResponse.SC_BAD_REQUEST, "ERROR: Query string must not be emtpy! \nUsage: http://<pipeline_transformer>/?t=<transformer_1>&...&t=<transformer_N>");
                        }
                    }
                });

        server.join();
    }
}
