/*
 */
package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import gov.osti.entity.SearchData;
import gov.osti.listeners.DoeServletContextListener;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
    private static ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    
    // configured location of the search service endpoint
    private static String SEARCH_URL = DoeServletContextListener.getConfigurationProperty("search.url");
    
    /**
     * Translate a SearchData parameter request to SOLR output search results.
     * 
     * @param parameters the JSON SearchData Object of search parameters
     * @return the output of the SOLR search results, if any
     * @throws IOException on unexpected IO errors
     * @throws URISyntaxException on URI search errors
     */
    @POST
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_JSON)
    public Response search(String parameters) throws IOException, URISyntaxException {
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
                .addParameter("sort", searchFor.getSort());
        // if values are specified for rows and start, supply those.
        if (null!=searchFor.getRows())
            builder.addParameter("rows", String.valueOf(searchFor.getRows()));
        if (null!=searchFor.getStart())
            builder.addParameter("start", String.valueOf(searchFor.getStart()));
        
        HttpGet get = new HttpGet(builder.build());
        
        HttpResponse response = hc.execute(get);
        
        if (HttpStatus.SC_OK==response.getStatusLine().getStatusCode()) {
            return Response
                    .status(Response.Status.OK)
                    .entity(EntityUtils.toString(response.getEntity()))
                    .build();
        } else {
            return Response
                    .status(response.getStatusLine().getStatusCode())
                    .entity("Search Error: " + EntityUtils.toString(response.getEntity()))
                    .build();
        }
    }
}
