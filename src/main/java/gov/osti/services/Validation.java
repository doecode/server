/*
 */
package gov.osti.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import gov.osti.listeners.DoeServletContextListener;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
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
    
    // Phone number validation
    private static PhoneNumberUtil phoneNumberValidator = PhoneNumberUtil.getInstance();
    // regular expressions for validating email addresses and URLs
    protected static final Pattern EMAIL_PATTERN = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
    protected static final Pattern URL_PATTERN = Pattern.compile("\\bhttps?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    protected static final Pattern DOI_PATTERN = Pattern.compile("10.\\d{4,9}/[-._;()/:A-Za-z0-9]+$");
    
    /**
     * Creates a new instance of ValidationResource
     */
    public Validation() {
    }
    
    /**
     * Determine whether or not this VALUE conforms to an EMAIL ADDRESS pattern.
     * 
     * @param value the value to check
     * @return true if matches an EMAIL ADDRESS pattern, false if not
     */
    public static boolean isValidEmail(String value) {
        return ( null==value ) ?
                false :
                EMAIL_PATTERN.matcher(value).matches();
    }
    
    /**
     * Check to see if a VALUE appears to be a valid URL.
     * 
     * If no "http" prefix found, add one and try that value.
     * 
     * @param value the VALUE to check
     * @return true if appears to be a URL, false if not
     */
    public static boolean isValidUrl(String value) {
        return ( null==value ) ?
                false :
                (value.toLowerCase().startsWith("http")) ?
                URL_PATTERN.matcher(value).matches() :
                URL_PATTERN.matcher("http://"+value).matches();
    }
    
    /**
     * Determine whether or not the phone number is valid, defaulting to US.
     * 
     * @param value the PHONE NUMBER
     * @return true if a valid number, false if not
     */
    public static boolean isValidPhoneNumber(String value) {
        try {
            if (null!=value) {
                PhoneNumber number = phoneNumberValidator.parse(value, "US");
                return phoneNumberValidator.isValidNumber(number);
            }
        } catch ( NumberParseException e ) {
            log.warn("Phone Number error: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Make an external validation call for a CONTRACT NUMBER for validity.
     * If unable to check, not configured properly, or an error occurs, assume
     * FALSE.
     * 
     * @param value the CONTRACT/AWARD NUMBER to check
     * @return true if valid, false if not
     */
    public static boolean isValidAwardNumber(String value) {
        RequestConfig rc = RequestConfig
                .custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
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
     * Determine whether or not the passed-in value is a VALID repository link.
     * 
     * Presently, valid means a remote-accessible HTTP(S)-based git repository.
     * 
     * @param value the repository link/URL to check
     * @return true if valid, false if not
     */
    public static boolean isValidRepositoryLink(String value) {
        if ( StringUtils.isBlank(value))
            return false;
        
        // if not starting with HTTP, make it so then test
        if (!value.toLowerCase().startsWith("http"))
            value = "http://" + value;
        
        try {
            Collection<Ref> references = Git
                    .lsRemoteRepository()
                    .setHeads(true)
                    .setTags(true)
                    .setRemote(value)
                    .call();
            
            // must be a valid repository if it has references
            return true;
        } catch ( Exception e ) {
            // jgit occasionally throws sloppy runtime exceptions
            log.warn("Repository URL " + value + " failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check to see if DOI is valid or not.
     * 
     * @param value the DOI to check
     * @return true if the DOI is valid and reachable; false if not
     */
    public static boolean isValidDoi(String value) {
        // set some reasonable default timeouts
        // create an HTTP client to request through
        CloseableHttpClient hc = 
                HttpClientBuilder
                .create()
                .setDefaultRequestConfig(RequestConfig
                        .custom()
                        .setSocketTimeout(5000)
                        .setConnectTimeout(5000)
                        .setConnectionRequestTimeout(5000)
                        .build())
                .build();
        
        try {
            // if value is missing or doesn't appear to be a DOI, don't bother
            if (null==value || !DOI_PATTERN.matcher(value).matches())
                return false;
            // for now, just try an HTTP connection via DOI_BASE_URL + value
            HttpGet get = new HttpGet(DOI_BASE_URL + URLEncoder.encode(value.trim(), "UTF-8"));
            HttpResponse response = hc.execute(get);
            
            // URL found? OK
            return (HttpStatus.SC_OK==response.getStatusLine().getStatusCode());
        } catch ( IOException e ) { 
            log.warn("IO Error Checking DOI: " + value, e);
            return false;
        } finally {
            try {
                hc.close();
            } catch ( IOException e ) {
                log.warn("Close Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Determine whether or not the PHONE NUMBER is valid.
     * 
     * Response Codes: 
     * 200 - OK, value is valid
     * 400 - Bad Request, value is NOT valid
     * 
     * @param value a PHONE NUMBER to check
     * @return a Response containing validity
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/phonenumber")
    public Response checkPhoneNumber(@QueryParam("value") String value) {
        return ( isValidPhoneNumber(value) ) ?
                Response.ok().build() :
                ErrorResponse.badRequest("\"" + value + "\" is not a valid phone number.").build();
    }
    
    /**
     * Check an AwardNumber for valid DOE contract number value.
     * 
     * Response Codes: 
     * 200 - OK, value is valid
     * 400 - Bad Request, value is NOT valid
     * 
     * @param value the AWARD NUMBER
     * @return a Response containing validity
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/awardnumber")
    public Response checkAwardNumber(@QueryParam("value") String value) {
        return ( isValidAwardNumber(value) ) ?
                Response.ok().build() :
                ErrorResponse.badRequest("\"" + value + "\" is not a valid award number.").build();
    }
    
    /**
     * Check a DOI.
     * 
     * Response Codes: 
     * 200 - OK, value is valid
     * 400 - Bad Request, value is NOT valid
     * 
     * @param value the DOI to check
     * @return a Response
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/doi")
    public Response checkDoi(@QueryParam("value") String value) {
        return ( isValidDoi(value) ) ?
                Response.ok().build() :
                ErrorResponse.badRequest("\"" + value + "\" is not a valid DOI.").build();
    }
    
    /**
     * Check a REPOSITORY LINK value.
     * 
     * Response Codes: 
     * 200 - OK, value is valid
     * 400 - Bad Request, value is NOT valid
     * 
     * @param value a REPOSITORY LINK to check
     * @return a Response containing whether or not this was valid
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/repositorylink")
    public Response checkRepositoryLink(@QueryParam("value") String value) {
        return ( isValidRepositoryLink(value) ) ?
                Response.ok().build() :
                ErrorResponse.badRequest("\"" + value + "\" is not a valid repository link.").build();
    }
    
    /**
     * Check a URL value.
     * 
     * Response Codes: 
     * 200 - OK, value is valid
     * 400 - Bad Request, value is NOT valid
     * 
     * @param value a URL value to check
     * @return a Response containing validity
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/url")
    public Response checkUrl(@QueryParam("value") String value) {
        return ( isValidUrl(value) ) ?
                Response.ok().build() :
                ErrorResponse.badRequest("\"" + value + "\" is not a valid URL.").build();
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
                    if ("DOI".equalsIgnoreCase(validation)) {
                        validationResponse.add((isValidDoi(value)) ? "" : value + " is not a valid DOI.");
                    } else if ("Award".equalsIgnoreCase(validation)) {
                        // ensure we're configured for that
                        if (StringUtils.isBlank(API_HOST)) {
                            return Response.status(Response.Status.NOT_FOUND).build();
                        }
                        // call the VALIDATION API to get a response
                        validationResponse.add((isValidAwardNumber(value)) ? "" : value + " is not a valid Award Number.");
                    } else if ("RepositoryLink".equalsIgnoreCase(validation)) {
                        // ensure this repository link value IS a valid git repository
                        validationResponse.add((isValidRepositoryLink(value)) ? "" : value + " is not a valid repository link.");
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
        }
    }
}
