/*
 */
package gov.osti.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.mvc.Viewable;

/**
 * Allow for an unauthenticated API documenation "home page" of sorts.
 * 
 * @author ensornl
 */
@Path("/")
public class Documentation {
    public Documentation() {
        
    }
    
    /**
     * Obtain a Viewable of the API home base documenation page.
     * @return a Viewable to the root documentation presentation
     */
    @GET
    @Produces (MediaType.TEXT_HTML)
    public Viewable getDocumentation() {
        return new Viewable("/api");
    }
}
