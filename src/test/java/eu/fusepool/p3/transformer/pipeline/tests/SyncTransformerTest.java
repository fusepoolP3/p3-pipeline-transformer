package eu.fusepool.p3.transformer.pipeline.tests;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerFactory;
import eu.fusepool.p3.transformer.pipeline.PipelineTransformer;
import eu.fusepool.p3.transformer.pipeline.tests.sample.SimpleRdfConsumingTransformer;
import eu.fusepool.p3.transformer.pipeline.tests.sample.SimpleRdfProducingTransformer;
import eu.fusepool.p3.transformer.pipeline.tests.sample.SimpleTransformer;
import eu.fusepool.p3.transformer.server.TransformerServer;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import javax.servlet.http.HttpServletRequest;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for Pipeline Transformer. It creates two transformers that have
 * compatible input and output media types and runs them in a pipeline.
 */
public class SyncTransformerTest {

    private String baseURI, transformerURI0, transformerURI1, transformerURI2;

    @Before
    public void setUp() throws Exception {

        // set up test transformer for testing content location
        int port = findFreePort();
        transformerURI0 = "http://localhost:" + port + "/";
        TransformerServer server = new TransformerServer(port);
        server.start(new SimpleTransformer());

        // set up first transformer (SYNC) which consumes text/plain and produces text/turtle
        port = findFreePort();
        transformerURI1 = "http://localhost:" + port + "/";
        server = new TransformerServer(port);
        server.start(new SimpleRdfProducingTransformer());

        // set up second transformer (SYNC) which consumes text/turtle and produces text/turtle
        port = findFreePort();
        transformerURI2 = "http://localhost:" + port + "/";
        server = new TransformerServer(port);
        server.start(new SimpleRdfConsumingTransformer());

        // set up pipeline transformer
        port = findFreePort();
        baseURI = "http://localhost:" + port + "/";
        server = new TransformerServer(port);
        server.start(new TransformerFactory() {
            @Override
            public Transformer getTransformer(HttpServletRequest request) {
                PipelineTransformer pipelineTransformer = new PipelineTransformer(request.getQueryString(), request.getHeader("Accept"));
                return pipelineTransformer;
            }
        });
    }

    @Test
    public void turtleOnGet() throws UnsupportedEncodingException {
        Response response = RestAssured.given().header("Accept", "text/turtle")
                .expect().statusCode(HttpStatus.SC_OK).header("Content-Type", "text/turtle").when()
                .get(baseURI + "?t=" + transformerURI0 + "&t=" + transformerURI1 + "&t=" + transformerURI2);
        Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), "text/turtle");
        System.out.println(graph.toString());
    }

    @Test
    public void turtlePost() {
        Response response = RestAssured.given()
                .header("Accept", "application/x-turtle; q=0.8, application/rdf+xml; q=0.9, text/turtle; q=1.0")
                .header("Content-Location", "http://example.com/test1")
                .contentType("text/plain;charset=UTF-8")
                .content("hello")
                .expect().statusCode(HttpStatus.SC_OK).header("Content-Type", "text/turtle").when()
                .post(baseURI + "?t=" + transformerURI0 + "&t=" + transformerURI1 + "&t=" + transformerURI2);
        Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), "text/turtle");
        Assert.assertTrue("Result doesn't contain originally posted text", graph.toString().contains("hello"));
    }

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
