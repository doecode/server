/*
 */
package gov.osti.services;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/**
 * REST Web Service
 *
 * @author ensornl
 */
@Path("validation")
public class Validation {

    @Context ServletContext context;
    @Context HttpServletRequest request;
    
    /**
     * Creates a new instance of ValidationResource
     */
    public Validation() {
    }

    /**
     * Determine whether or not a contract number is valid.
     * 
     * @param contractNo the contract number to validate
     * @return JSON containing "isValid" boolean to determine whether or not this
     * contract number is valid
     * @throws java.io.IOException on IO or HTTP client errors
     */
    @GET
    @Path("/contract/{contractno}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateContractNumber(@PathParam ("contractno") String contractNo) throws IOException {
        String apiHost = context.getInitParameter("api.host");
        
        if ( null==apiHost ) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // set some reasonable default timeouts
        RequestConfig rc = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000).build();
        // create an HTTP client to request through
        CloseableHttpClient hc = 
                HttpClientBuilder
                .create()
                .setDefaultRequestConfig(rc)
                .build();
        
        HttpGet get = new HttpGet(apiHost + "/api/contract/validate/" + contractNo);
        try {
            HttpResponse response = hc.execute(get);
            return Response.ok().entity(EntityUtils.toString(response.getEntity())).build();
        } finally {
            hc.close();
        }
    }
}
