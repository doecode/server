/*
 */
package gov.osti.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Incoming JSON holding the Validation Request information.
 * 
 * Must be non-inner-class for Jackson.
 */
class ValidationRequest {
    private String value;
    private String[] validations;

    public ValidationRequest() {

    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the validations
     */
    public String[] getValidations() {
        return validations;
    }

    /**
     * @param validations the validations to set
     */
    public void setValidations(String[] validations) {
        this.validations = validations;
    }
}

/**
 * Private Class for Validation Responses.
 * 
 * Must be non-inner-class for Jackson purposes.
 * 
 * @author ensornl
 */
class ValidationResponse {
    private Boolean isValid;
    private String site;

    public ValidationResponse() {

    }
    
    /**
     * @return the isValid
     */
    public Boolean isValid() {
        return isValid;
    }

    /**
     * @param isValid the isValid to set
     */
    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }

    /**
     * @return the site
     */
    public String getSite() {
        return site;
    }

    /**
     * @param site the site to set
     */
    public void setSite(String site) {
        this.site = site;
    }
}

/**
 *
 * REST web services for validation purposes.
 * 
 * @author ensornl
 */
@Path("validation")
public class Validation {

    @Context ServletContext context;
    @Context HttpServletRequest request;

    // the Logger
    private static final Logger log = LoggerFactory.getLogger(Validation.class);
    // a JSON mapper
    private static final ObjectMapper mapper = new ObjectMapper();
    // static DOI resolution prefix
    private static final String DOI_BASE_URL = "https://doi.org/";
    
    /**
     * Creates a new instance of ValidationResource
     */
    public Validation() {
    }

    /**
     * Determine whether or not a contract number is valid.
     * 
     * Receive JSON: 
     * { "value":"value-to-validate",
     *   "validations:[ "rules", "to", "apply" ] }
     * 
     * Return:
     * 
     * { "errors":"error-message-if-any" }
     * 
     * Empty "errors" implies value was accepted and passes indicated validation
     * rule(s).  Currently supported "rules" are "Award" (validate award number)
     * and "DOI" (validate reachable DOI).
     * 
     * @param object the String containing JSON of the validation request
     * 
     * @return "errors" JSON Object if any; empty if accepted
     * 
     * @throws java.io.IOException on IO or HTTP client errors
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response request(String object) throws IOException {
        String apiHost = context.getInitParameter("api.host");
        String error_message = "";
        
        // set some reasonable default timeouts
        RequestConfig rc = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000).build();
        // create an HTTP client to request through
        CloseableHttpClient hc = 
                HttpClientBuilder
                .create()
                .setDefaultRequestConfig(rc)
                .build();

        try {
            ValidationRequest validationRequest = mapper.readValue(new StringReader(object), ValidationRequest.class);
            
            /**
             * Validations:
             * 
             * "DOI" -- ensure that DOI_BASE_URL + value is reachable via the internets
             * "Award" -- call known validation endpoint with value, check for "isValid" true response
             * 
             */
            for ( String validation : validationRequest.getValidations() ) {
                if ("DOI".equals(validation)) {
                    // for now, just try an HTTP connection via DOI_BASE_URL + value
                    HttpGet get = new HttpGet(DOI_BASE_URL + validationRequest.getValue());
                    HttpResponse response = hc.execute(get);

                    if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode())
                        error_message += validationRequest.getValue() + " is not a valid DOI.";
                } else if ("Award".equals(validation)) {
                    // ensure we're configured for that
                    if (null==apiHost) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                    // call the VALIDATION API to get a response
                    HttpGet get = new HttpGet(apiHost + "/api/contract/validate/" + validationRequest.getValue());
                    HttpResponse response = hc.execute(get);
                    // get the RESPONSE
                    ValidationResponse validationResponse = mapper.readValue(response.getEntity().getContent(), ValidationResponse.class);
                    
                    if (!validationResponse.isValid())
                        error_message += validationRequest.getValue() + " is not a valid Award Number.";
                } else {
                    log.warn("Invalid validation request type: " + validation);
                    return Response
                            .status(HttpStatus.SC_BAD_REQUEST)
                            .build();
                }
            }
            // at the end, return any error message
            return Response
                    .ok()
                    .entity(mapper.createObjectNode().put("errors", error_message).toString())
                    .build();
        } catch ( JsonParseException | JsonMappingException e ) {
            log.warn("Bad Request: " + object);
            log.warn("Message: " + e.getMessage());
            // send back a 400 BAD REQUEST
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .build();
        } finally {
            hc.close();
        }
    }
}
