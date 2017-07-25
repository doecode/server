/*
 */
package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;

import gov.osti.connectors.BitBucket;
import gov.osti.connectors.ConnectorFactory;
import gov.osti.connectors.GitHub;
import gov.osti.connectors.HttpUtil;
import gov.osti.connectors.SourceForge;
import gov.osti.doi.DataCite;
import gov.osti.entity.Agent;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.entity.DOECodeMetadata.Status;
import gov.osti.entity.OstiMetadata;
import gov.osti.entity.User;
import gov.osti.indexer.AgentSerializer;
import gov.osti.listeners.DoeServletContextListener;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
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
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.jersey.server.mvc.Viewable;

/**
 * REST Web Service for Metadata.
 * 
 * endpoints:
 * 
 * GET
 * metadata/{codeId} - retrieve instance of JSON for codeId
 * metadata/autopopulate?repo={url} - attempt an auto-populate Connector call for
 * indicated URL
 * metadata/yaml/{codeId} - get the YAML for a given codeId
 * metadata/autopopulate/yaml?repo={url} - get auto-populate information in YAML
 * 
 * POST
 * metadata - send JSON for persisting to the storage layer
 * metadata/submit - send JSON for posting to both ELINK and persistence layer
 * metadata/yaml - send JSON, get YAML back
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
    
    // URL to indexer services, if configured
    private static String INDEX_URL = DoeServletContextListener.getConfigurationProperty("index.url");
    
    // create and start a ConnectorFactory for use by "autopopulate" service
    static {
        try {
        factory = ConnectorFactory.getInstance()
                .add(new GitHub())
                .add(new SourceForge())
                .add(new BitBucket())
                .build();
        } catch ( IOException e ) {
            log.warn("Configuration failure: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new instance of MetadataResource
     */
    public Metadata() {
    }
    
    // ObjectMapper instance for metadata interchange
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // ObjectMapper specifically for indexing purposes
    private static final ObjectMapper index_mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    static {
        // customized serializer module for Agent names consolidation
        SimpleModule module = new SimpleModule();
        module.addSerializer(Agent.class, new AgentSerializer());
        index_mapper.registerModule(module);
    }

    /**
     * Link to API Documentation template.
     * 
     * @return a Viewable API documentation template
     */
    @GET
    @Produces (MediaType.TEXT_HTML)
    public Viewable getDocumentation() {
        return new Viewable("/docs");
    }
    
    /**
     * Look up the METADATA if possible by its codeID value, and return the 
     * result in the desired format.
     * 
     * @param codeId the Metadata codeId to look for
     * @param format optionally specify the requested output format (JSON is the
     * default, or "text/yaml" if YAML desired)
     * @return the Metadata information in the desired format
     */
    @GET
    @Path ("{codeId}")
    @Produces ({MediaType.APPLICATION_JSON, "text/yaml"})
    public Response load(@PathParam ("codeId") Long codeId, @QueryParam ("format") String format) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        Subject subject = SecurityUtils.getSubject();
        
        try {
            DOECodeMetadata md = em.find(DOECodeMetadata.class, codeId);
            
            if ( null==md )
                throw new NotFoundException ("ID not on file.");
            
            // non-Published workflow REQUIRES authentication
            if (!Status.Published.equals(md.getWorkflowStatus())) {
                // send back a FORBIDDEN response
                if (!subject.isAuthenticated()) {
                    return Response
                            .status(Response.Status.FORBIDDEN)
                            .build();
                }
            }
            
            // if YAML is requested, return that; otherwise, default to JSON
            if ("yaml".equals(format)) {
                // return the YAML
                return
                    Response
                    .status(Response.Status.OK)
                    .header("Content-Disposition", "attachment; filename = \"metadata.yml\"")
                    .entity(HttpUtil.writeMetadataYaml(md))
                    .build();
            } else {
                // send back the JSON
                return Response
                    .status(Response.Status.OK)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
            }
        } catch ( IOException e ) {
            log.warn("YAML exception: " + e.getMessage());
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Output conversion error")
                    .build();
        } finally {
            em.close();
        }
    }
    
    private class RecordsList {
    	private List<DOECodeMetadata> records;
    	
    	RecordsList(List<DOECodeMetadata> records) {
    		this.records = records;
    	}

		public List<DOECodeMetadata> getRecords() {
			return records;
		}

		public void setRecords(List<DOECodeMetadata> records) {
			this.records = records;
		}
		
	    public JsonNode toJson() {
	        return mapper.valueToTree(this);
	    }

    	
    	
    }
    
    /**
     * Acquire a listing of all records by OWNER.
     * 
     * @return the Metadata information in the desired format
     * @throws JsonProcessingException 
     */
    @GET
    @Path ("/projects")
    @Produces (MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    public Response load() throws JsonProcessingException {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        // get the security user in context
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        try {
        	TypedQuery<DOECodeMetadata> query = em.createQuery("SELECT md FROM DOECodeMetadata md WHERE md.owner = :owner", DOECodeMetadata.class);
        	RecordsList records = new RecordsList(query.setParameter("owner", user.getEmail()).getResultList());
                    return Response
                            .status(Response.Status.OK)
                            .entity(mapper.createObjectNode().putPOJO("records", records.toJson()).toString())
                            .build();
        } finally {
            em.close();
        }
    }
    
    /**
     * Call to auto-populate Metadata information via Connector, if possible.
     * 
     * @param url the REPOSITORY URL to look up information from
     * @param format optionally, the output format ("yaml" supported) JSON is default
     * @return a Metadata instance in the desired output format if information was found
     */
    @GET
    @Path ("/autopopulate")
    @Produces ({MediaType.APPLICATION_JSON, "text/yaml"})
    public Response autopopulate(@QueryParam("repo") String url,
                                 @QueryParam("format") String format) {
        JsonNode result = factory.read(url);
        
        if (null==result)
            return Response.status(Response.Status.NO_CONTENT).build();
        
        // if YAML is requested, return that; otherwise, default to JSON output
        if ("yaml".equals(format)) {
            try {
            return Response
                    .status(Response.Status.OK)
                    .header("Content-Disposition", "attachment; filename = \"metadata.yml\"")
                    .entity(HttpUtil.writeMetadataYaml(result))
                    .build();
            } catch ( IOException e ) {
                log.warn("YAML conversion error: " + e.getMessage());
                return Response
                        .status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("YAML conversion error")
                        .build();
            }
        } else {
            // send back the default JSON response
            return Response.status(Response.Status.OK).entity(mapper.createObjectNode().putPOJO("metadata", result).toString()).build();
        }
    }
    
    /**
     * Persist the DOECodeMetadata Object to the persistence layer.  Assumes an
     * open Transaction is already in progress, and it's up to the caller to
     * handle Exceptions or commit as appropriate.
     * 
     * If the "code ID" is already present in the Object to store, it will 
     * attempt to merge changes; otherwise, a new Object will be instantiated
     * in the database.  Note that any WORKFLOW STATUS present will be preserved,
     * regardless of the incoming one.
     * 
     * @param em the EntityManager to interface with the persistence layer
     * @param md the Object to store
     * @param user the User performing this action (must be the OWNER of the
     * record in order to UPDATE)
     * @throws NotFoundException when record to update is not on file
     * @throws IllegalAccessException when attempting to update record not
     * owned by User
     * @throws InvocationTargetException on reflection errors
     */
    private void store(EntityManager em, DOECodeMetadata md, User user) throws NotFoundException, 
            IllegalAccessException, InvocationTargetException {
        // if there's a CODE ID, attempt to look up the record first and 
        // copy attributes into it
        if ( null==md.getCodeId() || 0==md.getCodeId()) {
            em.persist(md);
        } else {
            DOECodeMetadata emd = em.find(DOECodeMetadata.class, md.getCodeId());
            
            if ( null!=emd ) {
                // must be the OWNER in order to UPDATE
                if (!user.getEmail().equals(emd.getOwner()))
                    throw new IllegalAccessException("Invalid access attempt.");
                
                // found the record, keep the Status
                Status currentStatus = emd.getWorkflowStatus();
                
                // found it, "merge" Bean attributes
                BeanUtilsBean noNulls = new NoNullsBeanUtilsBean();
                noNulls.copyProperties(emd, md);
                emd.setWorkflowStatus(currentStatus); // preserve the Status
                
                // EntityManager should handle this attached Object
            } else {
                // can't find record to update, that's an error
                log.warn("Unable to locate record for " + md.getCodeId() + " to update.");
                throw new NotFoundException("Record Code ID " + md.getCodeId() + " not on file.");
            }
        }
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
     * Attempt to send this Metadata information to the indexing service configured.
     * If no service is configured, do nothing.
     * 
     * @param md the Metadata to send
     */
    private static void sendToIndex(DOECodeMetadata md) {
        // if indexing is not configured, skip this step
        if ("".equals(INDEX_URL))
            return;
        
        // set some reasonable default timeouts
        RequestConfig rc = RequestConfig
                .custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .build();
        // create an HTTP client to request through
        CloseableHttpClient hc = 
                HttpClientBuilder
                .create()
                .setDefaultRequestConfig(rc)
                .build();
        try {
            // construct a POST submission to the indexer service
            HttpPost post = new HttpPost(INDEX_URL);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");
            post.setEntity(new StringEntity(index_mapper.writeValueAsString(md)));
            
            HttpResponse response = hc.execute(post);
            
            if ( HttpStatus.SC_OK!=response.getStatusLine().getStatusCode() ) {
                log.warn("Indexing Error occurred for ID=" + md.getCodeId());
                log.warn("Message: " + EntityUtils.toString(response.getEntity()));
            } else {
                log.info("Response OK: " + EntityUtils.toString(response.getEntity()));
            }
        } catch ( IOException e ) {
            log.warn("Indexing Error: " + e.getMessage() + " ID=" + md.getCodeId());
        } finally {
            try {
                if (null!=hc) hc.close();
            } catch ( IOException e ) {
                log.warn("Index Close Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * PUBLISH a Metadata Object; this operation signifies the project is 
     * ready to be posted to DOECode's output search services.  This endpoint
     * DOES NOT transmit the project to OSTI's software services for publication 
     * there.
     * 
     * Will return a FORBIDDEN attempt should a User attempt to modify someone
     * else's record.
     * 
     * @param object JSON of the DOECodeMetadata object to PUBLISH
     * @return a Response containing the persisted metadata entity in JSON
     * @throws InternalServerErrorException on JSON parsing or other IO errors
     */
    @POST
    @Consumes ( MediaType.APPLICATION_JSON )
    @Produces ( MediaType.APPLICATION_JSON )
    @Path ("/publish")
    @RequiresAuthentication
    public Response publish(String object) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        try {
            em.getTransaction().begin();
            
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(object));
            // set the OWNER
            md.setOwner(user.getEmail());
            // set the WORKFLOW STATUS
            md.setWorkflowStatus(Status.Published);
            
            // store it
            store(em, md, user);
            
            // send to DataCite if needed
            if ( null!=md.getDoi() ) {
                if ( !DataCite.register(md) ) 
                    log.warn("DataCite registration failed for " + md.getDoi());
            }
            // commit it
            em.getTransaction().commit();
            
            // send this information to SOLR as well, if configured
            sendToIndex(md);
            
            // we are done here
            return Response
                    .status(200)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } catch ( NotFoundException e ) {
            throw e;
        } catch ( IllegalAccessException e ) {
            log.warn("Persistence Error: Unable to update record, invalid owner: " + user.getEmail());
            log.warn("Message: " + e.getMessage());
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .entity("Invalid Access:  Logged in User not allowed to modify this record.")
                    .build();
        } catch ( IOException | InvocationTargetException e ) {
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
     * Will return a FORBIDDEN response if the OWNER logged in does not match
     * the record's OWNER.
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
    @RequiresAuthentication
    public Response submit(String object) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        try {
            em.getTransaction().begin();
            
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(object));
            // set the OWNER
            md.setOwner(user.getEmail());
            // set the WORKFLOW STATUS
            md.setWorkflowStatus(Status.Submitted);
            
            // persist this to the database
            store(em, md, user);
            
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
                    // if appropriate, register or update the DOI with DataCite
                    if ( null!=md.getDoi() ) {
                        if ( !DataCite.register(md) )
                            log.warn("DataCite DOI registration failed.");
                    }
                } finally {
                    hc.close();
                }
            }
            // if we make it this far, go ahead and commit the transaction
            em.getTransaction().commit();
            
            // send it to the indexer
            sendToIndex(md);
            
            // and we're happy
            return Response
                    .status(200)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } catch ( NotFoundException e ) {
            throw e;
        } catch ( IllegalAccessException e ) {
            log.warn("Persistence Error: Invalid owner update attempt: " + user.getEmail());
            log.warn("Message: " + e.getMessage());
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .entity("Invalid Access:  Unable to edit indicated record.")
                    .build();
        } catch ( IOException |  InvocationTargetException e ) {
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
    @RequiresAuthentication
    public Response save(String object) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        try {
            em.getTransaction().begin();
            
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(object));
            md.setWorkflowStatus(Status.Saved); // default to this
            md.setOwner(user.getEmail()); // this User should OWN it
            
            store(em, md, user);
            
            // we're done here
            em.getTransaction().commit();
            
            return Response
                    .status(200)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } catch ( NotFoundException e ) {
            throw e;
        } catch ( IllegalAccessException e ) {
            log.warn("Persistence Error:  Invalid update attempt from " + user.getEmail());
            log.warn("Message: " + e.getMessage());
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .entity("Unable to persist update to indicated record.")
                    .build();
        } catch ( IOException | InvocationTargetException e ) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Persistence Error: " + e.getMessage());
            throw new InternalServerErrorException("IO Error: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
