/*
 */
package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.search.SearchData;
import gov.osti.search.SolrDocument;
import gov.osti.search.SolrResult;
import gov.osti.listeners.DoeServletContextListener;
import gov.osti.search.SearchResponse;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement a search interface with a SOLR backend.
 * 
 * @author ensornl
 */
@Path("/search")
public class SearchService {
    @Context ServletContext context;
    
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    
    // Jackson ObjectMapper instance
    protected static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    protected static final ObjectMapper XML_MAPPER = new XmlMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);
    protected static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    
    // configured location of the search service endpoint
    private static String SEARCH_URL = DoeServletContextListener.getConfigurationProperty("search.url");
    
    /**
     * Link to API Documentation template.
     *
     * @return a Viewable API documentation template
     */
    @GET
    @Produces (MediaType.TEXT_HTML)
    public Viewable getDocumentation() {
        return new Viewable("/search");
    }
    
    /**
     * Acquire information from the searching index if possible.  This endpoint
     * should ONLY return Approved records that have been indexed for searching.
     * Requires that searching be configured.
     * 
     * Response Codes:
     * 200 - OK, record found, and returned in desired format
     * 206 - No content, searching is not configured/unavailable
     * 404 - Record was not found
     * 500 - IO error or search malformed
     * 
     * @param codeId the CODE ID to find
     * @param format the desired FORMAT; may be "yaml" or "xml".  Default is JSON
     * unless specified
     * @return the record in the desired format, if found
     */
    @GET
    @Path("{codeId}")
    @Produces ({MediaType.APPLICATION_JSON, "text/yaml", MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getSingleRecord(@PathParam("codeId") Long codeId, @QueryParam("format") String format) {
        // no search configured, you get nothing
        if ("".equals(SEARCH_URL))
            return Response
                    .status(Response.Status.NO_CONTENT)
                    .build();
        CloseableHttpClient hc = HttpClientBuilder
                .create()
                .build();
        
        try {
            // construct a Search for a single CODEID value
            URIBuilder builder = new URIBuilder(SEARCH_URL)
                .addParameter("q", "codeId:" + codeId)
                .addParameter("fl", "json")
                .addParameter("rows", "1");
        
            HttpGet get = new HttpGet(builder.build());

            HttpResponse response = hc.execute(get);

            if (HttpStatus.SC_OK==response.getStatusLine().getStatusCode()) {
                SolrResult result = JSON_MAPPER.readValue(EntityUtils.toString(response.getEntity()), SolrResult.class);

                if (result.getSearchResponse().isEmpty())
                    return ErrorResponse
                            .notFound("No records found.")
                            .build();
                // get the first result
                SolrDocument doc = result.getSearchResponse().getDocuments()[0];
                // convert it to a POJO
                DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(doc.getJson()));

                // if YAML is requested, return that; otherwise, default to JSON
                if ("yaml".equals(format)) {
                    // return the YAML
                    return
                        Response
                        .status(Response.Status.OK)
                        .header("Content-Type", "text/yaml")
                        .header("Content-Disposition", "attachment; filename = \"metadata.yml\"")
                        .entity(YAML_MAPPER.writeValueAsString(md))
                        .build();
                } else if ("xml".equals(format)) {
                    return Response
                            .ok()
                            .header("Content-Type", MediaType.APPLICATION_XML)
                            .entity(XML_MAPPER.writeValueAsString(md))
                            .build();
                } else {
                    // send back the JSON
                    return Response
                        .ok()
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .entity(JSON_MAPPER
                                .createObjectNode()
                                .putPOJO("metadata", md.toJson()).toString())
                        .build();
                }
            } else {
                return ErrorResponse
                        .status(response.getStatusLine().getStatusCode())
                        .message(EntityUtils.toString(response.getEntity()))
                        .build();
            }
        } catch ( IOException | URISyntaxException e ) {
            log.warn("Searching Error.", e);
            return ErrorResponse.internalServerError("Search error encountered.").build();
        }
    }
    
    /**
     * Translate a SearchData parameter request to SOLR output search results.
     * 
     * @param parameters the JSON SearchData Object of search parameters
     * @param format the optional output format (YAML/JSON/XML; JSON is default)
     * @return the output of the SOLR search results, if any
     * @throws IOException on unexpected IO errors
     * @throws URISyntaxException on URI search errors
     */
    @POST
    @Produces ({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "text/yaml"})
    @Consumes (MediaType.APPLICATION_JSON)
    public Response search(String parameters, @QueryParam("format") String format) throws IOException, URISyntaxException {
        // no search configured, you get nothing
        if ("".equals(SEARCH_URL))
            return Response
                    .status(Response.Status.NO_CONTENT)
                    .build();
        
        // get a set of search parameters
        SearchData searchFor = SearchData.parseJson(new StringReader(parameters));
        
        CloseableHttpClient hc = HttpClientBuilder
                .create()
                .build();
        
        URIBuilder builder = new URIBuilder(SEARCH_URL)
                .addParameter("q", searchFor.toQ())
                .addParameter("fl", "json")
                .addParameter("sort", searchFor.getSort());
        // if values are specified for rows and start, supply those.
        if (null!=searchFor.getRows())
            builder.addParameter("rows", String.valueOf(searchFor.getRows()));
        if (null!=searchFor.getStart())
            builder.addParameter("start", String.valueOf(searchFor.getStart()));
        
        HttpGet get = new HttpGet(builder.build());
        
        HttpResponse response = hc.execute(get);
        
        if (HttpStatus.SC_OK==response.getStatusLine().getStatusCode()) {
            SolrResult result = JSON_MAPPER.readValue(EntityUtils.toString(response.getEntity()), SolrResult.class);
            // construct a search response object
            SearchResponse query = new SearchResponse();
            query.setStart(result.getSearchResponse().getStart());
            query.setNumFound(result.getSearchResponse().getNumFound());
            
            // if there are matched documents, load them in
            if ( null!=result.getSearchResponse().getDocuments() ) {
                for ( SolrDocument doc : result.getSearchResponse().getDocuments() ) {
                    query.add(JSON_MAPPER.readValue(doc.getJson(), DOECodeMetadata.class));
                }
            }
            // respond with the appropriate format based on the input parameter
            if ("xml".equals(format)) {
                return Response
                        .ok()
                        .header("Content-Type", MediaType.APPLICATION_XML)
                        .entity(XML_MAPPER.writeValueAsString(query))
                        .build();
            } else if ("yaml".equals(format)) {
                return Response
                        .ok()
                        .header("Content-Type", "text/yaml")
                        .entity(YAML_MAPPER.writeValueAsString(query))
                        .build();
            } else {
                return Response
                        .ok()
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .entity(JSON_MAPPER.writeValueAsString(query))
                        .build();
            }
        } else {
            // let the user know something failed
            return ErrorResponse
                    .status(response.getStatusLine().getStatusCode())
                    .message(EntityUtils.toString(response.getEntity()))
                    .build();
        }
    }
}
