package eu.fusepool.p3.transformer.pipeline.tests.sample;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.RdfGeneratingTransformer;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.commons.io.IOUtils;

/**
 * A simple transformer for junit tests. It expects text/plain and produces text/turtle.
 *
 * @author Gabor
 */
public class SimpleRdfProducingTransformer extends RdfGeneratingTransformer {

    public static final UriRef TEXUAL_CONTENT = new UriRef("http://example.org/ontology#TextualContent");

    @Override
    protected TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {
        final String text = IOUtils.toString(entity.getData(), "UTF-8");
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
    public Set<MimeType> getSupportedInputFormats() {
        try {
            MimeType mimeType = new MimeType("text/plain");
            return Collections.singleton(mimeType);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean isLongRunning() {
        return false;
    }

}
