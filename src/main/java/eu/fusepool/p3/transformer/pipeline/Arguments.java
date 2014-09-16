package eu.fusepool.p3.transformer.pipeline;

import org.wymiwyg.commons.util.arguments.ArgumentsWithHelp;
import org.wymiwyg.commons.util.arguments.CommandLine;


public interface Arguments extends ArgumentsWithHelp {
    
    @CommandLine(longName = "port", shortName = {"P"}, required = false,
            defaultValue = "7100",
            description = "The port on which the proxy shall listen")
    public int getPort();
    
}
