package gov.osti.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.mvc.Viewable;

/**
 * Allow for an unauthenticated API documentation "home page" of sorts.
 *
 * @author ensornl
 */
@Path("/docs")
public class Documentation {

    public Documentation() {

    }

    /**
     * Obtain a Viewable of the API home base documentation page.
     *
     * @return a Viewable to the root documentation presentation
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getDocumentation() {
        return new Viewable("/api");
    }

    /**
     * Link to API Metadata Documentation template.
     *
     * @return a Viewable API documentation template
     */
    @GET
    @Path("/metadata")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getMetadataDocumentation() {
        return new Viewable("/metadata");
    }

    /**
     * View the API User documentation.
     *
     * @return a Viewable to the documentation
     */
    @GET
    @Path("/user")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getUserDocumentation() {
        return new Viewable("/userservices");
    }

    /**
     * View the API Site documentation.
     *
     * @return a Viewable to the documentation
     */
    @GET
    @Path("/site")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getSiteDocumentation() {
        return new Viewable("/siteservices");
    }

    /**
     * Link to API Search Documentation template.
     *
     * @return a Viewable API documentation template
     */
    @GET
    @Path("/search")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getSearchDocumentation() {
        return new Viewable("/search");
    }

    /**
     * Link to API Types Documentation template.
     *
     * @return a Viewable API documentation template
     */
    @GET
    @Path("/types")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getTypesDocumentation() {
        return new Viewable("/types");
    }

    /**
     * View the API Validation documentation.
     *
     * @return a Viewable to the documentation
     */
    @GET
    @Path("/validation")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getValidationDocumentation() {
        return new Viewable("/validation");
    }
}
