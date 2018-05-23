/*
 */
package gov.osti.connectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gov.osti.entity.DOECodeMetadata;
import java.io.IOException;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common HTTP-related utilities, such as HttpClient-based web requests for API
 * information.
 * 
 * @author ensornl
 */
public class HttpUtil {
    // logger
    protected static final Logger log = LoggerFactory.getLogger(HttpUtil.class);
    
    // jackson mappers
    protected static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(Include.NON_NULL)
            .setTimeZone(TimeZone.getDefault());
    protected static final ObjectMapper XML_MAPPER = new XmlMapper()
            .setSerializationInclusion(Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setTimeZone(TimeZone.getDefault());
    protected static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setTimeZone(TimeZone.getDefault());
    /**
     * Retrieve just the String content from a given HttpGet request.
     * 
     * @param get the GET to execute
     * @return String contents of the results
     * @throws IOException on IO errors
     */
    protected static String fetch(HttpGet get) throws IOException {
        // create an HTTP client to request through
        CloseableHttpClient hc = 
                HttpClientBuilder
                .create()
                .setDefaultRequestConfig(RequestConfig
                    .custom()
                    .setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000)
                    .setSocketTimeout(5000)
                    .build())
                .build();
        
        try {
            // only return if response is OK
            HttpResponse response = hc.execute(get);
            return ( HttpServletResponse.SC_OK==response.getStatusLine().getStatusCode()) ?
                    EntityUtils.toString(response.getEntity()) :
                    "";
        } finally {
            hc.close();
        }
    }

    /**
     * Attempt to read URL (file) as a YAML metadata reference.  Returns null
     * if YAML file not found or could not process.
     *
     * @param url the URL to the file to attempt to read
     * @return JSON representation of the YAML read, or null if not found/invalid
     * @throws IOException on file IO errors
     */
    protected static JsonNode readMetadataYaml(String url) throws IOException {
        try {
            return readMetadataYaml(new HttpGet(url));
        } catch ( IOException e ) {
            // no YAML or illegal format, skip it
            return null;
        }
    }

    /**
     * Attempt to read URL (file) as a YAML metadata reference.  Returns null
     * if YAML file not found or could not process.
     *
     * @param url the HTTPGET URL to the file to attempt to read
     * @return JSON representation of the YAML read, or null if not found/invalid
     * @throws IOException on file IO errors
     */
    protected static JsonNode readMetadataYaml(HttpGet url) throws IOException {
        try {
            // read the YAML in as a DOECodeMetadata record
            DOECodeMetadata yaml = YAML_MAPPER.readValue(HttpUtil.fetch(url), DOECodeMetadata.class);
            return (null==yaml) ? null : yaml.toJson();
        } catch ( IOException e ) {
            // no YAML or illegal format, skip it
            return null;
        }
    }

    /**
     * Write the Metadata in YAML format.
     * @param in the DOECodeMetadata to write
     * @return YAML format for the metadata
     * @throws IOException on write errors
     */
    public static String writeMetadataYaml(DOECodeMetadata in) throws IOException {
        return YAML_MAPPER.writeValueAsString(in);
    }
    
    /**
     * Write a JsonNode Object in YAML format.
     * @param json the JsonNode to write
     * @return YAML formatted output
     * @throws IOException on write errors
     */
    public static String writeMetadataYaml(JsonNode json) throws IOException {
        return YAML_MAPPER.writeValueAsString(json);
    }
    
    /**
     * Write the DOECodeMetadata Object in XML format.
     * @param in the DOECodeMetadata Object
     * @return XML output
     * @throws IOException on write errors
     */
    public static String writeXml(DOECodeMetadata in) throws IOException {
        return XML_MAPPER
                .writer()
                .withRootName("metadata")
                .writeValueAsString(in);
    }
    
    /**
     * Write the DOECodeMetadata Object in JSON format as a String.
     * @param in the DOECodeMetadata Object to write
     * @return a JSON String
     * @throws IOException on write errors
     */
    public static String writeJson(DOECodeMetadata in) throws IOException {
        return JSON_MAPPER.writeValueAsString(in);
    }
}
