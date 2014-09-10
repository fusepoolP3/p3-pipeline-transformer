package eu.fusepoolp3.p3pipelinetrasformer;

import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerFactory;
import eu.fusepool.p3.transformer.server.TransformerServer;
import javax.servlet.http.HttpServletRequest;
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
        
        server.start(
                new TransformerFactory() {
                    @Override
                    public Transformer getTransformer(HttpServletRequest request) {
                        return new PipelineTransformer(request);
                    }
                });

        server.join();
    }
}
