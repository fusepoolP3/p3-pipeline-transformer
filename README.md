Pipeline transformer [![Build Status](https://travis-ci.org/fusepoolP3/p3-pipeline-transformer.svg)](https://travis-ci.org/fusepoolP3/p3-pipeline-transformer)
====================

The pipeline transformer is a series of connected data transformation elements, called transformers, where the output of one element is the input of the next one. The input of the pipeline transformer is the input of the first transformer in the pipeline, and the output is the output of the last transformer in the pipeline. The pipeline transformer itself does not perform any data transformation on its own.

###Install and run

Clone the repository to your local machine

    git clone https://github.com/fusepoolP3/p3-pipeline-transformer.git

Compile the application with

    mvn clean install

Start the application with

    mvn exec:java

Or start the application with parameters (`-P` sets the port, `-C` enables CORS)

    mvn exec:java -Dexec.args="-P 7100 -C"

###Usage

The pipeline transformer expects the list of transformers URIs in the query string of the request. The pipeline applies the transformers in the same order it was supplied in the request. It is important that the URIs of the transformers must be **URL encoded**, since these can also contain their own query strings.

    t=<transformer1_URI>&t=<transformer2_URI>&...&t=<transformerN_URI>

Note that the pipeline transformer cannot be invoked without supplying at least one transformer in the query string.

    curl -X GET "http://localhost:7100/"
    ERROR: Query string must not be empty!
    Usage: http://<pipeline_transformer>/?t=<transformer_1>&...&t=<transformer_N>

####Validation

The pipeline transformer performs a validation when invoked with a valid query string containing at least two transformers. A pipeline is valid if each transformer accepts at least one of the supported output formats the previous transformer.

If a pipeline is not valid, the transformer supplies the following error message

    curl -X GET "http://localhost:7100/?t=<transformer1_URI>&t=<transformer2_URI>"
    ERROR: Incompatible transformers found in pipeline!
    Reason: Transformer 2. does not accept the any of the supported output formats of transformer 1.

####Get supported formats

The supported input and output formats to a specific pipeline are determined by the first and the last transformer in the pipeline.

Get the supported formats to a pipeline using the command

    curl -X GET "http://localhost:7100/?t=<transformer1_URI>&...&t=<transformerN_URI>"

An example output to this GET request could look like the following

    <http://vocab.fusepool.info/transformer#supportedInputFormat>
              "text/plain"^^<http://www.w3.org/2001/XMLSchema#string> ;
    <http://vocab.fusepool.info/transformer#supportedOutputFormat>
              "text/turtle"^^<http://www.w3.org/2001/XMLSchema#string> ,  "text/rdf+nt"^^<http://www.w3.org/2001/XMLSchema#string> , "text/rdf+n3"^^<http://www.w3.org/2001/XMLSchema#string> .

In this case the first transformer in the pipeline accepts text/plain, and the last transformer produces text/turtle, text/rdf+nt or text/rdf+n3.

####Invoke pipeline with data

To invoke a specific pipeline with data use the following command

    curl -X POST -d <data> "http://localhost:7100/?t=<transformer1_URI>&...&t=<transformerN_URI>"

The output format of the pipeline is determined by the last transformer in the pipeline. If this transformer supports multiple output formats, then the format of the output is randomly chosen. To avoid this set the Accept-Header to the desired output format.

    curl -X POST -H "Accept: text/turtle" -d <data> "http://localhost:7100/?t=<transformer1_URI>&...&t=<transformerN_URI>"

It is also possible to set the Content-Location header when invoking the pipeline transformer, which then will forward this to each transformers in the pipeline.

    curl -X POST -H "Content-Location: http://example.com/document1" -d <data> "http://localhost:7100/?t=<transformer1_URI>&...&t=<transformerN_URI>"
