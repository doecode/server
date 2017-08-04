/*
 */
package gov.osti.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.osti.listeners.DoeServletContextListener;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
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
    private String[] values;
    private String[] validations;

    public ValidationRequest() {

    }

    /**
     * Acquire the set of VALUES to validate.
     * 
     * @return the set of Values to check
     */
    public String[] getValues() {
        return values;
    }

    /**
     * Request a set of String VALUES to validate
     * @param values the values to set
     */
    public void setValues(String[] values) {
        this.values = values;
    }

    /**
     * A String set of validations to apply to the values
     * @return the validations to check
     */
    public String[] getValidations() {
        return validations;
    }

    /**
     * Set the type of validations to perform
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
class ApiResponse {
    private Boolean isValid;
    private String site;

    public ApiResponse() {

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
 * Set of "error message" responses for the indicated validations.
 * 
 * A simple Class for easier Jackson serialization, and future extension should
 * errors require more information or resolution.
 * 
 * @author ensornl
 */
class ValidationResponse {
    // set of errors; empty String means "ok"
    private List<String> errors = new ArrayList<>();
    
    public ValidationResponse() {
        
    }
    
    /**
     * Get the List of error messages.
     * @return an ArrayList of String error messages
     */
    public List<String> getErrors() {
        return errors;
    }
    
    /**
     * Add an "error message"; blank means validation passed.
     * @param message the message to add
     */
    public void add(String message) {
        errors.add(message);
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
    // API host for servicing external validation calls
    private static final String API_HOST = DoeServletContextListener.getConfigurationProperty("api.host");
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
     * Make an external validation call for a CONTRACT NUMBER for validity.
     * If unable to check, not configured properly, or an error occurs, assume
     * FALSE.
     * 
     * @param value the CONTRACT/AWARD NUMBER to check
     * @return true if valid, false if not
     */
    public static boolean isAwardNumberValid(String value) {
        RequestConfig rc = RequestConfig
                .custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .build();
        CloseableHttpClient hc = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(rc)
                .build();
        
        try {
            // if not configured, abort
            if (StringUtils.isBlank(API_HOST))
                return false;
            
            // call the VALIDATION API to get a response
            HttpGet get = new HttpGet(API_HOST + "/contract/validate/" + URLEncoder.encode(value.trim(), "UTF-8"));
            HttpResponse response = hc.execute(get);
            // get the RESPONSE
            ApiResponse apiResponse = mapper.readValue(response.getEntity().getContent(), ApiResponse.class);
            
            return apiResponse.isValid();
        } catch ( IOException e ) { 
            log.warn("Error checking " + value + ": " + e.getMessage());
        } finally {
            try {
                hc.close();
            } catch ( IOException e ) {
                log.warn("IOException Checking contract number " + value + ": " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Determine whether or not a contract number is valid.
     * 
     * Receive JSON: 
     * { "values":["value1", "value2", ...],
     *   "validations:[ "rules", "to", "apply" ] }
     * 
     * Return:
     * 
     * { "errors":["value1-error-message", "value2-error-message", ... ] }
     * 
     * Empty "error messages" implies value was accepted and passes indicated validation
     * rule(s).  Currently supported "rules" are "Award" (validate award number)
     * and "DOI" (validate reachable DOI).  There should exist one output error
     * message per incoming value, in the order received.
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
        ValidationResponse validationResponse = new ValidationResponse();
        
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
            for ( String value : validationRequest.getValues() ) {
                // apply each validation rule to each value to validate
                for ( String validation : validationRequest.getValidations() ) {
                    if ("DOI".equals(validation)) {
                        // for now, just try an HTTP connection via DOI_BASE_URL + value
                        HttpGet get = new HttpGet(DOI_BASE_URL + URLEncoder.encode(value.trim(), "UTF-8"));
                        HttpResponse response = hc.execute(get);

                        // add empty String for no error, or error message if not found
                        validationResponse.add( HttpStatus.SC_OK==response.getStatusLine().getStatusCode() ?
                                "" :
                                value + " is not a valid DOI.");
                    } else if ("Award".equals(validation)) {
                        // ensure we're configured for that
                        if (StringUtils.isBlank(API_HOST)) {
                            return Response.status(Response.Status.NOT_FOUND).build();
                        }
                        // call the VALIDATION API to get a response
                        validationResponse.add((isAwardNumberValid(value)) ? "" : value + " is not a valid Award Number.");
                    } else {
                        log.warn("Invalid validation request type: " + validation);
                        return Response
                                .status(HttpStatus.SC_BAD_REQUEST)
                                .build();
                    }
                }
            }
            // at the end, return any error message
            return Response
                    .ok()
                    .entity(mapper.valueToTree(validationResponse).toString())
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
