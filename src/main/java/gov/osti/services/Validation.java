/*
 */
package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import gov.osti.listeners.DoeServletContextListener;
import gov.osti.repository.GitRepository;
import gov.osti.repository.SubversionRepository;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.regex.Matcher;
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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final PhoneNumberUtil PHONE_NUMBER_VALIDATOR = PhoneNumberUtil.getInstance();
    // regular expressions for validating email addresses and URLs
    protected static final Pattern EMAIL_PATTERN = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
    protected static final Pattern URL_PATTERN = Pattern.compile("\\bhttps?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    protected static final Pattern DOI_PATTERN = Pattern.compile("10.\\d{4,9}/[-._;()/:A-Za-z0-9]+$");
    protected static final Pattern ORCID_PATTERN = Pattern.compile("(?i)^\\s*(?:(?:https?:\\/\\/)?orcid\\.org\\/)?(\\d{4}(?:-?\\d{4}){2}(?:-?\\d{3}[\\dX]))\\s*$");

    @JsonIgnoreProperties (ignoreUnknown = true)
    private static class ValidationRequest implements Serializable {
        private String type;
        private String value;
        private String error;

        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(String type) {
            this.type = type;
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
         * @return the error
         */
        public String getError() {
            return error;
        }

        /**
         * @param error the error to set
         */
        public void setError(String error) {
            this.error = error;
        }
    }
    
    /** 
     * Generates check digit as per ISO 7064 11,2. (from orcid.org)
     * @param value the value to check.
     * @return string the checksum digit of the value.
     */ 
    private static String generateCheckDigit(String value) { 
        int total = 0; 
        for (int i = 0; i < value.length(); i++) { 
            int digit = Character.getNumericValue(value.charAt(i)); 
            total = (total + digit) * 2; 
        } 
        int remainder = total % 11; 
        int result = (12 - remainder) % 11; 
        return result == 10 ? "X" : String.valueOf(result); 
    }
    
    /** 
     * Strip a string down to the known ORCID 16 digit value
     * @param value the value to format.
     * @return string the stripped down base 16 digits of the input ORCID, or null if value is invalid ORCID.
     */ 
    private static String stripBaseORCID(String value) {
        if (value == null)
            return null;
        
        // strip out valid 16 digit numeric
        Matcher matcher = ORCID_PATTERN.matcher(value);
        
        if (matcher.find())
            // return matching 16 digit characters
            return matcher.group(1).replaceAll("-", "").replaceAll("x", "X");
        else
            // no valid ORCID pattern
            return null;
    }
    
    /** 
     * Return a JSON of various formats of the ORCID value
     * @param value the value to format.
     * @return ObjectNode with three formats [base, dashed, url] of the value, or NULL if value is invalid ORCID.
     */ 
    private static ObjectNode formatORCID(String value) { 
        String baseValue = stripBaseORCID(value);
        
        ObjectNode theNode = mapper.createObjectNode();
        
        if (baseValue == null) {
            theNode.put("original", value);
            return theNode;
        }
        
        String dashedValue = String.format("%1$s-%2$s-%3$s-%4$s", baseValue.substring(0,4), baseValue.substring(4,8), baseValue.substring(8,12), baseValue.substring(12));
        
        theNode.put("base", baseValue);
        theNode.put("dashed", dashedValue);
        theNode.put("url", "https://orcid.org/" + dashedValue);        
        
        return theNode;
    }
    
    /** 
     * Return a specific format of the ORCID value
     * @param value the value to format.
     * @param format the specific format to return. Valid values [base, dashed, url].
     * @return string that matches the requested format, or NULL if value is invalid ORCID.
     */ 
    public static String formatORCID(String value, String format) { 
        ObjectNode theNode = formatORCID(value);     
        
        JsonNode originalValue = theNode.get("original");
        
        // if value is invalid, we get back the original, and if it is NULL, return NULL.
        if (originalValue != null && originalValue.isNull())
            return null;
        
        // otherwise, return the value of the original, or the formatted value.
        return (theNode.isNull()) ? null : (originalValue != null ? theNode.get("original").asText() : theNode.get(format).asText());
    }

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
     * No longer presumes to add protocol; it must be provided to be a proper
     * URL value.
     *
     * @param value the VALUE to check
     * @return true if appears to be a URL, false if not
     */
    public static boolean isValidUrl(String value) {
        return ( null==value ) ?
                false :
                URL_PATTERN.matcher(value).matches();
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
                PhoneNumber number = PHONE_NUMBER_VALIDATOR.parse(value, "US");
                return PHONE_NUMBER_VALIDATOR.isValidNumber(number);
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
     * Supports: git and subversion repository types that are publically
     * available via HTTP(s).
     *
     * @param value the repository link/URL to check
     * @return true if valid, false if not
     */
    public static boolean isValidRepositoryLink(String value) {
        if ( StringUtils.isBlank(value))
            return false;

        // check what we consider "valid" for repository info
        return (GitRepository.isValid(value) || 
                SubversionRepository.isValid(value));
    }

    /**
     * Check to see if DOI is valid or not.  Matches the pattern of a DOI URL, or
     * bare DOI value.
     *
     * @param value the DOI to check
     * @return true if the DOI is a valid pattern; false if not
     */
    public static boolean isValidDoi(String value) {
        // make sure the value, if present, conforms to DOI pattern.
        return (null==value) ? false : DOI_PATTERN.matcher(value).find();
    }

    /**
     * Check to see if ORCID is valid or not.  Matches the pattern of a ORCID, and
     * has a valid checksum.
     *
     * @param value the ORCID to check
     * @return true if the ORCID is a valid pattern and passes checksum; false if not
     */
    public static boolean isValidORCID(String value) {
        value = stripBaseORCID(value);
        if (value == null)
            return false;
        
        // 16 digits found, now validate checksum
        String lastChar = (value.substring(value.length() - 1));
        String baseValue = (value.substring(0, value.length() - 1));
        
        return lastChar.equals(generateCheckDigit(baseValue));
    }

    /**
     * View the API documentation.
     *
     * @return a Viewable to the documentation
     */
    @GET
    @Produces (MediaType.TEXT_HTML)
    public Viewable getDocumentation() {
        return new Viewable ("/validation");
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
                Response.ok().entity(mapper.createObjectNode().put("value", "OK").toString()).build() :
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
                Response.ok().entity(mapper.createObjectNode().put("value", "OK").toString()).build() :
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
                Response.ok().entity(mapper.createObjectNode().put("value", "OK").toString()).build() :
                ErrorResponse.badRequest("\"" + value + "\" is not a valid DOI.").build();
    }

    /**
     * Check a ORCID.
     *
     * Response Codes:
     * 200 - OK, value is valid
     * 400 - Bad Request, value is NOT valid
     *
     * @param value the ORCID to check
     * @return a Response
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/orcid")
    public Response checkORCID(@QueryParam("value") String value) {        
        ObjectNode theNode = mapper.createObjectNode();
        
        if (isValidORCID(value)) {
            theNode.put("value", "OK");
            theNode.set("format", formatORCID(value));
            
            return Response.ok().entity(theNode.toString()).build();
        }
        else
            return ErrorResponse.badRequest("\"" + value + "\" is not a valid ORCID.").build();
    }

    /**
     * Check an email address.
     *
     * Response Codes:
     * 200 - OK, value is valid
     * 400 - Bad Request, value is NOT valid
     *
     * @param value the email address to check
     * @return a Response
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/email")
    public Response checkEmail(@QueryParam("value") String value) {
        return ( isValidEmail(value) ) ?
                Response.ok().entity(mapper.createObjectNode().put("value", "OK").toString()).build() :
                ErrorResponse.badRequest("\"" + value + "\" is not a valid email address.").build();
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
                Response.ok().entity(mapper.createObjectNode().put("value", "OK").toString()).build() :
                ErrorResponse.badRequest(generateURLErrorMsg(value, "repositorylink")).build();
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
                Response.ok().entity(mapper.createObjectNode().put("value", "OK").toString()).build() :
                ErrorResponse.badRequest(generateURLErrorMsg(value, "url")).build();
    }

    /**
     * Determine whether or not a contract number is valid.
     *
     * Receive JSON:
     * [ { "value":"value1", "type":"type1" }, {"value":"value2", "type":"type2"} ]
     *
     * Return:
     *
     * [ { "value":"value1", "type":"type1", "error":"Not a valid type1"}, ...]
     *
     * Empty "error messages" implies value was accepted and passes indicated validation
     * rule(s).
     *
     * Types supported (NOT case-sensitive): "doi", "awardnumber", "phonenumber",
     * "email", "url", "repositorylink".
     *
     * @param object the String containing JSON of the validation request
     *
     * @return JSON containing records with "error" messages with each
     *
     * @throws java.io.IOException on IO or HTTP client errors
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response request(String object) throws IOException {
        try {
            ValidationRequest[] requests = mapper.readValue(object, ValidationRequest[].class);

            /**
             * Validations:
             *
             * "DOI" -- ensure that DOI_BASE_URL + value is reachable via the internets
             * "Award" -- call known validation endpoint with value, check for "isValid" true response
             *
             */
            for ( ValidationRequest req : requests ) {
                if (StringUtils.equalsIgnoreCase(req.getType(), "doi")) {
                    req.setError((isValidDoi(req.getValue()) ? "" : req.getValue() + " is not a valid DOI."));
                } else if (StringUtils.equalsIgnoreCase(req.getType(), "repositorylink")) {
                     req.setError(isValidRepositoryLink(req.getValue()) ? "" : generateURLErrorMsg(req.getValue(), req.getType()));
                } else if (StringUtils.equalsIgnoreCase(req.getType(), "phonenumber")) {
                    req.setError((isValidPhoneNumber(req.getValue()) ? "" : req.getValue() + " is not a valid phone number."));
                } else if (StringUtils.equalsIgnoreCase(req.getType(), "url")) {
                     req.setError(isValidUrl(req.getValue()) ? "" : generateURLErrorMsg(req.getValue(), req.getType()));
                } else if (StringUtils.equalsIgnoreCase(req.getType(), "email")) {
                    req.setError((isValidEmail(req.getValue()) ? "" : req.getValue() + " is not a valid email address."));
                } else if (StringUtils.equalsIgnoreCase(req.getType(), "awardnumber")) {
                    req.setError((isValidAwardNumber(req.getValue()) ? "" : req.getValue() + " is not a valid Award Number."));
                } else if (StringUtils.equalsIgnoreCase(req.getType(), "orcid")) {
                    boolean isValid = isValidORCID(req.getValue());
                    req.setError((isValid ? "" : req.getValue() + " is not a valid ORCID."));
                    req.setValue((isValid ? req.getValue() : formatORCID(req.getValue(), "dashed")));
                } else {
                    log.warn("Unknown validation request type: " + req.getType());
                    return ErrorResponse
                            .badRequest("Unknown request type: " + req.getType())
                            .build();
                }
            }
            // at the end, return any error message
            return Response
                    .ok()
                    .entity(mapper.valueToTree(requests).toString())
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
    
    private String generateURLErrorMsg(String url, String type) {
         String safeUrl = (null==url) ? "" : url.trim();
         String msg = "";

         if (!safeUrl.startsWith("http"))
              msg = "Please include a correctly associated prefix with the URL (e.g., \"http://\" or \"https://\").";
         else {
              if (type.equalsIgnoreCase("repositorylink"))
                   msg = "Not a valid repository link.";
              else
                   msg = safeUrl + " is not a valid URL.";
         }
         
         return msg;
    }
}
