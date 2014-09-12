package eu.fusepool.p3.transformer.pipeline.tests;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.sample.SimpleTransformer;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;

/**
 * This class extends a SimpleTransformer to have a transformer that can
 * digest turtle as input format (which is the output of a SimpleTransformer),
 * and thus pipelining multiple transformers for testing wouldn't produce 
 * incompatible transformers error.
 *
 * @author Gabor
 */
public class RdfConsumingSimpleTransformer extends SimpleTransformer {

    public static final UriRef TEXUAL_CONTENT = new UriRef("http://example.org/ontology#TextualContent");

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        try {
            MimeType mimeType = new MimeType("text/turtle");
            return Collections.singleton(mimeType);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get SIOC content from the RDF as text and return it.
     * 
     * @param entity
     * @return
     * @throws IOException 
     */
    @Override
    protected TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {
        // 
        String text = "";
        Graph graph = Parser.getInstance().parse(entity.getData(), "text/turtle");
        Iterator<Triple> triples = graph.filter(null, SIOC.content, null);
        if (triples.hasNext()) {
            Literal literal = (Literal) triples.next().getObject();
            text = literal.getLexicalForm();
        }
        
        final TripleCollection result = new SimpleMGraph();
        final Resource resource = entity.getContentLocation() == null
                ? new BNode()
                : new UriRef(entity.getContentLocation().toString());
        final GraphNode node = new GraphNode(resource, result);
        node.addProperty(RDF.type, TEXUAL_CONTENT);
        node.addPropertyValue(SIOC.content, text);
        node.addPropertyValue(new UriRef("http://example.org/ontology#textLength"), text.length());
        return result;
    }

    @Override
    public boolean isLongRunning() {
        return false;
    }
}
