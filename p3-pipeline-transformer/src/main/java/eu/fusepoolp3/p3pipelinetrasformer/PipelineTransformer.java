/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepoolp3.p3pipelinetrasformer;

import eu.fusepool.extractor.HttpRequestEntity;
import eu.fusepool.extractor.RdfGeneratingExtractor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Gabor
 */
public class PipelineTransformer extends RdfGeneratingExtractor {

    @Override
    protected TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {
        final String queryString = entity.getRequest().getQueryString();
        final String data = IOUtils.toString(entity.getData(), "UTF-8");
        final TripleCollection result = new SimpleMGraph();
        final GraphNode node = new GraphNode(new BNode(), result);
        String output;

//        System.out.println(queryString);
//        System.out.println(data);

        HashMap<String, String> queryParams = new HashMap<>();

        if (queryString != null) {
            String[] params = queryString.split("&");
            String[] param;
            for (int i = 0; i < params.length; i++) {
                param = params[i].split("=", 2);
                queryParams.put(param[0], param[1]);
            }
        }

        String uriString = queryParams.get("uri");

        if (uriString == null) {
            throw new RuntimeException("No list of transformers was supplied!");
        }

        Pipeline pipeline = new Pipeline(getSupportedInputFormats(), getSupportedOutputFormats());
        Transformer transformer;
        URI uri;

        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            throw new RuntimeException("URI syntax error!", e);
        }
        
        try (BufferedReader in = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.isEmpty()) {
                    transformer = new Transformer(line);
                    pipeline.addTransformer(transformer);
                }
            }
        }
        
        // validate pipeline
        if(!pipeline.Validate()){
           throw new RuntimeException("Incompatible transformers!"); 
        }
        
        // run pipeline
        if (data != null && !data.isEmpty()) {
            output = pipeline.run(data);
            node.addPropertyValue(SIOC.content, output);
        }                

        return result;
    }

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        try {
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
