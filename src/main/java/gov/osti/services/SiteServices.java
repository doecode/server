package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import gov.osti.entity.Site;

import gov.osti.listeners.DoeServletContextListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.persistence.TypedQuery;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.glassfish.jersey.server.mvc.Viewable;

@Path("site")
public class SiteServices {

    private static final Logger log = LoggerFactory.getLogger(SiteServices.class);

    // ObjectMapper instance for metadata interchange
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public SiteServices() {
    }

    /**
     * View the API documentation.
     *
     * @return a Viewable to the documentation
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getDocumentation() {
        return new Viewable("/siteservices");
    }

    /**
     * Query to get all POINTS OF CONTACT.
     *
     * Response Codes:
     * 200 - OK, JSON contains SITE CODE, EMAIL DOMAINS, POC_EMAILS, and LAB
     * 500 - Internal service error
     *
     * @return a Response containing the JSON if found
     */
    @GET
    @RequiresAuthentication
    @RequiresRoles("OSTI")
    @Consumes({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/info")
    public Response getAllSiteInfo() {
        EntityManager em = DoeServletContextListener.createEntityManager();

        try {

            TypedQuery<Site> query = em.createNamedQuery("Site.findAll", Site.class);

            List<Site> sites = query.getResultList();

            // return the results back
            return Response
                    .ok()
                    .entity(mapper.writeValueAsString(sites))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("POC Site Lookup Error", e);
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }

    /**
     * Query to get POINTS OF CONTACT based on SITE CODE value.
     *
     * Response Codes:
     * 200 - OK, JSON contains SITE CODE, EMAIL DOMAINS, POC_EMAILS, and LAB
     * 500 - Internal service error
     *
     * @param siteCode the SITE CODE for which to gather POC_EMAILS
     * @return Array of SITE as JSON; empty array if not found
     */
    @GET
    @RequiresAuthentication
    @RequiresRoles("OSTI")
    @Consumes({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/info/{site}")
    public Response getSpecificSiteInfo(@PathParam("site") String siteCode) {
        EntityManager em = DoeServletContextListener.createEntityManager();

        try {

            TypedQuery<Site> query = em.createNamedQuery("Site.findBySiteCode", Site.class)
                    .setParameter("site", siteCode);

            // look up the Site, set emails
            Site site;
            try {
                site = query.getSingleResult();
            } catch (Exception e) {
                return ErrorResponse
                        .badRequest("site_code: '" + siteCode + "' is invalid.")
                        .build();
            }

            // return the results back
            return Response
                    .ok()
                    .entity(mapper.writeValueAsString(site))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("POC Site Lookup Error", e);
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }

    /**
     * Processes POC edits to a siteCode. May contain array of SITE_CODE and POC_EMAILS.
     *
     * Response Codes:
     *
     * 200 - OK, POCs changed
     * 400 - Bad request, no siteCode sent or is invalid
     * 500 - Unable to parse request or internal system error
     *
     * @param input the JSON containing the SITE_CODE and POC_EMAILS information
     * @return an OK Response if everything fine, or exception otherwise
     */
    @POST
    @RequiresAuthentication
    @RequiresRoles("OSTI")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/update")
    public Response editSite(String input) {

        EntityManager em = DoeServletContextListener.createEntityManager();
        POCRequest[] requests;
        try {
            requests = mapper.readValue(input, POCRequest[].class);
        } catch (IOException e) {
            log.error("Error in register: ", e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, "Error processing request. [" + e.getMessage() + "]")
                    .build();
        }

        // nothing to do?
        if (requests == null || requests.length == 0)
            return ErrorResponse
                    .badRequest("Required information missing.")
                    .build();

        try {
            em.getTransaction().begin();

            // update the values
            for (POCRequest req : requests) {
                List<String> emails = req.getPocEmails();
                String siteCode = req.getSiteCode();

                TypedQuery<Site> query = em.createNamedQuery("Site.findBySiteCode", Site.class)
                        .setParameter("site", siteCode);

                // look up the Site, set emails
                Site site;
                try {
                    site = query.getSingleResult();
                } catch (Exception e) {
                    return ErrorResponse
                            .badRequest("site_code: '" + siteCode + "' is invalid.")
                            .build();
                }

                em.merge(site);
            }

            em.getTransaction().commit();

            // at the end, return any error message
            return Response
                    .ok()
                    .entity(mapper.valueToTree(requests).toString())
                    .build();
        } catch (IllegalArgumentException e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();

            log.error("Persistence Error Updating POC Emails", e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }

    /**
     * POCRequest Request -- for POC update requests.
     *
     * siteCode - desired siteCode to alter
     * pocEmails - array of pocEmails to associate to the siteCode
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class POCRequest implements Serializable {

        private String siteCode;
        private List<String> pocEmails;

        public POCRequest() {
        }

        /**
         * @return the siteCode
         */
        public String getSiteCode() {
            return siteCode;
        }

        /**
         * @param site the siteCode to set
         */
        public void setSiteCode(String site) {
            this.siteCode = site;
        }

        /**
         * @return the pocEmails
         */
        public List<String> getPocEmails() {
            return pocEmails;
        }

        /**
         * @param emails associated to a siteCode
         */
        public void setPocEmails(List<String> pocEmails) {
            for (int i = 0; i < pocEmails.size(); i++)
                pocEmails.set(i, pocEmails.get(i).toLowerCase());

            this.pocEmails = pocEmails;
        }

    }

    /**
     * Locate a Site record by SITE_CODE.
     *
     * @param siteCode the SITE_CODE to look for
     * @return a Site object if possible or null if not found or errors
     */
    protected static Site findSiteBySiteCode(String siteCode) {
        EntityManager em = DoeServletContextListener.createEntityManager();

        try {
            return em.find(Site.class, siteCode);
        } catch (Exception e) {
            log.warn("Error locating site : " + siteCode, e);
            return null;
        } finally {
            em.close();
        }
    }
}
