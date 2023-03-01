/*
 */
package gov.osti.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import gov.osti.entity.Contributor;
import gov.osti.entity.ContributingOrganization;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.entity.DOECodeMetadata.ProjectType;
import gov.osti.entity.DOECodeMetadata.License;
import gov.osti.entity.FundingIdentifier;
import gov.osti.entity.RelatedIdentifier;
import gov.osti.entity.RelatedIdentifier.RelationType;
import gov.osti.entity.Limitation;
import gov.osti.indexer.ProjectTypeSerializer;
import gov.osti.indexer.ContributorTypeSerializer;
import gov.osti.indexer.ContributingOrgTypeSerializer;
import gov.osti.indexer.FundingIdentifierSerializer;
import gov.osti.indexer.LicenseSerializer;
import gov.osti.indexer.RelatedIdentifierTypeSerializer;
import gov.osti.indexer.RelationTypeSerializer;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.TypedQuery;
import gov.osti.listeners.DoeServletContextListener;
import javax.persistence.EntityManager;

/**
 * REST Web Service
 *
 * @author ensornl
 */
@Path("types")
public class Types {

    @Context
    private UriInfo context;
    // a Logger
    private static final Logger log = LoggerFactory.getLogger(Types.class);
    // JSON data binder
    private static final ObjectMapper mapper = new ObjectMapper().setTimeZone(TimeZone.getDefault());
    static {
        // customized serializer module for Agent names consolidation
        SimpleModule module = new SimpleModule()
            .addSerializer(License.class, new LicenseSerializer())
            .addSerializer(Contributor.Type.class, new ContributorTypeSerializer())
            .addSerializer(ContributingOrganization.Type.class, new ContributingOrgTypeSerializer())
            .addSerializer(RelationType.class, new RelationTypeSerializer())
            .addSerializer(RelatedIdentifier.Type.class, new RelatedIdentifierTypeSerializer())
            .addSerializer(FundingIdentifier.Type.class, new FundingIdentifierSerializer())
            .addSerializer(ProjectType.class, new ProjectTypeSerializer());
        
        mapper.registerModule(module);
    }
    
    /**
     * Creates a new instance of TypeService
     */
    public Types() {
    }
    
    /**
     * Acquire a listing of all valid PROJECT TYPE values and descriptions
     * @return JSON containing an array of PROJECT TYPE values
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/projecttypes")
    public Response getProjectType() {
        try {
            return Response
                    .ok()
                    .entity(mapper
                            .createObjectNode()
                            .putPOJO("project_type", 
                                    mapper.writeValueAsString(Arrays.asList(DOECodeMetadata.ProjectType.values()))).toString())
                    .build();
        } catch ( JsonProcessingException e ) {
            log.warn("JSON Output Error", e);
            return ErrorResponse
                    .internalServerError("JSON Error")
                    .build();
        }
    }
    
    /**
     * Get a listing of all the valid LICENSES.
     * @return a JSON Response containing an array of available LICENSES
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path ("/licenses")
    public Response getLicenses() {
        try {
            return Response
                    .ok()
                    .entity(mapper
                            .createObjectNode()
                            .putPOJO("licenses", 
                                    mapper.writeValueAsString(Arrays.asList(DOECodeMetadata.License.values()))).toString())
                    .build();
        } catch ( JsonProcessingException e ) {
            log.warn("JSON Output Error", e);
            return ErrorResponse
                    .internalServerError("JSON Error")
                    .build();
        }
    }

    /**
     * Retrieve a listing of all CONTRIBUTOR TYPES
     * @return a Response in JSON containing all the CONTRIBUTOR TYPES mappings
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path ("/contributortypes")
    public Response getContributorTypes() {
        try {
            return Response
                    .ok()
                    .entity(mapper
                            .createObjectNode()
                            .putPOJO("personalContributorTypes",
                                    mapper.valueToTree(Contributor.Type.values()))
                            .putPOJO("organizationalContributorTypes",
                                    mapper.valueToTree(ContributingOrganization.Type.values())).toString())
                    .build();
        } catch (Exception e ) {
            log.warn("JSON Error: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("JSON Error")
                    .build();
        }
    }
    
    /**
     * Enumerate the RELATED IDENTIFIERS types and relations as JSON.
     * 
     * @return JSON containing valid related identifier types
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/relatedidentifiertypes")
    public Response getRelatedIdentifierTypes() {
        try {
        return Response
                .ok()
                .entity(mapper
                        .createObjectNode()
                        .putPOJO("relatedIdentiferTypes", mapper.writeValueAsString(RelatedIdentifier.Type.values())).toString())
                .build();
        } catch ( JsonProcessingException e ) {
            log.warn("JSON Error: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("JSON Error")
                    .build();
        }
    }
    
    /**
     * Obtain a JSON listing of valid relation types for related identifiers.
     * 
     * @return JSON containing list of valid relation types
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/relationtypes")
    public Response getRelationTypes() {
        try {
            return Response
                    .ok()
                    .entity(mapper
                            .createObjectNode()
                            .putPOJO("relationTypes", 
                                    mapper.writeValueAsString(RelatedIdentifier.RelationType.values())).toString())
                    .build();
        } catch ( JsonProcessingException e ) {
            log.warn("JSON Error: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("JSON Error")
                    .build();
        }
    }
    
    /**
     * Enumerate the valid FUNDING IDENTIFIERS types.
     * 
     * @return JSON containing a single array "types" listing all the valid FUNDING IDENTIFIER TYPES.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fundingidentifiertypes")
    public Response getFundingIdentifierTypes() {
        try {
            return Response
                    .ok()
                    .entity(mapper
                            .createObjectNode()
                            .putPOJO("fundingIdentifierTypes", 
                                    mapper.writeValueAsString(FundingIdentifier.Type.values())).toString())
                    .build();
        } catch ( JsonProcessingException e ) {
            log.warn("JSON Error: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("JSON Error")
                    .build();
        }
    }
    
    /**
     * Obtain a JSON listing of valid relation types for related identifiers.
     * 
     * @return JSON containing list of valid relation types
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/accesslimitationtypes")
    public Response getAccessLimitationTypes() {
        EntityManager em = DoeServletContextListener.createEntityManager();

        try {

            TypedQuery<Limitation> query = em.createNamedQuery("Limitation.findAll", Limitation.class);

            List<Limitation> limits = query.getResultList();

            // return the results back
            return Response
                    .ok()
                    .entity(mapper.writeValueAsString(limits))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Access Limitation Lookup Error", e);
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }
}
