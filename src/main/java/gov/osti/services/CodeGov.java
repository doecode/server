package gov.osti.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import gov.osti.entity.MetadataSnapshot;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.entity.Site;
import gov.osti.listeners.DoeServletContextListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;
import org.apache.shiro.authz.annotation.RequiresRoles;

/**
 * REST Web Service for CodeGov information.
 *
 * endpoints:
 *
 * GET
 * codegov - retrieve JSON for use with Code.gov
 * codegov/refresh - refresh JSON data for "codegov" endpoint, if owner/administrator
 *
 * @author sowerst
 */
@Path("codegov")
public class CodeGov {

    // inject a Context
    @Context
    ServletContext context;

    // logger instance
    private static final Logger log = LoggerFactory.getLogger(CodeGov.class);
    // absolute filesystem location to store uploaded files, if any
    private static final String FILE_UPLOADS = DoeServletContextListener.getConfigurationProperty("file.uploads");

    /**
     * Creates a new instance of MetadataResource for use with Code.gov
     */
    public CodeGov() {
    }

    /**
     * Implement a simple JSON filter to remove named properties.
     */
    @JsonFilter("filter properties by name")
    class PropertyFilterMixIn {
    }

    // filter out certain attribute names
    protected final static String[] ignoreProperties = {
        "softwareType",
        "software_type",
        "developers",
        "contributors",
        "sponsoringOrganizations",
        "sponsoring_organizations",
        "contributingOrganizations",
        "contributing_organizations",
        "researchOrganizations",
        "research_organizations",
        "relatedIdentifiers",
        "related_identifiers",
        "releaseDate",
        "release_date",
        "acronym",
        "doi",
        "documentationUrl",
        "documentation_url",
        "countryOfOrigin",
        "country_of_origin",
        "keywords",
        "siteAccessionNumber",
        "site_accession_number",
        "otherSpecialRequirements",
        "other_special_requirements",
        "fileName",
        "file_name",
        "recipientName",
        "recipient_name",
        "recipientEmail",
        "recipient_email",
        "recipientPhone",
        "recipient_phone",
        "recipientOrg",
        "recipient_organization",
        "workflowStatus",
        "workflow_status",
        "accessLimitations",
        "access_limitations",
        "disclaimers"
    };
    protected static FilterProvider filter = new SimpleFilterProvider()
            .addFilter("filter properties by name",
                    SimpleBeanPropertyFilter.serializeAllExcept(ignoreProperties));

    // ObjectMapper instance for metadata interchange
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setTimeZone(TimeZone.getDefault());

    // ObjectMapper instance for metadata interchange
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .addMixIn(Object.class, PropertyFilterMixIn.class)
            .setTimeZone(TimeZone.getDefault());

    /**
     * Intended to be a List of retrieved Metadata records/projects.
     */
    private class RecordsList {

        // the records
        private List<DOECodeMetadata> records;
        // a total count of a matched query
        private long total;

        RecordsList(List<DOECodeMetadata> records) {
            this.records = records;
        }

        /**
         * Acquire the list of records (a single page of results).
         *
         * @return a List of DOECodeMetadata Objects
         */
        public List<DOECodeMetadata> getRecords() {
            return records;
        }

        /**
         * Set the current page of results.
         *
         * @param records a List of DOECodeMetadata Objects for this page
         */
        public void setRecords(List<DOECodeMetadata> records) {
            this.records = records;
        }

        /**
         * Set a TOTAL count of matches for this search/list.
         *
         * @param count the count to set
         */
        public void setTotal(long count) {
            total = count;
        }

        /**
         * Get the TOTAL number of matching rows.
         *
         * @return the total count matched
         */
        public long getTotal() {
            return total;
        }
    }

    /**
     * Listing current Code.gov JSON data.
     *
     * @return the Code.gov JSON information
     * @throws JsonProcessingException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCodeGovData()
            throws JsonProcessingException {

        try {
            java.nio.file.Path codegovFile = Paths.get(FILE_UPLOADS, "codegov", "code.json");

            // if no file was found, fail
            if (!Files.exists(codegovFile))
                return ErrorResponse
                        .status(Response.Status.NOT_FOUND, "Code.gov JSON file not found!")
                        .build();

            // read file
            JsonNode json = JSON_MAPPER.readTree(new FileInputStream(codegovFile.toString()));

            return Response
                    .status(Response.Status.OK)
                    .entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json))
                    .build();

        } catch (IOException e) {  // IO
            log.warn("JSON conversion error: " + e.getMessage());
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, "JSON conversion error.")
                    .build();
        }
    }

    /**
     * Acquire a listing of all Approved records.
     *
     * @return the Metadata information in the desired format for Scrapper
     * @throws JsonProcessingException
     */
    @GET
    @Path("/listrecords")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresRoles("ContentAdmin")
    public Response listApprovedSnapshots()
            throws JsonProcessingException {
        EntityManager em = DoeServletContextListener.createEntityManager();

        try {
            TypedQuery<MetadataSnapshot> query;

            // find all Approved records
            query = em.createNamedQuery("MetadataSnapshot.findAllByStatus", MetadataSnapshot.class)
                    .setParameter("status", DOECodeMetadata.Status.Approved);

            // lookup Snapshot JSON at time of Approval
            List<MetadataSnapshot> snapshots = query.getResultList();

            // map Snapshot JSON to Metadata objects
            List<DOECodeMetadata> metadataList = new ArrayList<>();
            for (MetadataSnapshot s : snapshots)
                // pull metadata from snapshot
                metadataList.add(JSON_MAPPER.readValue(s.getJson(), DOECodeMetadata.class));

            // create a List of Metadata records, then filter down to desired results
            RecordsList records = new RecordsList(metadataList);
            ObjectNode recordsObject = mapper.setFilterProvider(filter).valueToTree(records);

            // lookup Announced Snapshot status info for each item
            TypedQuery<MetadataSnapshot> querySnapshot = em.createNamedQuery("MetadataSnapshot.findByCodeIdAndStatus", MetadataSnapshot.class)
                    .setParameter("status", DOECodeMetadata.Status.Announced);

            // lookup Lab Name for each item
            TypedQuery<Site> querySite = em.createNamedQuery("Site.findBySiteCode", Site.class);

            JsonNode recordNode = recordsObject.get("records");
            if (recordNode.isArray()) {
                int rowCount = 0;
                for (JsonNode objNode : recordNode) {
                    rowCount++;

                    // get code_id to find Snapshot
                    long codeId = objNode.get("code_id").asLong();
                    querySnapshot.setParameter("codeId", codeId);

                    // if Announced Snapshot exists, then it has been "ever_announced"
                    List<MetadataSnapshot> snapshotResults = querySnapshot.setMaxResults(1).getResultList();
                    boolean everAnnounced = snapshotResults.size() > 0;

                    // add "ever_announced" status indicator to response record
                    ((ObjectNode) objNode).put("ever_announced", everAnnounced);

                    // get site_ownership_code to find Lab
                    String siteCode = objNode.get("site_ownership_code").asText();
                    querySite.setParameter("site", siteCode);

                    // if Site Code exists, then it create "lab_display_name" from Lab and Site Code.
                    List<Site> siteResults = querySite.setMaxResults(1).getResultList();
                    if (siteResults.size() > 0) {
                        Site s = siteResults.get(0);

                        String labDisplayName = s.getLab() + " (" + s.getSiteCode() + ")";

                        // add "lab_display_name" info to response record
                        ((ObjectNode) objNode).put("lab_display_name", labDisplayName);
                    }
                }

                // update Total
                recordsObject.put("total", rowCount);
            }

            return Response
                    .status(Response.Status.OK)
                    .entity(recordsObject.toString())
                    .build();

        } catch (IOException e) {
            log.warn("JSON conversion error: " + e.getMessage());
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, "JSON conversion error.")
                    .build();
        } finally {
            em.close();
        }
    }
}
