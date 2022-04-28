package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
import java.util.ArrayList;
import java.util.TimeZone;

import javax.persistence.TypedQuery;
import javax.persistence.NoResultException;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.commons.beanutils.BeanUtilsBean;

@Path("site")
public class SiteServices {

    private static final Logger log = LoggerFactory.getLogger(SiteServices.class);

    // ObjectMapper instance for metadata interchange
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setTimeZone(TimeZone.getDefault());

    public SiteServices() {
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
    @RequiresRoles("SiteAdmin")
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
    @RequiresRoles("SiteAdmin")
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
     * Query to get SOFTWARE GROUP EMAIL info.
     *
     * Response Codes:
     * 200 - OK, JSON contains SITE CODE, SOFTWARE GROUP EMAIL
     * 500 - Internal service error
     *
     * @return Array of JSON; empty array if not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public Response getSpecificSiteInfo() {
        EntityManager em = DoeServletContextListener.createEntityManager();
        ArrayNode return_data = mapper.createArrayNode();

        try {

            TypedQuery<Site> query = em.createNamedQuery("Site.findWithSoftwareGroupEmail", Site.class);

            // look up the Sites
            try {
                List<Site> sites = query.getResultList();

                for (Site site:sites) {
                    ObjectNode sub_data = mapper.createObjectNode();
                    sub_data.put("site_code", site.getSiteCode());
                    sub_data.put("software_group_email", site.getSoftwareGroupEmail());
                    return_data.add(sub_data);
                }                
        
            } catch (Exception e) {
                return ErrorResponse
                        .badRequest("Error while gathering Site Software Group Email info.")
                        .build();
            }

            // return the results back
            return Response
                    .ok()
                    .entity(mapper.writeValueAsString(return_data))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Software Group Email Site Lookup Error", e);
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }

    /**
     * Processes Site edits to a siteCode. May contain array of SITE EDIT REQUESTs.
     *
     * Response Codes:
     *
     * 200 - OK, Data changed
     * 400 - Bad request, no siteCode sent or is invalid
     * 500 - Unable to parse request or internal system error
     *
     * @param input the JSON containing the SITE EDIT REQUEST information
     * @return an OK Response if everything fine, or exception otherwise
     */
    @POST
    @RequiresAuthentication
    @RequiresRoles("SiteAdmin")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/edit")
    public Response editSite(String input) {

        EntityManager em = DoeServletContextListener.createEntityManager();
        Site[] requests;
        try {
            requests = mapper.readValue(input, Site[].class);
        } catch (IOException e) {
            log.error("Error in register: ", e);
            return ErrorResponse
                    .internalServerError("Error processing request. [" + e.getMessage() + "]")
                    .build();
        }

        // nothing to do?
        if (requests == null || requests.length == 0) {
            return ErrorResponse
                    .badRequest("Required information missing.")
                    .build();
        }

        List<Site> editedSites = new ArrayList<>();
        try {
            List<String> errors = new ArrayList<>();

            em.getTransaction().begin();

            // update the values
            Site site = null;
            for (int i = 0; i < requests.length; i++) {
                Site req = requests[i];
                String siteCode = req.getSiteCode();
                Boolean isStandard = req.isStandardUsage();

                TypedQuery<Site> query = em.createNamedQuery("Site.findBySiteCode", Site.class)
                        .setParameter("site", siteCode);

                try{
                    site = query.getSingleResult();
                } catch(NoResultException e) {
                    site = null;
                }

                // if there's not already a Site on file, cannot edit
                if ( site == null ) {
                    errors.add("A Site of ["+ siteCode +"] does not exist.");

                    continue;
                }

                try{
                    // found it, "merge" Bean attributes
                    BeanUtilsBean noNulls = new NoNullsBeanUtilsBean();
                    noNulls.copyProperties(site, req);

                    editedSites.add(site);
                } catch (Exception e) {
                    errors.add("site_code: '" + siteCode + "' update failed: " + e.getMessage());
                }

                errors.addAll(validateSite(site));

                // persist only if no errors
                if ( errors.isEmpty() ) {
                    em.merge(site);
                }

            }

            // return
            if ( errors.isEmpty() ) {
                em.getTransaction().commit();

                // no errors
                return Response
                        .ok()
                        .entity(mapper.valueToTree(editedSites).toString())
                        .build();
            }
            else {
                em.getTransaction().rollback();
                
                // errors, return any error message
                return ErrorResponse
                        .badRequest(errors)
                        .build();
            }
        } catch (IllegalArgumentException e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();

            log.error("Persistence Error Updating Site info", e);
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();
        }

    }

    /**
     * Processes Site additions for a siteCode. May contain array of SITE EDIT REQUESTs.
     *
     * Response Codes:
     *
     * 200 - OK, Data to add
     * 400 - Bad request, no siteCode sent or is invalid
     * 500 - Unable to parse request or internal system error
     *
     * @param input the JSON containing the SITE EDIT REQUEST information
     * @return an OK Response if everything fine, or exception otherwise
     */
    @PUT
    @RequiresAuthentication
    @RequiresRoles("SiteAdmin")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/new")
    public Response addSite(String input) {

        EntityManager em = DoeServletContextListener.createEntityManager();
        Site[] requests;
        try {
            requests = mapper.readValue(input, Site[].class);
        } catch (IOException e) {
            log.error("Error in register: ", e);
            return ErrorResponse
                    .internalServerError("Error processing request. [" + e.getMessage() + "]")
                    .build();
        }

        // nothing to do?
        if (requests == null || requests.length == 0) {
            return ErrorResponse
                    .badRequest("Required information missing.")
                    .build();
        }

        try {
            List<String> errors = new ArrayList<>();

            em.getTransaction().begin();

            // add the values
            Site site;
            //for (Site req : requests) {
            for (int i = 0; i < requests.length; i++) {
                Site req = requests[i];
                String siteCode = req.getSiteCode();

                // confirm not exists
                TypedQuery<Site> query = em.createNamedQuery("Site.findBySiteCode", Site.class)
                    .setParameter("site", siteCode);

                try{
                    site = query.getSingleResult();
                } catch(NoResultException e) {
                    site = null;
                }

                // if there's already a Site on file, cannot re-add
                if ( site != null ) {
                    errors.add("A Site of ["+ siteCode +"] already exists.");

                    continue;
                }

                // validate
                errors.addAll(validateSite(req));

                // persist only if no errors
                if ( errors.isEmpty() ) {
                    em.persist(req);
                }

            }

            // return
            if ( errors.isEmpty() ) {
                em.getTransaction().commit();

                // no errors
                return Response
                        .ok()
                        .entity(mapper.valueToTree(requests).toString())
                        .build();
            }
            else {
                em.getTransaction().rollback();
                
                // errors, return any error message
                return ErrorResponse
                        .badRequest(errors)
                        .build();
            }

        } catch (IllegalArgumentException e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();

            log.error("Persistence Error Adding Site info", e);
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }

    /**
     * Perform validations for SITE records.
     *
     * @param s the Site information to validate
     * @return a List of error messages if any validation errors, empty if none
     */
    protected static List<String> validateSite(Site s) {
        List<String> failures = new ArrayList<>();

        // allow overwrites and defaults
        if (StringUtils.isBlank(s.getSoftwareGroupEmail())) {
            s.setSoftwareGroupEmail(null);
        }
        if (s.isStandardUsage() == null) {
            s.setStandardUsage(false);
        }
        if (s.isHqUsage() == null) {
            s.setHqUsage(false);
        }

        String siteCode = s.getSiteCode();
        String labName = s.getLabName();
        List<String> emails = s.getPocEmails();
        List<String> emailDomains = s.getEmailDomains();
        String softwareGroupEmail = s.getSoftwareGroupEmail();
        boolean isStandard = s.isStandardUsage();

        try {
            // site must be a valid one
            if (siteCode == null || StringUtils.isBlank(siteCode)) {
                failures.add("Site Code is required.");
            }

            if (labName == null || StringUtils.isBlank(labName)) {
                // Lab Name must be provided
                failures.add("Lab Name not provided for Site ["+ siteCode +"].");
            }

            if (softwareGroupEmail != null) {
                // email must be a valid one
                if (!Validation.isValidEmail(softwareGroupEmail)) {
                    failures.add("Invalid Software Group Email address ["+ softwareGroupEmail +"] for Site ["+ siteCode +"].");
                }
            }

            if (isStandard || (!isStandard && siteCode.equals("HQ"))) {
                if (emailDomains == null || emailDomains.size() == 0) {
                    failures.add("At least one Email Domain required for Site ["+ siteCode +"].");
                }
                else {
                    for (String domain : emailDomains) {
                        // email must be a valid one
                        if (domain == null || !domain.startsWith("@") || !Validation.isValidEmail("xxxx" + domain)) {
                            failures.add("Invalid Email Domain ["+ domain +"] for Site ["+ siteCode +"].");
                        }
                    }
                }
            }
            else {
                if (emailDomains != null && emailDomains.size() > 0) {
                    failures.add("For Site ["+ siteCode +"], Email Domain is only allowed for site 'HQ' and standard usage sites.");
                }
            }

            if (emails != null) {
                for (String email : emails) {
                    // email must be a valid one
                    if (!Validation.isValidEmail(email)) {
                        failures.add("Invalid POC Email address ["+ email +"] for Site ["+ siteCode +"].");
                    }
                }
            }
        } catch (Exception e) {
            failures.add("site_code: '" + siteCode + "' validation failed: " + e.getMessage());
        }

        return failures;
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
