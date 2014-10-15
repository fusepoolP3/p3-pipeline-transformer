package eu.fusepool.p3.transformer.pipeline.tests;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.SyncTransformer;
import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerFactory;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.WritingEntity;
import eu.fusepool.p3.transformer.pipeline.PipelineTransformer;
import eu.fusepool.p3.transformer.sample.SimpleTransformer;
import eu.fusepool.p3.transformer.server.TransformerServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.io.IOUtils;
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
        server.start(new TestContentLocationTransformer());
        
        // set up first transformer (SYNC) which consumes text/plain and produces text/turtle
        port = findFreePort();
        transformerURI1 = "http://localhost:" + port + "/";
        server = new TransformerServer(port);
        server.start(new SimpleTransformer());

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
                PipelineTransformer pipelineTransformer = new PipelineTransformer(request.getQueryString());
                pipelineTransformer.setAcceptHeader(request.getHeader("Accept"));
                return pipelineTransformer;
            }
        });
    }
    
    @Test
    public void turtleOnGet() throws UnsupportedEncodingException {
        Response response = RestAssured.given().header("Accept", "text/turtle")
                .expect().statusCode(HttpStatus.SC_OK).header("Content-Type", "text/turtle").when()
                .get(baseURI + "?t1=" + transformerURI0 + "&t2=" + transformerURI1 + "&t3=" + transformerURI2);
    }
    
    @Test
    public void turtlePost() {
        Response response = RestAssured.given()
                .header("Accept", "text/turtle")
                .header("Content-Location", "http://example.com/resources/content")
                .contentType("text/plain;charset=UTF-8")
                .content("hello")
                .expect().statusCode(HttpStatus.SC_OK).header("Content-Type", "text/turtle").when()
                .post(baseURI + "?t1=" + transformerURI0 + "&t2=" + transformerURI1 + "&t3=" + transformerURI2);
        Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), "text/turtle");
        Iterator<Triple> typeTriples = graph.filter(null, RDF.type,
                new UriRef("http://example.org/ontology#TextualContent"));
        Assert.assertTrue("No type triple found", typeTriples.hasNext());
        Resource textDescription = typeTriples.next().getSubject();
        Assert.assertTrue("TextDescription resource is not a BNode", textDescription instanceof BNode);
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
    
    class TestContentLocationTransformer implements SyncTransformer {
        
        @Override
        public Entity transform(HttpRequestEntity entity) throws IOException {
            // get mimetype of content
            final MimeType mimeType = entity.getType();

            // get inputstream
            final InputStream in = entity.getData();

            // get content location header
            final URI contentLocation = entity.getContentLocation();
            
            System.out.println("Test Transformer Input Content-Location: " + contentLocation);
            
            final Entity output = new WritingEntity() {
                @Override
                public MimeType getType() {
                    return mimeType;
                }
                
                @Override
                public void writeData(OutputStream out) throws IOException {
                    IOUtils.copy(in, out);
                }
                
                @Override
                public URI getContentLocation() {
                    return contentLocation;
                }
            };
            
            System.out.println("Test Transformer Output Content-Location: " + output.getContentLocation());
            
            return output;
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
        public Set<MimeType> getSupportedOutputFormats() {
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
}
