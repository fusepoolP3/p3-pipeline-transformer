package eu.fusepool.p3.transformer.pipeline.tests;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerFactory;
import eu.fusepool.p3.transformer.pipeline.PipelineTransformer;
import eu.fusepool.p3.transformer.sample.LongRunningTransformer;
import eu.fusepool.p3.transformer.server.TransformerServer;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for Pipeline Transformer. It creates two transformers that have
 * compatible input and output media types and runs them in a pipeline.
 */
public class LongRunningTransformerTest {

    private String baseURI, transformerURI1, transformerURI2;
    /*
    @Before
    public void setUp() throws Exception {
        // set up first transformer (LONGRUNNING) which consumes text/plain and produces text/turtle
        int port = findFreePort();
        transformerURI1 = "http://localhost:" + port + "/";
        TransformerServer server = new TransformerServer(port);
        server.start(new LongRunningTransformer());

        // set up second transformer (SYNC) which consumes text/turtle and produces text/turtle
        port = findFreePort();
        transformerURI2 = "http://localhost:" + port + "/";
        server = new TransformerServer(port);
        server.start(new RdfConsumingSimpleTransformer());

        // set up pipeline transformer
        port = findFreePort();
        baseURI = "http://localhost:" + port + "/";
        server = new TransformerServer(port);
        server.start(new TransformerFactory() {
            @Override
            public Transformer getTransformer(HttpServletRequest request) {
                return new PipelineTransformer(request.getQueryString());
            }
        });
    }

    @Test
    public void turtleOnGet() throws UnsupportedEncodingException {
        Response response = RestAssured.given().header("Accept", "text/turtle")
                .expect().statusCode(HttpStatus.SC_OK).header("Content-Type", "text/turtle").when()
                .get(baseURI + "?t1=" + transformerURI1 + "&t2=" + transformerURI2);
    }

    @Test
    public void turtlePost() {
        Response response = RestAssured.given().header("Accept", "text/turtle")
                .contentType("text/plain;charset=UTF-8")
                .content("hello")
                .expect().statusCode(HttpStatus.SC_OK).header("Content-Type", "text/turtle").when()
                .post(baseURI + "?t1=" + transformerURI1 + "&t2=" + transformerURI2);   
        Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), "text/turtle");
        Iterator<Triple> typeTriples = graph.filter(null, RDF.type,
                new UriRef("http://example.org/ontology#TextualContent"));
        Assert.assertTrue("No type triple found", typeTriples.hasNext());
        Resource textDescription = typeTriples.next().getSubject();
        Assert.assertTrue("TextDescription resource is not a BNode", textDescription instanceof BNode);
    }
    */
    public static int findFreePort() {
        int port = 0;
        try (ServerSocket server = new ServerSocket(0);) {
            port = server.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("unable to find a free port");
        }
        return port;
    }
}
