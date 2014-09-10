package eu.fusepoolp3.p3pipelinetrasformer;

import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerFactory;
import eu.fusepool.p3.transformer.server.TransformerServer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
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
        TransformerServer server = new TransformerServer(arguments.getPort());

        // Map for caching pipeline transformers based on the query string
        final Map<String, PipelineTransformer> pipelines = new HashMap<>();

        server.start(
                new TransformerFactory() {
                    @Override
                    public Transformer getTransformer(HttpServletRequest request) {
                        if (StringUtils.isNotEmpty(request.getQueryString())) {
                            PipelineTransformer pipelineTransformer = pipelines.get(request.getQueryString());
                            // if pipeline transformer is not found in cache
                            if(pipelineTransformer == null){
                                // create a new  pipeline transformer
                                pipelineTransformer = new PipelineTransformer(request);
                                // put the pipeline transformer in the cache 
                                pipelines.put(request.getQueryString(), pipelineTransformer);
                            }
                            return pipelineTransformer;
                        }
                        else{
                            throw new RuntimeException("Query string must not be empty!");
                        } 
                    }
                });

        server.join();
    }
}
