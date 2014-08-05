/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepoolp3.p3pipelinetrasformer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.util.HashSet;

/**
 *
 * @author Gabor
 */
public class Transformer {

    public URI uri;
    final public Set<MimeType> supportedInputFormats;
    final public Set<MimeType> supportedOutputFormats;

    public Transformer(URI _uri) {
        uri = _uri;
        supportedInputFormats = new HashSet<>();
        supportedOutputFormats = new HashSet<>();
        setMimeTypes();
    }

    public Transformer(String uriString) {
        try {
            uri = new URI(uriString);
            supportedInputFormats = new HashSet<>();
            supportedOutputFormats = new HashSet<>();
            setMimeTypes();
        } catch (URISyntaxException e) {
            throw new RuntimeException("URI syntax error!", e);
        }
    }

    private void setMimeTypes() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/xml");

            StringBuilder result = new StringBuilder();
            String temp;

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                while ((temp = in.readLine()) != null) {
                    result.append(temp);
                    result.append("\n");
                }
            }

            StringReader stringReader = new StringReader(result.toString());
            Model model = ModelFactory.createDefaultModel();
            model.read(stringReader, null, "TTL");

            // save supported formats
            for (Statement s : model.listStatements().toList()) {
                if (s.asTriple().getPredicate().getLocalName().equals("supportedInputFormat")) {
                    String inputType = s.asTriple().getObject().getLiteralValue().toString();
                    try {
                        supportedInputFormats.add(new MimeType(inputType));
                    } catch (MimeTypeParseException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (s.asTriple().getPredicate().getLocalName().equals("supportedOutputFormat")) {
                    String outputType = s.asTriple().getObject().getLiteralValue().toString();
                    try {
                        supportedOutputFormats.add(new MimeType(outputType));
                    } catch (MimeTypeParseException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Cannot establish connection to " + uri.toString() + " !", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String run(String data, String acceptedMediaTypes) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", acceptedMediaTypes);
            connection.setRequestProperty("charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(data.length()));

            connection.setDoOutput(true);
            connection.setUseCaches(false);

            try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
                out.write(data);
            }

            StringBuilder result = new StringBuilder();
            String temp;

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                while ((temp = in.readLine()) != null) {
                    result.append(temp);
                    result.append("\n");
                }
            }

            return result.toString();

        } catch (IOException e) {
            throw new RuntimeException("Cannot establish connection to " + uri.toString() + " !", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public Boolean isCompatible(Transformer t) {
        for (MimeType m : supportedOutputFormats) {
            for (MimeType m2 : t.supportedInputFormats) {
                if (m.match(m2)) {
                    return true;
                }
            }
        }
        return false;
    }
}
