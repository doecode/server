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
import java.util.TimeZone;
import javax.ws.rs.core.MediaType;
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
 * 
 * Includes several convenience constructor methods for common error responses,
 * such as notFound(), forbidden(), etc.
 * 
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
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setTimeZone(TimeZone.getDefault());;

    protected ErrorResponse() {
        
    }
    
    protected ErrorResponse(int code) {
        this.status = code;
    }
    
    protected ErrorResponse(Response.Status status) {
        this.status = status.getStatusCode();
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
     * Construct an empty BAD REQUEST response.
     * 
     * @return an ErrorResponse of BAD REQUEST (400)
     */
    public static ErrorResponse badRequest() {
        return new ErrorResponse (Response.Status.BAD_REQUEST);
    }
    
    /**
     * Create a BAD REQUEST with a single message.
     * 
     * @param message the error message
     * @return an ErrorResponse of BAD REQUEST
     */
    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse (Response.Status.BAD_REQUEST, message);
    }
    
    /**
     * Create a BAD REQUEST error message with a list of messages.
     * @param messages the error messages
     * @return an ErrorResponse of BAD REQUEST
     */
    public static ErrorResponse badRequest(List<String> messages) {
        return new ErrorResponse (Response.Status.BAD_REQUEST, messages);
    }
    
    /**
     * Create an INTERNAL SERVER ERROR response.
     * 
     * @return an empty INTERNAL SERVER ERROR response
     */
    public static ErrorResponse internalServerError() {
        return new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Create an INTERNAL SERVER ERROR response with a message.
     * 
     * @param message the error message
     * @return an ErrorResponse of INTERNAL SERVER ERROR
     */
    public static ErrorResponse internalServerError(String message) {
        return new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
    
    /**
     * Create an INTERNAL SERVER ERROR response with a List of messages.
     * @param messages the error messages
     * @return an ErrorResponse of INTERNAL SERVER ERROR
     */
    public static ErrorResponse internalServerError(List<String> messages) {
        return new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, messages);
    }
    
    /**
     * Create a FORBIDDEN error response
     * @return a FORBIDDEN error response
     */
    public static ErrorResponse forbidden() {
        return new ErrorResponse(Response.Status.FORBIDDEN);
    }
    
    /**
     * Create a FORBIDDEN error response with a message
     * @param message the error message
     * @return a FORBIDDEN error response
     */
    public static ErrorResponse forbidden(String message) {
        return new ErrorResponse(Response.Status.FORBIDDEN, message);
    }
    
    /**
     * Create a FORBIDDEN error response with a List of messages
     * @param messages the error messages
     * @return a FORBIDDEN error response
     */
    public static ErrorResponse forbidden(List<String> messages) {
        return new ErrorResponse(Response.Status.FORBIDDEN, messages);
    }
    
    /**
     * Create an UNAUTHORIZED error response
     * @return an UNAUTHORIZED error response
     */
    public static ErrorResponse unauthorized() {
        return new ErrorResponse(Response.Status.UNAUTHORIZED);
    }
    
    /**
     * Create an UNAUTHORIZED error response with a message
     * @param message the error message
     * @return an UNAUTHORIZED error response
     */
    public static ErrorResponse unauthorized(String message) {
        return new ErrorResponse (Response.Status.UNAUTHORIZED, message);
    }
    
    /**
     * Create an UNAUTHORIZED error response with a List of messages
     * @param messages the error messages
     * @return an UNAUTHORIZED error response
     */
    public static ErrorResponse unauthorized(List<String> messages) {
        return new ErrorResponse (Response.Status.UNAUTHORIZED, messages);
    }
    
    /**
     * Create a NOT FOUND error response
     * @return a NOT FOUND error response
     */
    public static ErrorResponse notFound() {
        return new ErrorResponse(Response.Status.NOT_FOUND);
    }
    
    /**
     * Create a NOT FOUND error response with a message
     * @param message the error message
     * @return a NOT FOUND error response
     */
    public static ErrorResponse notFound(String message) {
        return new ErrorResponse (Response.Status.NOT_FOUND, message);
    }
    
    /**
     * Create a NOT FOUND error response with a List of messages
     * @param messages a List of error messages
     * @return a NOT FOUND error response
     */
    public static ErrorResponse notFound(List<String> messages) {
        return new ErrorResponse (Response.Status.NOT_FOUND, messages);
    }
    
    /**
     * Instantiate and initialize an ErrorResponse.
     * @param s the Response.Status to use
     * @param message the error message
     * @return an ErrorResponse
     */
    public static ErrorResponse status(Response.Status s, String message) {
        return new ErrorResponse(s, message);
    }
    
    /**
     * Instantiate and initialize an ErrorResponse.
     * 
     * @param s the Response.Status to use
     * @param messages a List of error messages
     * @return an ErrorResponse
     */
    public static ErrorResponse status(Response.Status s, List<String> messages) {
        return new ErrorResponse(s, messages);
    }
    
    /**
     * Set the HTTP status code value directly.
     * 
     * @param code the HTTP status code to use for this ErrorResponse
     * @return the ErrorResponse
     */
    public static ErrorResponse status(int code) {
        return new ErrorResponse(code);
    }
    
    /**
     * Set the Response.Status value for response codes.
     * 
     * @param s the Response.Status to use
     * @return this ErrorResponse
     */
    public ErrorResponse status(Response.Status s) {
        return new ErrorResponse(s);
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
                .header("Content-Type", MediaType.APPLICATION_JSON)
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
