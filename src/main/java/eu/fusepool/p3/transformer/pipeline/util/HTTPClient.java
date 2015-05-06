package eu.fusepool.p3.transformer.pipeline.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;

/**
 *
 * @author Gabor
 */
public class HTTPClient {

    final static UriRef LIST = new UriRef("http://schema.example.org/list");
    final static UriRef FIRST = new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#first");
    final static UriRef REST = new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");

    /**
     * Retrieves a resource from the supplied URI.
     *
     * @param uri
     * @return
     */
    public static List<String> getTransformers(String uri) {
        HttpURLConnection httpConnection = null;
        try {
            URL url = new URL(uri);
            // open connection
            httpConnection = (HttpURLConnection) url.openConnection();
            // set method to GET
            httpConnection.setRequestMethod("GET");
            // set accept, content-type to turtle
            httpConnection.setRequestProperty("Accept", "text/turtle");
            httpConnection.setRequestProperty("Content-Type", "text/turtle");
            // set timeout to 30 sec
            httpConnection.setReadTimeout(30 * 1000);
            // connect
            httpConnection.connect();
            // get the response
            InputStream resource = httpConnection.getInputStream();
            // parse resource and get transformer URIs
            return parseTurtle(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    /**
     * Get the list of transformer URIs from input graph.
     *
     * @param data
     * @return
     */
    private static List<String> parseTurtle(InputStream data) {
        final List<String> transformerURIs = new ArrayList<>();
        final MGraph graph = new IndexedMGraph();
        // parse inputstream as turtle
        Parser.getInstance().parse(graph, data, "text/turtle");

        final Iterator<Triple> listTriple = graph.filter(null, LIST, null);

        if (!listTriple.hasNext()) {
            throw new RuntimeException("ERROR: No triple found with " + LIST.getUnicodeString());
        } else {
            UriRef uriRef;
            Literal literal;
            Triple triple = listTriple.next();
            NonLiteral object = (NonLiteral) triple.getObject();

            // find the first list item
            Iterator<Triple> firstTriple = graph.filter(object, FIRST, null);
            Iterator<Triple> restTriple = graph.filter(object, REST, null);

            // loop through the linked list
            while (firstTriple.hasNext() && restTriple.hasNext()) {
                // get first triple
                triple = firstTriple.next();
                // if object is an UriRef
                if (triple.getObject() instanceof UriRef) {
                    // get object of triple as UriRef
                    uriRef = (UriRef) triple.getObject();
                    // add literal to transformer URI list
                    transformerURIs.add(uriRef.getUnicodeString());
                } else {
                    // get object of triple as literal
                    literal = (Literal) triple.getObject();
                    // add literal to transformer URI list
                    transformerURIs.add(literal.getLexicalForm());
                }

                // get rest triple
                triple = restTriple.next();
                // get object of rest triple as non literal
                object = (NonLiteral) triple.getObject();
                // find next list item
                firstTriple = graph.filter(object, FIRST, null);
                restTriple = graph.filter(object, REST, null);
            }
        }
        return transformerURIs;
    }
}
