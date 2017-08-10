/*
 */
package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

/**
 * Creates an API Error Response Object for reporting of issues.
 * 
 * Uses Fluent-style constructors:
 * 
 * ErrorResponse.create(Response.Status.BAD_REQUEST, "Message").build();
 * OR
 * ErrorResponse.create().status(400).message("Message").build();
 * 
 * Resulting Response JSON:
 * { "status":400, "errors":["Message"] }
 * 
 * for example.
 * @author ensornl
 */
public class ErrorResponse {
    // HTTP status code
    private int status;
    // list of errors, if applicable
    private List<String> errors = new ArrayList<>();
    
    // XML/JSON mapper reference
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    protected ErrorResponse() {
        
    }
    
    protected ErrorResponse(Response.Status status, String message) {
        this.status = status.getStatusCode();
        errors.add(message);
    }
    
    protected ErrorResponse(Response.Status s, List<String> messages) {
        this.status = s.getStatusCode();
        errors.addAll(messages);
    }
    
    /**
     * Instantiate an ErrorResponse Object to use.
     * @return an ErrorResponse
     */
    public static ErrorResponse create() {
        return new ErrorResponse();
    }
    
    /**
     * Instantiate and initialize an ErrorResponse.
     * @param s the Response.Status to use
     * @param message the error message
     * @return an ErrorResponse
     */
    public static ErrorResponse create(Response.Status s, String message) {
        return new ErrorResponse(s, message);
    }
    
    /**
     * Instantiate and initialize an ErrorResponse.
     * 
     * @param s the Response.Status to use
     * @param messages a List of error messages
     * @return an ErrorResponse
     */
    public static ErrorResponse create(Response.Status s, List<String> messages) {
        return new ErrorResponse(s, messages);
    }
    
    /**
     * Set the HTTP status code value directly.
     * 
     * @param code the HTTP status code to use for this ErrorResponse
     * @return the ErrorResponse
     */
    public ErrorResponse status(int code) {
        this.status = code;
        return this;
    }
    
    /**
     * Set the Response.Status value for response codes.
     * 
     * @param s the Response.Status to use
     * @return this ErrorResponse
     */
    public ErrorResponse status(Response.Status s) {
        this.status = s.getStatusCode();
        return this;
    }
    
    /**
     * Add a message to the ErrorResponse
     * 
     * @param message the error message to add
     * @return an ErrorResponse
     */
    public ErrorResponse message(String message) {
        errors.add(message);
        return this;
    }
    
    /**
     * Add a List of error messages to the ErrorResponse
     * @param messages a List of error messages to add
     * @return the ErrorResponse Object
     */
    public ErrorResponse messages(List<String> messages) {
        errors.addAll(messages);
        return this;
    }
    
    /**
     * Construct the appropriate HTTP Response from this ErrorResponse Object.
     * 
     * @return a Response in consistent JSON style if possible.
     */
    public Response build() {
        try {
        return Response
                .status(this.status)
                .entity(mapper.writeValueAsString(this))
                .build();
        } catch ( JsonProcessingException e ) {
            // fall back to Strings in plain text if JSON fails
            return Response
                    .status(getStatus())
                    .entity("Status: " + getStatus() + " Errors: " + StringUtils.join(errors, ", "))
                    .build();
        }
    }
    
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
