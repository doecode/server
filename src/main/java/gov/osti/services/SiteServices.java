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
        SiteEditRequest[] requests;
        try {
            requests = mapper.readValue(input, SiteEditRequest[].class);
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



            // validate values
            for (SiteEditRequest req : requests) {
                List<String> emails = req.getPocEmails();
                List<String> emailDomains = req.getEmailDomains();
                String softwareGroupEmail = req.getSoftwareGroupEmail();
                String siteCode = req.getSiteCode();

                TypedQuery<Site> query = em.createNamedQuery("Site.findBySiteCode", Site.class)
                        .setParameter("site", siteCode);

                // look up the Site
                Site site;
                try {
                    // site must be a valid one
                    if (siteCode == null || StringUtils.isBlank(siteCode))
                    return ErrorResponse
                            .badRequest("Site Code is required.")
                            .build();

                    try{
                        site = query.getSingleResult();
                    } catch(NoResultException e) {
                        site = null;
                    }

                    // if there's not a Site on file, cannot edit
                    if ( site == null ) {
                        return ErrorResponse
                                .status(Response.Status.BAD_REQUEST, "A Site with code ["+ siteCode +"] does not exists.")
                                .build();
                    }
        
                    if (softwareGroupEmail != null) {
                        // email must be a valid one
                        if (!Validation.isValidEmail(softwareGroupEmail))
                        return ErrorResponse
                                .badRequest("Invalid Software Group Email address ["+ softwareGroupEmail +"] for Site ["+ siteCode +"].")
                                .build();
                    }

                    if (emailDomains == null || emailDomains.size() == 0) {
                        return ErrorResponse
                                .badRequest("At least one Email Domain required for Site ["+ siteCode +"].")
                                .build();
                    }

                    if (emails != null) {
                        for (String email : emails) {
                            // email must be a valid one
                            if (!Validation.isValidEmail(email))
                            return ErrorResponse
                                    .badRequest("Invalid POC Email address ["+ email +"] for Site ["+ siteCode +"].")
                                    .build();
                        }
                    }

                    for (String domain : emailDomains) {
                        // email must be a valid one
                        if (!Validation.isValidEmail("xxxx" + domain))
                        return ErrorResponse
                                .badRequest("Invalid Email Domain ["+ domain +"] for Site ["+ siteCode +"].")
                                .build();
                    }
                } catch (Exception e) {
                    return ErrorResponse
                            .badRequest("site_code: '" + siteCode + "' validation failed: " + e.getMessage())
                            .build();
                }
            }

        try {
            em.getTransaction().begin();

            // update the values
            for (SiteEditRequest req : requests) {
                List<String> emails = req.getPocEmails();
                String siteCode = req.getSiteCode();

                TypedQuery<Site> query = em.createNamedQuery("Site.findBySiteCode", Site.class)
                        .setParameter("site", siteCode);

                // look up the Site, set emails
                Site site;
                try {
                    site = query.getSingleResult();
                    site.setStandardUsage(req.isStandardUsage());
                    site.setHqUsage(req.isHqUsage());
                    site.setLab(req.getLabName());
                    site.setPocEmails(req.getPocEmails());
                    site.setEmailDomains(req.getEmailDomains());
                    site.setSoftwareGroupEmail(req.getSoftwareGroupEmail());
                } catch (Exception e) {
                    return ErrorResponse
                            .badRequest("site_code: '" + siteCode + "' update failed: " + e.getMessage())
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

            log.error("Persistence Error Updating Site info", e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
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
        SiteEditRequest[] requests;
        try {
            requests = mapper.readValue(input, SiteEditRequest[].class);
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



            // validate values
            for (SiteEditRequest req : requests) {
                List<String> emails = req.getPocEmails();
                List<String> emailDomains = req.getEmailDomains();
                String softwareGroupEmail = req.getSoftwareGroupEmail();
                String siteCode = req.getSiteCode();

                TypedQuery<Site> query = em.createNamedQuery("Site.findBySiteCode", Site.class)
                        .setParameter("site", siteCode);

                // look up the Site
                Site site;
                try {
                    // site must be a valid one
                    if (siteCode == null || StringUtils.isBlank(siteCode))
                    return ErrorResponse
                            .badRequest("Site Code is required.")
                            .build();

                    try{
                        site = query.getSingleResult();
                    } catch(NoResultException e) {
                        site = null;
                    }

                    // if there's already a Site on file, cannot re-add
                    if ( site != null ) {
                        return ErrorResponse
                                .status(Response.Status.BAD_REQUEST, "A Site with code ["+ siteCode +"] already exists.")
                                .build();
                    }
        
                    if (softwareGroupEmail != null) {
                        // email must be a valid one
                        if (!Validation.isValidEmail(softwareGroupEmail))
                        return ErrorResponse
                                .badRequest("Invalid Software Group Email address ["+ softwareGroupEmail +"] for Site ["+ siteCode +"].")
                                .build();
                    }

                    if (emailDomains == null || emailDomains.size() == 0) {
                        return ErrorResponse
                                .badRequest("At least one Email Domain required for Site ["+ siteCode +"].")
                                .build();
                    }

                    if (emails != null) {
                        for (String email : emails) {
                            // email must be a valid one
                            if (!Validation.isValidEmail(email))
                            return ErrorResponse
                                    .badRequest("Invalid POC Email address ["+ email +"] for Site ["+ siteCode +"].")
                                    .build();
                        }
                    }

                    for (String domain : emailDomains) {
                        // email must be a valid one
                        if (!Validation.isValidEmail("xxxx" + domain))
                        return ErrorResponse
                                .badRequest("Invalid Email Domain ["+ domain +"] for Site ["+ siteCode +"].")
                                .build();
                    }
                } catch (Exception e) {
                    return ErrorResponse
                            .badRequest("site_code: '" + siteCode + "' validation failed: " + e.getMessage())
                            .build();
                }
            }

        try {
            // add the values
            for (SiteEditRequest req : requests) {
                em.getTransaction().begin();
                String siteCode = req.getSiteCode();

                // look up the Site, set emails
                Site site;
                try {
                    site = new Site();
                    site.setSiteCode(req.getSiteCode());
                    site.setStandardUsage(req.isStandardUsage());
                    site.setHqUsage(req.isHqUsage());
                    site.setLab(req.getLabName());
                    site.setPocEmails(req.getPocEmails());
                    site.setEmailDomains(req.getEmailDomains());
                    site.setSoftwareGroupEmail(req.getSoftwareGroupEmail());
                } catch (Exception e) {
                    return ErrorResponse
                            .badRequest("site_code: '" + siteCode + "' addition failed: " + e.getMessage())
                            .build();
                }

                em.persist(site);
                em.getTransaction().commit();
            }


            // at the end, return any error message
            return Response
                    .ok()
                    .entity(mapper.valueToTree(requests).toString())
                    .build();
        } catch (IllegalArgumentException e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();

            log.error("Persistence Error Adding Site info", e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }

    /**
     * SiteEditRequest Request -- for Site update requests.
     *
     * siteCode - desired siteCode to alter
     * labName - desired Lab Name value
     * pocEmails - array of pocEmails to associate to the siteCode
     * softwareGroupEmail - desired Software Group Email value
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SiteEditRequest implements Serializable {

        private String siteCode;
        private String labName;
        private List<String> emailDomains;
        private List<String> pocEmails;
        private boolean standardUsage = false;
        private boolean hqUsage = false;
        private String softwareGroupEmail;

        public SiteEditRequest() {
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
         * @return the labName
         */
        public String getLabName() {
            return labName;
        }

        /**
         * @param lab the labName to set
         */
        public void setLabName(String lab) {
            this.labName = lab;
        }

        /**
         * @return the pocEmails
         */
        public List<String> getPocEmails() {
            return pocEmails;
        }

        /**
         * @param pocEmails associated to a siteCode
         */
        public void setPocEmails(List<String> pocEmails) {
            if (pocEmails == null)
                pocEmails = new ArrayList<String>();

            for (int i = pocEmails.size() - 1; i >= 0; i--) {
                // do not allow empty strings
                if (StringUtils.isBlank(pocEmails.get(i)))
                    pocEmails.remove(i);
                else
                    pocEmails.set(i, pocEmails.get(i).toLowerCase());
            }

            this.pocEmails = pocEmails;
        }

        /**
         * @return the emailDomains
         */
        public List<String> getEmailDomains() {
            return emailDomains;
        }

        /**
         * @param emailDomains domains associated to a siteCode
         */
        public void setEmailDomains(List<String> emailDomains) {
            for (int i = emailDomains.size() - 1; i >= 0; i--) {
                // do not allow empty strings
                if (StringUtils.isBlank(emailDomains.get(i)))
                    emailDomains.remove(i);
                else
                    emailDomains.set(i, emailDomains.get(i).toLowerCase());
            }

            this.emailDomains = emailDomains;
        }

        /**
         * @return the isStandardUsage
         */
        public boolean isStandardUsage() {
            return standardUsage;
        }

        /**
         * @param usage boolean for Standard usage
         */   
        public void setStandardUsage(boolean usage) {
            this.standardUsage = usage;
        }
    
        /**
         * @return the isHqUsage
         */
        public boolean isHqUsage() {
            return hqUsage;
        }

        /**
         * @param usage boolean for HQ usage
         */    
        public void setHqUsage(boolean usage) {
            this.hqUsage = usage;
        }

        /**
         * @return the softwareGroupEmail
         */
        public String getSoftwareGroupEmail() {
            return softwareGroupEmail;
        }

        /**
         * @param email associated to a siteCode
         */
        public void setSoftwareGroupEmail(String email) {
            this.softwareGroupEmail = email;
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
