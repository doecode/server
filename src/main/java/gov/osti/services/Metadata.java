/*
 */
package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import gov.osti.entity.DOECodeMetadata.Accessibility;
import gov.osti.entity.DOECodeMetadata.Status;
import gov.osti.entity.Developer;
import gov.osti.entity.OstiMetadata;
import gov.osti.entity.ResearchOrganization;
import gov.osti.entity.SponsoringOrganization;
import gov.osti.entity.User;
import gov.osti.indexer.AgentSerializer;
import gov.osti.listeners.DoeServletContextListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.apache.commons.lang3.StringUtils;
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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.jersey.server.mvc.Viewable;

/**
 * REST Web Service for Metadata.
 * 
 * endpoints:
 * 
 * GET
 * metadata/edit/{codeId} - retrieve JSON of metadata if permitted (requires authentication)
 * metadata/{codeId} - retrieve instance of JSON for codeId (PUBLISHED only), optionally in YAML format
 * metadata/autopopulate?repo={url} - attempt an auto-populate Connector call for
 * indicated URL, optionally in YAML format
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
    // absolute filesystem location to store uploaded files, if any
    private static String FILE_UPLOADS = DoeServletContextListener.getConfigurationProperty("file.uploads");
    
    // regular expressions for validating phone numbers (US) and email addresses
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
    
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
     * Simple class for validation responses in JSON.
     */
    private class ErrorResponse {
        // HTTP status code
        private int status;
        // list of errors, if applicable
        private List<String> errors = new ArrayList<>();
        
        /**
         * Set the HTTP status code
         * @param s the status code to set
         */
        public void setStatus(int s) { this.status = s; }
        /**
         * Get the HTTP status code
         * @return the HTTP status code
         */
        public int getStatus() { return this.status; }
        
        /**
         * Determine whether or not there are errors.  Does not serialize
         * to JSON.
         * @return true if errors are present, false otherwise
         */
        @JsonIgnore
        public boolean isEmpty() { return errors.isEmpty(); }
        
        /**
         * Add all messages at once.
         * 
         * @param messages messages to add to the errors
         * @return true if list was modified
         */
        public boolean addAll(List<String> messages) { return errors.addAll(messages); }
        
        /**
         * Add a single error message.
         * 
         * @param message a message to add to error messages
         */
        public void addError(String message) { errors.add(message); }
        /**
         * Set all the error messages at once.
         * @param e a List of all error messages
         */
        public void setErrors(List<String> e) { errors = e; }
        /**
         * Get the error messages, if any
         * @return the List of errors, possibly empty
         */
        public List<String> getErrors() { return this.errors; }
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
     * Create a JSON error response in JSONAPI format if possible.
     * 
     * @param status the Response.Status HTTP status code of this error
     * @param message an error message
     * @return a Response in JSONAPI format
     */
    protected Response errorResponse(Response.Status status, String message) {
        ErrorResponse errors = new ErrorResponse();
        errors.setStatus(status.getStatusCode());
        errors.addError(message);
        
        try {
            // send back JSON error response
            return Response
                    .status(status)
                    .entity(mapper.writeValueAsString(errors))
                    .build();
        } catch ( JsonProcessingException e ) {
            log.warn("JSON Error: " + e.getMessage());
            // fall back to plain text
            return Response
                    .status(status)
                    .entity("Error: " + message)
                    .build();
        }
    }
    
    /**
     * Generate a JSON error response in JSONAPI given a set of error messages.
     * 
     * @param status the Response.Status HTTP status code for the error
     * @param messages a List of messages to return
     * @return a Response in JSONAPI format
     */
    protected Response errorResponse(Response.Status status, List<String> messages) {
        ErrorResponse errors = new ErrorResponse();
        errors.setStatus(status.getStatusCode());
        errors.addAll(messages);
        
        try {
            // send back JSON error response
            return Response
                    .status(status)
                    .entity(mapper.writeValueAsString(errors))
                    .build();
        } catch ( JsonProcessingException e ) {
            log.warn("JSON Error: " + e.getMessage());
            // fall back to plain text
            return Response
                    .status(status)
                    .entity("Error: " + StringUtils.join(messages, ", "))
                    .build();
        }
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
     * Look up a record for EDITING, checks authentication and ownership prior
     * to succeeding.
     * 
     * Result Codes:
     * 200 - OK, with JSON containing the metadata information
     * 400 - you didn't specify a CODE ID
     * 401 - authentication required
     * 403 - forbidden, logged in user does not have permission to this metadata
     * 404 - requested metadata is not on file
     * 
     * @param codeId the CODE ID to look up
     * @return a Response containing JSON if successful
     */
    @GET
    @Path ("/edit/{codeId}")
    @Produces (MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    public Response edit(@PathParam("codeId") Long codeId) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        // no CODE ID?  Bad request.
        if (null==codeId)
            return errorResponse(Response.Status.BAD_REQUEST, "Missing code id.");
        
        DOECodeMetadata md = em.find(DOECodeMetadata.class, codeId);

        // no metadata?  404
        if ( null==md ) 
            return errorResponse(Response.Status.NOT_FOUND, "Code ID not on file.");

        // do you have permissions to get this?
        if ( !user.getEmail().equals(md.getOwner()) )
            return errorResponse(Response.Status.FORBIDDEN, "Permission denied.");

        // return the metadata
        return Response
                .status(Response.Status.OK)
                .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                .build();
    }
    
    /**
     * Look up the METADATA if possible by its codeID value, and return the 
     * result in the desired format.  Only retrieves PUBLISHED records.
     * 
     * Response Codes:
     * 200 - OK, with the JSON of the metadata
     * 403 - access to this record is forbidden (not PUBLISHED)
     * 404 - record is not on file
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
        
        try {
            DOECodeMetadata md = em.find(DOECodeMetadata.class, codeId);
            
            if ( null==md ) 
                return errorResponse(Response.Status.NOT_FOUND, "Code ID not on file.");
            
            // non-Published workflow REQUIRES authentication, not for here; use /edit
            if (!Status.Published.equals(md.getWorkflowStatus())) {
                return errorResponse(Response.Status.FORBIDDEN, "Access to record denied.");
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
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Output conversion error.");
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
                return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "YAML conversion error.");
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
        // fix the open source value before storing
        md.setOpenSource( !Accessibility.CS.equals(md.getAccessibility()) );
        
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
                
                // if already Published, keep it that way (can't go back to Saved)
                if (Status.Published.equals(emd.getWorkflowStatus()))
                    md.setWorkflowStatus(Status.Published);
                
                // found it, "merge" Bean attributes
                BeanUtilsBean noNulls = new NoNullsBeanUtilsBean();
                noNulls.copyProperties(emd, md);
                
                // what comes back needs to be complete:
                noNulls.copyProperties(md, emd);
                
                // EntityManager should handle this attached Object
                // NOTE: the returned Object is NOT ATTACHED to the EntityManager
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
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "YAML conversion error.");
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
     * Perform SAVE workflow on indicated METADATA.
     * 
     * @param json the JSON String containing the metadata to SAVE
     * @param file a FILE associated with this record if any
     * @param fileInfo file disposition of information if any
     * @return a Response containing the JSON of the saved record if successful,
     * or error information if not
     */
    private Response doSave(String json, InputStream file, FormDataContentDisposition fileInfo) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        try {
            em.getTransaction().begin();
            
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(json));
            md.setWorkflowStatus(Status.Saved); // default to this
            md.setOwner(user.getEmail()); // this User should OWN it
            
            store(em, md, user);
            
            // if there's a FILE associated here, store it
            if ( null!=file && null!=fileInfo ) {
                // re-attach metadata to transaction in order to store the filename
                md = em.find(DOECodeMetadata.class, md.getCodeId());
                
                try {
                    String fileName = writeFile(file, md.getCodeId(), fileInfo.getFileName());
                    md.setFileName(fileName);
                } catch ( IOException e ) {
                    log.error ("File Upload Failed: " + e.getMessage());
                    return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "File upload failed.");
                }
            }
            
            // we're done here
            em.getTransaction().commit();
            
            return Response
                    .status(200)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } catch ( NotFoundException e ) {
            return errorResponse(Response.Status.NOT_FOUND, e.getMessage());
        } catch ( IllegalAccessException e ) {
            log.warn("Persistence Error:  Invalid update attempt from " + user.getEmail());
            log.warn("Message: " + e.getMessage());
            return errorResponse(Response.Status.FORBIDDEN, "Unable to persist update for indicated record.");
        } catch ( IOException | InvocationTargetException e ) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Persistence Error: " + e.getMessage());
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Save IO Error: " + e.getMessage());
        } finally {
            em.close();
        }
    }
    
    /**
     * Handle PUBLISH workflow logic.
     * 
     * @param json JSON String containing the METADATA object to PUBLISH
     * @param file (optional) a FILE associated with this METADATA
     * @param fileInfo (optional) the FILE disposition information, if any
     * @return an appropriate Response object to the caller
     */
    private Response doPublish(String json, InputStream file, FormDataContentDisposition fileInfo) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        try {
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(json));
            
            em.getTransaction().begin();
            
            // set the ownership and workflow status
            md.setOwner(user.getEmail());
            md.setWorkflowStatus(Status.Published);
            
            // store it
            store(em, md, user);
            // check validations for Published workflow
            List<String> errors = validatePublished(md);
            if ( !errors.isEmpty() ) {
                // generate a JSONAPI errors object
                return errorResponse(Response.Status.BAD_REQUEST, errors);
            }
            
            // if there's a FILE associated here, store it
            if ( null!=file && null!=fileInfo ) {
                // re-attach metadata to transaction in order to store the filename
                md = em.find(DOECodeMetadata.class, md.getCodeId());
                
                try {
                    String fileName = writeFile(file, md.getCodeId(), fileInfo.getFileName());
                    md.setFileName(fileName);
                } catch ( IOException e ) {
                    log.error ("File Upload Failed: " + e.getMessage());
                    return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "File upload failed.");
                }
            }
            
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
                    .status(Response.Status.OK)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } catch ( NotFoundException e ) {
            return errorResponse(Response.Status.NOT_FOUND, e.getMessage());
        } catch ( IllegalAccessException e ) {
            log.warn("Persistence Error: Unable to update record, invalid owner: " + user.getEmail());
            log.warn("Message: " + e.getMessage());
            return errorResponse(Response.Status.FORBIDDEN, "Logged in User is not allowed to modify this record.");
        } catch ( IOException | InvocationTargetException e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Persistence Error Publishing: " + e.getMessage());
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Persistence error publishing record.");
        } finally {
            em.close();
        }
    }
    
    /**
     * Perform SUBMIT workflow operation, optionally with associated file uploads.
     * 
     * @param json String containing JSON of the Metadata to SUBMIT
     * @param file the FILE (if any) to attach to this metadata
     * @param fileInfo file disposition information if FILE present
     * @return a Response containing the JSON of the submitted record if successful, or
     * error information if not
     */
    private Response doSubmit(String json, InputStream file, FormDataContentDisposition fileInfo) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        try {
            DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(json));
            
            em.getTransaction().begin();
            // set the OWNER
            md.setOwner(user.getEmail());
            // set the WORKFLOW STATUS
            md.setWorkflowStatus(Status.Published);
            
            // persist this to the database
            store(em, md, user);
            // check validations
            List<String> errors = validateSubmit(md);
            if ( !errors.isEmpty() ) {
                return errorResponse(Response.Status.BAD_REQUEST, errors);
            }
            
            // if there's a FILE associated here, store it
            if ( null!=file && null!=fileInfo ) {
                // re-attach metadata to transaction in order to store the filename
                md = em.find(DOECodeMetadata.class, md.getCodeId());
                
                try {
                    String fileName = writeFile(file, md.getCodeId(), fileInfo.getFileName());
                    md.setFileName(fileName);
                } catch ( IOException e ) {
                    log.error ("File Upload Failed: " + e.getMessage());
                    return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "File upload failed.");
                }
            }
            
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
                    .status(Response.Status.OK)
                    .entity(mapper.createObjectNode().putPOJO("metadata", md.toJson()).toString())
                    .build();
        } catch ( NotFoundException e ) {
            return errorResponse(Response.Status.NOT_FOUND, e.getMessage());
        } catch ( IllegalAccessException e ) {
            log.warn("Persistence Error: Invalid owner update attempt: " + user.getEmail());
            log.warn("Message: " + e.getMessage());
            return errorResponse(Response.Status.FORBIDDEN, "Invalid Access: Unable to edit indicated record.");
        } catch ( IOException |  InvocationTargetException e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Persistence Error Publishing: " + e.getMessage());
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "IO Error submitting record.");
        } finally {
            em.close();
        }
    }
    
    /**
     * Support multipart-file upload POSTs to PUBLISH.
     * 
     * Response Codes:
     * 200 - OK, JSON returned of the metadata information published
     * 400 - validation error, errors returned in JSON
     * 401 - authentication is required to POST
     * 403 - access is forbidden to this record
     * 500 - file upload or database operation failed
     * 
     * @param metadata contains the JSON of the record metadata information
     * @param file the uploaded file to attach
     * @param fileInfo disposition information for the file name
     * @return a Response appropriate to the request status
     */
    @POST
    @Consumes (MediaType.MULTIPART_FORM_DATA)
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/publish")
    @RequiresAuthentication
    public Response publishFile(@FormDataParam("metadata") String metadata,
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileInfo) {
        return doPublish(metadata, file, fileInfo);
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
        return doPublish(object, null, null);
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
        return doSubmit(object, null, null);
    }
    
    /**
     * Perform SUBMIT workflow with associated file upload.
     * 
     * Response Codes:
     * 200 - OK, response includes metadata JSON
     * 400 - record validation failed, errors in JSON
     * 401 - Authentication is required to POST
     * 403 - Access is forbidden to this record
     * 500 - JSON parsing error or other unhandled exception
     * 
     * @param metadata the METADATA to SUBMIT
     * @param file a FILE to associate with this METADATA
     * @param fileInfo file disposition information for the FILE
     * @return a Response containing the metadata, or error information
     */
    @POST
    @Consumes (MediaType.MULTIPART_FORM_DATA)
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/submit")
    @RequiresAuthentication
    public Response submitFile(@FormDataParam("metadata") String metadata,
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileInfo) {
        return doSubmit(metadata, file, fileInfo);
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
        return doSave(object, null, null);
    }
    
    /**
     * POST a Metadata to be SAVED with a file upload.
     * 
     * @param metadata the JSON containing the Metadata information
     * @param file a FILE associated with this record
     * @param fileInfo file disposition information for the FILE
     * @return a Response containing the JSON of the metadata if successful, or
     * error information if not
     */
    @POST
    @Consumes (MediaType.MULTIPART_FORM_DATA)
    @Produces (MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    public Response save(@FormDataParam("metadata") String metadata,
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileInfo) {
        return doSave(metadata, file, fileInfo);
    }
    
    /**
     * Store a File to a specific directory location. All files associated with
     * a CODEID are stored in the same folder.
     * 
     * @param in the InputStream containing the file content
     * @param codeId the CODE ID associated with this file content
     * @param fileName the base file name of the file
     * @return the absolute filesystem path to the file
     * @throws IOException on IO errors
     */
    private static String writeFile(InputStream in, Long codeId, String fileName) throws IOException {
        // store this file in a designated base path
        java.nio.file.Path destination = 
                Paths.get(FILE_UPLOADS, String.valueOf(codeId), fileName);
        // make intervening folders if needed
        Files.createDirectories(destination.getParent());
        // save it
        Files.copy(in, destination);
        
        return destination.toString();
    }
    
    /**
     * Perform validations for PUBLISHED records.
     * 
     * @param m the Metadata information to validate
     * @return a List of error messages if any validation errors, empty if none
     */
    private static List<String> validatePublished(DOECodeMetadata m) {
        List<String> reasons = new ArrayList<>();
        Matcher matcher;
        
        if (null==m.getAccessibility())
            reasons.add("Missing Source Accessibility.");
        if (StringUtils.isBlank(m.getRepositoryLink()) && StringUtils.isBlank(m.getLandingPage()))
            reasons.add("Either a repository link or landing page is required.");
        if (StringUtils.isBlank(m.getSoftwareTitle()))
            reasons.add("Software title is required.");
        if (StringUtils.isBlank(m.getDescription()))
            reasons.add("Description is required.");
        if (null==m.getLicenses())
            reasons.add("A License is required.");
        if (null==m.getDevelopers() || m.getDevelopers().isEmpty())
            reasons.add("At least one developer is required.");
        else {
            for ( Developer developer : m.getDevelopers() ) {
                if ( StringUtils.isBlank(developer.getFirstName()) )
                    reasons.add("Developer missing first name.");
                if ( StringUtils.isBlank(developer.getLastName()) )
                    reasons.add("Developer missing last name.");
                if ( StringUtils.isNotBlank(developer.getEmail()) ) {
                    matcher = EMAIL_PATTERN.matcher(developer.getEmail());

                    if (!matcher.matches())
                        reasons.add("Developer email \"" + developer.getEmail() +"\" is not valid.");
                }
            }
        }
        if (StringUtils.isNotBlank(m.getDoi()) && null==m.getReleaseDate())
            reasons.add("Release Date is required for DOI registration.");
        return reasons;
    }
    
    /**
     * Perform SUBMIT validations on metadata.
     * 
     * @param m the Metadata to check
     * @return a List of submission validation errors, empty if none
     */
    private static List<String> validateSubmit(DOECodeMetadata m) {
        List<String> reasons = new ArrayList<>();
        Matcher matcher;
        
        // get all the PUBLISHED reasons, if any
        reasons.addAll(validatePublished(m));
        // add SUBMIT-specific validations
        if (null==m.getReleaseDate())
            reasons.add("Release date is required.");
        if (null==m.getSponsoringOrganizations() || m.getSponsoringOrganizations().isEmpty())
            reasons.add("At least one sponsoring organization is required.");
        else {
            for ( SponsoringOrganization o : m.getSponsoringOrganizations() ) {
                if (StringUtils.isBlank(o.getOrganizationName()))
                    reasons.add("Sponsoring organization name is required.");
                if (StringUtils.isBlank(o.getPrimaryAward()))
                    reasons.add("Primary award number is required.");
                else if (!Validation.isAwardNumberValid(o.getPrimaryAward()))
                    reasons.add("Award Number " + o.getPrimaryAward() + " is not valid.");
            }
        }
        if (null==m.getResearchOrganizations() || m.getResearchOrganizations().isEmpty())
            reasons.add("At least one research organization is required.");
        else {
            for ( ResearchOrganization o : m.getResearchOrganizations() ) {
                if (StringUtils.isBlank(o.getOrganizationName()))
                    reasons.add("Research organization name is required.");
            }
        }
        if (StringUtils.isBlank(m.getRecipientName()))
            reasons.add("Contact name is required.");
        if (StringUtils.isBlank(m.getRecipientEmail()))
            reasons.add("Contact email is required.");
        else {
            matcher = EMAIL_PATTERN.matcher(m.getRecipientEmail());
            if (!matcher.matches())
                reasons.add("Contact email is not valid.");
        }
        if (StringUtils.isBlank(m.getRecipientPhone()))
            reasons.add("Contact phone number is required.");
        else {
            matcher = PHONE_NUMBER_PATTERN.matcher(m.getRecipientPhone());
            if (!matcher.matches())
                reasons.add("Contact phone number is not valid.");
        }
        if (StringUtils.isBlank(m.getRecipientOrg()))
            reasons.add("Contact organization is required.");
        
        return reasons;
    }
}
