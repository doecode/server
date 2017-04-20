/*
 */
package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import gov.osti.connectors.BitBucket;
import gov.osti.connectors.ConnectorFactory;
import gov.osti.connectors.GitHub;
import gov.osti.connectors.HttpUtil;
import gov.osti.connectors.SourceForge;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.entity.DOECodeMetadata.Status;
import gov.osti.entity.OstiMetadata;
import gov.osti.listeners.DoeServletContextListener;
import java.io.IOException;
import java.io.StringReader;
import javax.persistence.EntityManager;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service for Metadata.
 * 
 * endpoints:
 * 
 * GET
 * metadata/{codeId} - retrieve instance of JSON for codeId
 * metadata/autopopulate?repo={url} - attempt an auto-populate Connector call for
 * indicated URL
 * 
 * POST
 * metadata - send JSON for persisting to the storage layer
 * metadata/submit - send JSON for posting to both ELINK and persistence layer
 *
 * @author ensornl
 */
@Path("metadata")
public class Metadata {
    // inject a Context
    @Context ServletContext context;
    
    // logger instance
    private static Logger log = LoggerFactory.getLogger(Metadata.class);
    private static ConnectorFactory factory;
    
    // create and start a ConnectorFactory for use by "autopopulate" service
    static {
        try {
        factory = ConnectorFactory.getInstance()
                .add(new GitHub())
                .add(new SourceForge())
                .add(new BitBucket())
                .build();
        } catch ( IOException e ) {
            log.warn("Unable to start ConnectorFactory: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new instance of MetadataResource
     */
    public Metadata() {
    }
    
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    
    /**
     * Attempt to look up and return a Metadata record.
     * 
     * Produces:  application/json
     * 
     * @param codeId the Metadata codeId to look for
     * @return JSON of the Metadata if found
     */
    @GET
    @Path ("{codeId}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response load(@PathParam ("codeId") Long codeId ) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            DOECodeMetadata md = em.find(DOECodeMetadata.class, codeId);
            
            if ( null==md )
                throw new NotFoundException("ID not on file.");
            
            // just send it back
            return Response
                    .status(Response.Status.OK)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } finally {
            em.close();
        }
    }
    
    /**
     * Look up the METADATA if possible by its codeID value, and return the
     * result as YAML.
     * 
     * Accept: text/yaml
     * 
     * @param codeId the Metadata codeId to look for
     * @return YAML of the Metadata information if possible
     */
    @GET
    @Path ("{codeId}")
    @Produces ("text/yaml")
    public Response loadYaml(@PathParam ("codeId") Long codeId ) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            DOECodeMetadata md = em.find(DOECodeMetadata.class, codeId);
            
            if ( null==md )
                throw new NotFoundException ("ID not on file.");
            
            // send back the YAML
            return Response
                    .status(Response.Status.OK)
                    .entity(HttpUtil.writeMetadataYaml(md))
                    .build();
        } catch ( IOException e ) {
            log.warn("YAML Output Error: " + e.getMessage());
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("YAML Output Error")
                    .build();
        } finally {
            em.close();
        }
    }
    
    /**
     * Call to auto-populate Metadata information via Connector, if possible.
     * 
     * @param url the REPOSITORY URL to look up information from
     * @return a Metadata instance (JSON) if information was found
     */
    @GET
    @Path ("/autopopulate")
    @Produces (MediaType.APPLICATION_JSON)
    public Response autopopulate(@QueryParam("repo") String url) {
        JsonNode result = factory.read(url);
        
        // no connector information, return nothing
        return (null==result) ?
                Response.status(Response.Status.NO_CONTENT).build() :
                Response.status(Response.Status.OK).entity(mapper.createObjectNode().putPOJO("metadata", result).toString()).build();
    }
    
    /**
     * Persist the DOECodeMetadata Object to the persistence layer.  Assumes an
     * open Transaction is already in progress, and it's up to the caller to
     * handle Exceptions or commit as appropriate.
     * 
     * If the "code ID" is already present in the Object to store, it will 
     * attempt to merge changes; otherwise, a new Object will be instantiated
     * in the database.
     * 
     * @param em the EntityManager to interface with the persistence layer
     * @param md the Object to store
     */
    private void store(EntityManager em, DOECodeMetadata md) {
        if ( null==md.getCodeId() || 0==md.getCodeId() )
            em.persist(md);
        else
            em.merge(md);
    }
    
    /**
     * Convert incoming JSON object of Metadata information to YAML if possible.
     * 
     * @param object JSON of the Metadata information
     * @return YAML of that JSON object, if mappable
     */
    @POST
    @Consumes (MediaType.APPLICATION_JSON)
    @Produces ("text/yaml")
    @Path ("/yaml")
    public Response asYAML(String object) {
        try {
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(object));
            
            return Response
                    .status(Response.Status.OK)
                    .entity(HttpUtil.writeMetadataYaml(md))
                    .build();
        } catch ( IOException e ) {
            log.warn("YAML conversion error: " + e.getMessage());
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("YAML conversion error")
                    .build();
        }
    }
    
    /**
     * PUBLISH a Metadata Object; this operation signifies the project is 
     * ready to be posted to DOECode's output search services.  This endpoint
     * DOES NOT transmit the project to OSTI's software services for publication 
     * there.
     * 
     * @param object JSON of the DOECodeMetadata object to PUBLISH
     * @return a Response containing the persisted metadata entity in JSON
     * @throws InternalServerErrorException on JSON parsing or other IO errors
     */
    @POST
    @Consumes ( MediaType.APPLICATION_JSON )
    @Produces ( MediaType.APPLICATION_JSON )
    @Path ("/publish")
    public Response publish(String object) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            em.getTransaction().begin();
            
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(object));
            
            // set the WORKFLOW STATUS
            md.setWorkflowStatus(Status.Published);
            
            // store it
            store(em, md);
            
            // commit it
            em.getTransaction().commit();
            
            // we are done here
            return Response
                    .status(200)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } catch ( IOException e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Persistence Error Publishing: " + e.getMessage());
            throw new InternalServerErrorException("IO Error: " + e.getMessage());
        } finally {
            em.close();  
        }
    }
    
    /**
     * SUBMIT endpoint; saves Software record to DOECode and sends results to
     * OSTI in order to obtain a DOI registration and integrate with OSTI workflow.
     * 
     * @param object the JSON of the record to PUBLISH/SUBMIT.
     * @return a Response containing the resulting JSON metadata sent to OSTI,
     * including any DOI registered.
     * @throws InternalServerErrorException on JSON parsing or other IO errors
     */
    @POST
    @Consumes ( MediaType.APPLICATION_JSON )
    @Produces ( MediaType.APPLICATION_JSON )
    @Path ("/submit")
    public Response submit(String object) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            em.getTransaction().begin();
            
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(object));
            
            // set the WORKFLOW STATUS
            md.setWorkflowStatus(Status.Published);
            
            // persist this to the database
            store(em, md);
            
            // send this to OSTI
            OstiMetadata omd = new OstiMetadata();
            omd.set(md);
            
            // if configured, post this to OSTI
            String publishing_host = context.getInitParameter("publishing.host");
            if (null!=publishing_host) {
                // set some reasonable default timeouts
                RequestConfig rc = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000).build();
                // create an HTTP client to request through
                CloseableHttpClient hc = 
                        HttpClientBuilder
                        .create()
                        .setDefaultRequestConfig(rc)
                        .build();
                HttpPost post = new HttpPost(publishing_host + "/services/softwarecenter?action=api");
                post.setHeader("Content-Type", "application/json");
                post.setHeader("Accept", "application/json");
                post.setEntity(new StringEntity(omd.toJsonString()));
                
                try {
                    HttpResponse response = hc.execute(post);
                    String text = EntityUtils.toString(response.getEntity());

                    if ( HttpStatus.SC_OK!=response.getStatusLine().getStatusCode()) {
                        log.warn("OSTI Error: " + text);
                        throw new IOException ("OSTI software publication error");
                    }
                    // the TEXT coming back should contain JSON
                    JsonNode node = mapper.readTree(text);
                    JsonNode metadata = node.get("metadata");
                    if (null!=metadata) {
                        OstiMetadata m = OstiMetadata.fromJson(new StringReader(metadata.asText()));
                        // if we have no DOI, get one
                        if (null==md.getDoi()) {
                            md.setDoi(m.getDoi());
                            // save that
                            em.merge(md);
                        }
                    }
                } finally {
                    hc.close();
                }
            }
            // if we make it this far, go ahead and commit the transaction
            em.getTransaction().commit();
            
            // and we're happy
            return Response
                    .status(200)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } catch ( IOException e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Persistence Error Publishing: " + e.getMessage());
            throw new InternalServerErrorException("IO Error: " + e.getMessage());
        } finally {
            em.close();
        }
    }
    
    /**
     * POST a Metadata JSON object to the persistence layer. 
     * Saves the object to persistence layer; if the entity is already Published,
     * this operation is invalid.
     * 
     * @param object the JSON to post
     * @return the JSON after persistence; perhaps containing assigned codeId, etc.
     */
    @POST
    @Consumes ( MediaType.APPLICATION_JSON )
    @Produces ( MediaType.APPLICATION_JSON )
    public Response save(String object) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            em.getTransaction().begin();
            
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(object));
            
            // if this Entity is already Published, we cannot save
            if (null!=md.getCodeId()) {
                DOECodeMetadata emd = em.find(DOECodeMetadata.class, md.getCodeId());
                
                if (null!=emd && Status.Published==emd.getWorkflowStatus())
                    return Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Unable to Save a Published Metadata Object.")
                            .build();
            }
            // set the WORKFLOW STATUS
            md.setWorkflowStatus(Status.Saved);

            // store it
            store(em, md);
            
            em.getTransaction().commit();
            
            return Response
                    .status(200)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } catch ( IOException e ) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Persistence Error: " + e.getMessage());
            throw new InternalServerErrorException("IO Error: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
