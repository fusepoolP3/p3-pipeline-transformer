package eu.fusepoolp3.p3pipelinetrasformer;

import eu.fusepool.extractor.server.ExtractorServer;
import org.wymiwyg.commons.util.arguments.ArgumentHandler;

public class Main {

    public static void main(String[] args) throws Exception {
        Arguments arguments = ArgumentHandler.readArguments(Arguments.class, args);
        if (arguments != null) {
            start(arguments);
        }
    }

    private static void start(Arguments arguments) throws Exception {
        ExtractorServer server = new ExtractorServer(arguments.getPort());

        server.start(new PipelineTransformer());

        server.join();
    }
}
