/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepoolp3.p3pipelinetrasformer;

import org.wymiwyg.commons.util.arguments.ArgumentsWithHelp;
import org.wymiwyg.commons.util.arguments.CommandLine;


public interface Arguments extends ArgumentsWithHelp {
    
    @CommandLine(longName = "port", shortName = {"P"}, required = false,
            defaultValue = "7101",
            description = "The port on which the proxy shall listen")
    public int getPort();
    
}
