package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import gov.osti.entity.Site;

import gov.osti.entity.User;
import gov.osti.listeners.DoeServletContextListener;
import gov.osti.security.DOECodeCrypt;
import io.jsonwebtoken.Claims;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.TypedQuery;
import javax.ws.rs.NotFoundException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.glassfish.jersey.server.mvc.Viewable;

@Path("user")
public class UserServices {
    private static Logger log = LoggerFactory.getLogger(UserServices.class);
    private static final PasswordService PASSWORD_SERVICE = new DefaultPasswordService();

    // SMTP email host name
    private static final String EMAIL_HOST = DoeServletContextListener.getConfigurationProperty("email.host");
    // base SITE URL for the front-end
    private static final String SITE_URL = DoeServletContextListener.getConfigurationProperty("site.url");
    // EMAIL send-from account name
    private static final String EMAIL_FROM = DoeServletContextListener.getConfigurationProperty("email.from");

    public UserServices() {

    }

    /**
     * Determine whether or not this password is kosher.
     * 
     * Rules:
     * 1. at least 8 characters
     * 2. contains 1 special and 1 number character
     * 3. NOT the email address (may not CONTAIN it, any case)
     * 4. mix of upper and lower case letters
     * 
     * @param email the EMAIL ADDRESS (login name)
     * @param password the PASSWORD to validate
     * @return true if password is acceptable, false if not
     */
    protected static boolean validatePassword(String email, String password) {
        // define what's a special character
        String specialCharacters = "!@#$%^&*()_",
               specialPattern = ".*[" + Pattern.quote(specialCharacters) + "].*";
        
        // must be at least 8 characters (also null protection)
        if ( StringUtils.length(password)<8 || null==password )
            return false;
        // cannot be equivalent to (or contain?) the email address
        if ( StringUtils.containsIgnoreCase(password, email) )
            return false;
        // must contain at least 1 special character
        if (!password.matches(specialPattern))
            return false;
        // must contain mix of upper and lower characters
        if (password.equals(password.toLowerCase()))
            return false;
        if (password.equals(password.toUpperCase()))
            return false;
        
        // must be OK
        return true;
    }
    
    /**
     * View the API documentation.
     * 
     * @return a Viewable to the documentation
     */
    @GET
    @Produces (MediaType.TEXT_HTML)
    public Viewable getDocumentation() {
        return new Viewable ("/userservices");
    }

    /**
     * Endpoint to determine whether or not a Session/user exists and is logged in.
     * 
     * @return an OK Response if session is logged in, otherwise a FORBIDDEN or
     * UNAUTHENTICATED response as appropriate
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    @Path ("/authenticated")
    @RequiresAuthentication
    public Response isAuthenticated() {
        // return an OK if authenticated, otherwise authentication services will handle status
        return Response
                .status(Response.Status.OK)
                .entity(mapper.createObjectNode().put("status", "success").toString())
                .build();
    }

    /**
     * Endpoint that returns user email
     * 
     * @return an OK Response if session is logged in, otherwise a FORBIDDEN or
     * UNAUTHENTICATED response as appropriate
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_JSON)
    @Path ("/load")
    @RequiresAuthentication
    public Response load() {
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();

        // return an OK if authenticated, otherwise authentication services will handle status
        return Response
                .status(Response.Status.OK)
                .entity(mapper.createObjectNode().put("email", user.getEmail()).toString())
                .build();
    }
    
    /**
     * Process a "log out" request.
     * 
     * @return an OK Response
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes ({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    @Path ("/logout")
    public Response logout() {
        Subject subject = SecurityUtils.getSubject();
        
        if (null!=subject) subject.logout();
        
        return Response
                .ok()
                .cookie(DOECodeCrypt.invalidateCookie())
                .entity(mapper.createObjectNode().put("status", "success").toString())
                .build();
    }

    /**
     * Process login requests.
     * 
     * JSON includes email address and password OR confirmation_code
     * JWT value.  The latter is to support forgot-my-password functionality for
     * a one-time login request, as token is reset after success.
     * 
     * Response Codes:
     * 200 - login OK, sets token and cookie
     * 401 - authentication failed
     * 403 - forbidden access
     * 500 - internal system error, unable to read JSON, etc.
     * 
     * JSON returns email address, XSRF token, hasSite indiciating whether or not
     * user is a lab site user, and first/last names if present.
     * 
     * @param object JSON containing "email" and "password" to authenticate.
     * @return an appropriate Response based on whether or not authentication succeeded
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_JSON)
    @Path ("/login")
    public Response login(String object) {
        LoginRequest request;
	try {
            request = mapper.readValue(object, LoginRequest.class);
	} catch (IOException e) {
            // TODO Auto-generated catch block
            log.warn("JSON Mapper error: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("Error processing request.")
                    .build();
	}
        // a User object to use
        User user = null;
        
        // is this a typical username + password login attempt?
        if (null!=request.getEmail() && null!=request.getPassword()) {
            // attempt to look up the user
            user = findUserByEmail(request.getEmail());

            // ensure the user exists and is verified / active
            if (null==user || !user.isVerified() || !user.isActive())
                return ErrorResponse
                    .unauthorized()
                    .build();
            
            // if account password is expired, we need to inform the user
            if (user.isPasswordExpired()) {
                sendPasswordExpiredEmail(request.getEmail());
                return ErrorResponse
                        .unauthorized()
                        .build();
            }

            // ensure the PASSWORD matches, or implement three-strikes failure policy
            if (!PASSWORD_SERVICE.passwordsMatch(request.getPassword(), user.getPassword())) {
                log.warn("Password mismatch for " + request.getEmail());

                // implement three-strikes rule
                processUserLogin(request.getEmail(), true);
                // inform the user of the failure in generic terms
                return ErrorResponse
                        .unauthorized()
                        .build();
            }
            // success, set the failure count to zero
            processUserLogin(request.getEmail(), false);
        } else if (null!=request.getConfirmationCode()) {
            // check the CONFIRMATION CODE token for the EMAIL + CODE
            Claims claims = DOECodeCrypt.parseJWT(request.getConfirmationCode());
            String confirmationCode = claims.getId();
            String email = claims.getSubject();

            user = findUserByEmail(email);

            // user MUST be active and verified, and exist
            if (null==user || !user.isActive() || !user.isVerified())
                return ErrorResponse
                        .unauthorized()
                        .build();

            // if the EMAIL ADDRESS doesn't match the requesting user, it's an error
            if (!user.getEmail().equals(email))
                return ErrorResponse
                        .forbidden("Confirmation code invalid.")
                        .build();

            // ensure the confirmation codes match
            if (!StringUtils.equals(user.getConfirmationCode(), confirmationCode))
                return ErrorResponse
                        .forbidden("Confirmation code invalid.")
                        .build();

            // if successful, we need to CLEAR OUT the user TOKEN
            resetUserToken(email);
        } else {
            // no username+password OR confirmation code, bad request
            return ErrorResponse
                    .badRequest("Invalid login request.")
                    .build();
        }
    
	String xsrfToken = DOECodeCrypt.nextRandomString();
	String accessToken = DOECodeCrypt.generateLoginJWT(user.getApiKey(), xsrfToken);
	NewCookie cookie = DOECodeCrypt.generateNewCookie(accessToken);
	
        return Response
                .status(Response.Status.OK)
                .entity(mapper
                        .createObjectNode()
                        .put("xsrfToken", xsrfToken)
                        .put("hasSite", !"CONTR".equals(user.getSiteId()))
                        .put("email", user.getEmail())
                        .put("first_name", user.getFirstName())
                        .put("last_name", user.getLastName())
                        .toString())
                .cookie(cookie)
                .build();
    }
    
    /**
     * Query to determine SITE CODE based on EMAIL DOMAIN values.
     * 
     * Response Codes:
     * 200 - OK, JSON contains EMAIL ADDRESS and SITE CODE (or "CONTR")
     * 400 - Bad Request, missing required EMAIL
     * 500 - Internal service error
     * 
     * @param object JSON containing the "email" to query
     * @return a Response containing the JSON if found
     */
    @POST
    @Consumes (MediaType.APPLICATION_JSON)
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/getsitecode")
    public Response getSiteCode(String object) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        EmailRequest request;
        
        try {
            request = mapper.readValue(object, EmailRequest.class);
        } catch ( IOException e ) {
            return ErrorResponse
                    .internalServerError("Unable to process request.")
                    .build();
        }
        
        // no email?
        if (StringUtils.isBlank(request.getEmail()))
            return ErrorResponse
                    .badRequest("Missing required email.")
                    .build();
        
        try {
            // assign as SITE if possible based on the EMAIL, or default to CONTRACTOR
            String domain = request.getEmail().substring(request.getEmail().indexOf("@"));
            TypedQuery<Site> query = em.createQuery("SELECT s FROM Site s join s.emailDomains d WHERE d = :domain", Site.class);
            query.setParameter("domain", domain);

            // look up the Site and set CODE, or CONTR if not found
            List<Site> sites = query.getResultList();
            String siteCode = ((sites.isEmpty()) ? "CONTR" : sites.get(0).getSiteCode());
            
            // return the results back
            return Response
                    .ok()
                    .entity(mapper
                            .createObjectNode()
                            .put("email", request.getEmail())
                            .put("site_code", siteCode).toString())
                    .build();
        } catch ( Exception e ) {
            log.error("Site Lookup Error", e);
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }

    /**
     * Process a registration request.
     * 
     * JSON should include an EMAIL ADDRESS, requested PASSWORD (and CONFIRMATION),
     * and optionally a FIRST and LAST name.  If a Contractor, should also include
     * a CONTRACT NUMBER; this will be validated and required at the confirmation
     * stage prior to verification.
     * 
     * Response Codes:
     * 200 - OK, confirmation code is sent to the email address indicated
     * 400 - Bad request, missing required EMAIL address, or contract number is 
     * invalid or missing for CONTRACTOR registration, or password is missing
     * or unacceptable
     * 500 - Unexpected internal service error
     * 
     * 
     * @param object the JSON containing the registration request information
     * @return an OK Response if everything fine, or exception otherwise
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_JSON)
    @Path ("/register")
    public Response register(String object) {
	
        EntityManager em = DoeServletContextListener.createEntityManager();
        RegistrationRequest request;
        
        try {
            request = mapper.readValue(object, RegistrationRequest.class);
        } catch (IOException e) {
                log.error("Error in register: ",e);
                return ErrorResponse
                        .status(Response.Status.INTERNAL_SERVER_ERROR, "Unable to process JSON.")
                        .build();
        }
        
        // some values have to be set
        if (StringUtils.isBlank(request.getEmail()))
            return ErrorResponse
                    .badRequest("Missing required email address for registration.")
                    .build();
        
        try {
            User user = em.find(User.class, request.getEmail());

            // if there's already a user on file, cannot re-register if VERIFIED
            if ( user != null && user.isVerified() ) {
                return ErrorResponse
                        .status(Response.Status.BAD_REQUEST, "An account with this email address already exists.")
                        .build();
            }	

            // ensure passwords sent match up
            if (!StringUtils.equals(request.getPassword(), request.getConfirmPassword())) {
                return ErrorResponse
                        .status(Response.Status.BAD_REQUEST, "Password does not match.")
                        .build();
            }
            // ensure password is acceptable
            if (!validatePassword(request.getEmail(), request.getPassword())) 
                return ErrorResponse
                        .status(Response.Status.BAD_REQUEST, "Password is not acceptable.")
                        .build();

            String encryptedPassword = PASSWORD_SERVICE.encryptPassword(request.getPassword());

            // assign as SITE if possible based on the EMAIL, or default to CONTRACTOR
            String domain = request.getEmail().substring(request.getEmail().indexOf("@"));
            TypedQuery<Site> query = em.createQuery("SELECT s FROM Site s join s.emailDomains d WHERE d = :domain", Site.class);
            query.setParameter("domain", domain);

            // look up the Site and set CODE, or CONTR if not found
            List<Site> sites = query.getResultList();
            String siteCode = ((sites.isEmpty()) ? "CONTR" : sites.get(0).getSiteCode());
            
            // if CONTR, we need to REQUIRE and VALIDATE the CONTRACT NUMBER
            if (StringUtils.equals(siteCode, "CONTR")) {
                if (StringUtils.isBlank(request.getContractNumber()))
                    return ErrorResponse
                            .badRequest("Missing required contract number.")
                            .build();
                else if (!Validation.isValidAwardNumber(request.getContractNumber()))
                    return ErrorResponse
                            .badRequest("Contract number is not a valid DOE contract number.")
                            .build();
            }
            
            // first and last names are required
            if (StringUtils.isBlank(request.getFirstName()) || StringUtils.isBlank(request.getLastName()))
                return ErrorResponse
                        .badRequest("Missing required first/last names.")
                        .build();
            
            String apiKey = DOECodeCrypt.nextUniqueString();
            String confirmationCode = DOECodeCrypt.nextUniqueString();

            User newUser = new User(request.getEmail(),encryptedPassword,apiKey, confirmationCode);
        	
            em.getTransaction().begin();
            
            // if USER already exists in the persistence context, just update
            // its values; if not, need to create and persist a new one
            if (null==user) {
                user = new User(request.getEmail(), encryptedPassword, apiKey, confirmationCode);
                user.setFirstName(request.getFirstName());
                user.setLastName(request.getLastName());
                user.setContractNumber(request.getContractNumber());
                user.setSiteId(siteCode);
                em.persist(user);
            } else {
                user.setApiKey(apiKey);
                user.setPassword(encryptedPassword);
                user.setConfirmationCode(confirmationCode);
                user.setFirstName(request.getFirstName());
                user.setLastName(request.getLastName());
                user.setContractNumber(request.getContractNumber());
                user.setSiteId(siteCode);
            }
          
            em.getTransaction().commit();
            
            // send email to user
            sendRegistrationConfirmation(newUser.getConfirmationCode(), newUser.getEmail());
            
            //  return an OK response
            return Response
                    .ok()
                    .entity(mapper.createObjectNode().put("status", "success").toString())
                    .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
                    .build();
        } finally {
            em.close();  
        }
    }
    
    /**
     * Process a "Forgot-my-password" request.  If the indicated EMAIL User 
     * account is VERIFIED, a new confirmation code is issued and an email 
     * sent to the account with a link to one-time log in based on that value.
     * 
     * Response Codes:
     * 200 - OK (empty) request processed successfully
     * 400 - unable to read JSON, or account not found/not verified
     * 500 - unexpected error or database failure
     * 
     * @param object JSON containing the "email" address to use
     * @return an appropriate Response
     */
    @POST
    @Consumes (MediaType.APPLICATION_JSON)
    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/forgotpassword")
    public Response forgotPassword(String object) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        EmailRequest request;
        
        try {
            request = mapper.readValue(object, EmailRequest.class);
        } catch ( IOException e ) {
            log.warn("JSON Error forgotpassword: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("Unable to process request.")
                    .build();
        }
        
        // attempt to process the request
        try {
            User user = em.find(User.class, request.getEmail());
            
            // account has to exist AND be verified
            if (null==user || !user.isVerified()) 
                return ErrorResponse
                        .badRequest("Invalid account in request.")
                        .build();
            
            // create a new CONFIRMATION CODE
            String confirmationCode = DOECodeCrypt.nextUniqueString();
            
            // store it
            em.getTransaction().begin();
            
            user.setConfirmationCode(confirmationCode);
            
            em.getTransaction().commit();
            
            // send an EMAIL
            sendForgotPassword(user.getConfirmationCode(), user.getEmail());
            // return OK
            return Response
                    .ok()
                    .entity(mapper.createObjectNode().put("status", "success").toString())
                    .build();
        } catch ( Exception e ) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("forgot password error: " + e.getMessage());
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
            
        } finally {
            em.close();
        }
    }
    
    /**
     * Generate a new API key for a User by request.  Must be authenticated, and
     * will store the new key immediately and return it to the requestor.
     * 
     * Response Codes:
     * 200 - OK, new API key in the JSON
     * 500 - internal error occurred in the database or key generation
     * 
     * @return a Response containing the new API key, or error information
     */
    @GET
    @RequiresAuthentication
    @Produces (MediaType.APPLICATION_JSON)
    @Consumes ({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    @Path ("/newapikey")
    public Response newApiKey() {
        EntityManager em = DoeServletContextListener.createEntityManager();
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        try {
            // generate a new API key and store it
            String apiKey = DOECodeCrypt.nextUniqueString();
            
            user.setApiKey(apiKey);
            
            // store it in the database
            em.getTransaction().begin();
            
            em.merge(user);
            
            em.getTransaction().commit();
            
            // send back the Response with information
            return Response
                    .ok(mapper.createObjectNode().put("apiKey", user.getApiKey()).toString())
                    .build();
        } catch ( Exception e ) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            log.warn("API Key generation failed: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("API key generation failed.")
                    .build();
        } finally {
            em.close();
        }
    }
    
    /**
     * Request site admin privileges for the logged-in user account.
     * 
     * CONTR users may not access this endpoint.  Logged in lab users may
     * request permission to access other records from their site; this role
     * is PENDING APPROVAL until a site admin user ("OSTI") either approves or
     * disapproves that role.
     * 
     * Response Codes:
     * 200 - OK, user already has or has requested this role
     * 201 - CREATED, user role set to pending approval
     * 401 - unauthorized, not currently logged in
     * 403 - forbidden, unable to access
     * 500 - unexpected or database error
     * 
     * @return an empty Response with the appropriate status code
     */
    @GET
    @RequiresAuthentication
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/requestadmin")
    public Response requestAdmin() {
        EntityManager em = DoeServletContextListener.createEntityManager();
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        try {
            // contractors MAY NOT do this
            if (StringUtils.equals("CONTR", user.getSiteId()))
                return ErrorResponse
                        .forbidden("Operation not permitted.")
                        .build();
            
            // already have this role?  Return OK
            Set<String> pendingRoles = user.getPendingRoles();
            Set<String> roles = user.getRoles();
            if ((null!=roles && roles.contains(user.getSiteId())) ||
                (null!=pendingRoles && pendingRoles.contains(user.getSiteId())))
                return Response
                        .ok()
                        .entity(mapper.createObjectNode().put("status", "success").toString())
                        .build();
            
            // post a pending role request
            em.getTransaction().begin();
            pendingRoles = new HashSet<>();
            pendingRoles.add(user.getSiteId());
            
            user.setPendingRoles(pendingRoles);
            em.merge(user);
            
            em.getTransaction().commit();
            
            // return CREATED
            return Response
                    .status(Response.Status.CREATED)
                    .entity(mapper.createObjectNode().put("status", "success").toString())
                    .build();
        } catch ( Exception e ) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Error: " + e.getMessage());
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }

    /**
     * Processes edits to a user.  Allows changing FIRST and LAST name.
     * 
     * Cannot change EMAIL or PASSWORD information from this endpoint.
     * 
     * Response Codes:
     * 
     * 200 - OK, user name changed
     * 400 - Bad request, no name information sent
     * 500 - Unable to parse request or internal system error
     * 
     * @param object the JSON containing the user information
     * @return an OK Response if everything fine, or exception otherwise
     */
    @POST
    @RequiresAuthentication
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_JSON)
    @Path ("/update")
    public Response editUser(String object) {
	
        EntityManager em = DoeServletContextListener.createEntityManager();
        RegistrationRequest request;
        try {
            request = mapper.readValue(object, RegistrationRequest.class);
        } catch (IOException e) {
                log.error("Error in register: ",e);
                return ErrorResponse
                        .status(Response.Status.INTERNAL_SERVER_ERROR, "Error processing request.")
                        .build();
        }
        
        // nothing to do?
        if (StringUtils.isBlank(request.getFirstName()) ||
            StringUtils.isBlank(request.getLastName()))
            return ErrorResponse
                    .badRequest("Required information missing.")
                    .build();
        
        // get the USER from the session
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        	
        try {
            em.getTransaction().begin();
            
            // update the names
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            
            em.merge(user);
            
            em.getTransaction().commit();

            return Response
                    .ok()
                    .entity(mapper.createObjectNode().put("status", "success").toString())
                    .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
                    .build();
        } finally {
            em.close();  
        }
    }


    /**
     * Processes edits to a user.
     * 
     * @param object the JSON containing the user information
     * @return an OK Response if everything fine, or exception otherwise
     */
    @POST
    @RequiresAuthentication
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_JSON)
    @Path ("/changepassword")
    public Response changePassword(String object) {
	
        EntityManager em = DoeServletContextListener.createEntityManager();
        RegistrationRequest request;
        try {
            request = mapper.readValue(object, RegistrationRequest.class);
        } catch (IOException e) {
            log.error("Error in register: ",e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, "Unable to process request.")
                    .build();
        }

        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        if (!StringUtils.equals(request.getPassword(), request.getConfirmPassword())) {
            return ErrorResponse
                    .status(Response.Status.BAD_REQUEST, "Passwords do not match.")
                    .build();
        }
        if (!validatePassword(user.getEmail(), request.getPassword()))
            return ErrorResponse
                    .status(Response.Status.BAD_REQUEST, "Password is not acceptable.")
                    .build();
        
        try {
            User u = em.find(User.class, user.getEmail());

            if (null==u) {
                return ErrorResponse
                        .status(Response.Status.BAD_REQUEST, "Unable to update user information.")
                        .build();
            }
            
            //  set the new password
            em.getTransaction().begin();

            // set the encrypted password
            u.setPassword(PASSWORD_SERVICE.encryptPassword(request.getPassword()));
            // set the date to now
            u.setDatePasswordChanged();
            
            em.getTransaction().commit();

            return Response
                    .ok()
                    .entity(mapper.createObjectNode().put("status", "success").toString())
                    .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
                    .build();
        } finally {
            em.close();  
        }
    }

    /**
     * Administer a user.  Allows ADMIN account to activate/deactivate a
     * User account by EMAIL.
     * 
     * Response Codes:
     * 200 - OK, user action performed. JSON includes email address and active state flag
     * 400 - Bad request, email is missing or account is not verified
     * 401 - Unauthorized, user is not logged in
     * 403 - Forbidden, user has no permission to access this function
     * 404 - Not found, user email is not on file
     * 500 - Internal error occurred
     * 
     * @param object JSON containing the request
     * @return a Response based on the action taken
     */
    @POST
    @RequiresAuthentication
    @RequiresRoles("OSTI")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/admin")
    public Response adminUser(String object) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        AdminRequest request;
        try {
            request = mapper.readValue(object, AdminRequest.class);
        } catch ( IOException e ) {
            log.error("Error in admin: ",e);
                return ErrorResponse
                        .internalServerError("Unable to process request.")
                        .build();
        }
        
        // required values
        if (StringUtils.isBlank(request.getEmail()))
            return ErrorResponse
                    .badRequest("Missing required email address of user.")
                    .build();
        
        try {
            User user = em.find(User.class, request.getEmail());
            
            if (null==user)
                return ErrorResponse
                        .notFound("User not on file.")
                        .build();
            // user should be verified to administer
            if (!user.isVerified())
                return ErrorResponse
                        .badRequest("User account is not verified.")
                        .build();
            
            // modify the user according to the request
            em.getTransaction().begin();
            
            user.setActive(request.isActivate());
            
            em.getTransaction().commit();
            
            // respond with success
            return Response
                    .ok()
                    .entity(mapper.createObjectNode()
                            .put("email", user.getEmail())
                            .put("active", user.isActive()).toString())
                    .build();
        } catch ( Exception e ) {
            log.error("Admin Error: ", e);
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();
        }
    }

    /**
     * Processes edits to a user.  Approves any PENDING ROLES on the indicated
     * user account.
     * 
     * @param object the JSON containing the user information
     * @return an OK Response if everything fine, or exception otherwise
     */
    @POST
    @RequiresAuthentication
    @RequiresRoles("OSTI")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_JSON)
    @Path ("/approve")
    public Response approveRoles(String object) {
	
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        EmailRequest request;
        try {
            request = mapper.readValue(object, EmailRequest.class);
        } catch (IOException e) {
                log.error("Error in register: ",e);
                return ErrorResponse
                        .internalServerError("Unable to process request.")
                        .build();
        }

        User user = em.find(User.class,request.getEmail());
        
        if (null==user)
            return ErrorResponse
                    .notFound("User is not on file.")
                    .build();
        	        	
        try {
            em.getTransaction().begin();

            user.setRoles(user.getPendingRoles());
            user.setPendingRoles(new HashSet<>());

            em.getTransaction().commit();
            
            return Response
                    .ok()
                    .entity(mapper.createObjectNode().put("status", "success").toString())
                    .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .internalServerError(e.getMessage())
                    .build();
        } finally {
            em.close();  
        }
    }

    /**
     * Processes edits to a user.  Disapproves any PENDING ROLES on the indicated
     * user account.
     * 
     * @param object the JSON containing the user information
     * @return an OK Response if everything fine, or exception otherwise
     */
    @POST
    @RequiresAuthentication
    @RequiresRoles("OSTI")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes (MediaType.APPLICATION_JSON)
    @Path ("/disapprove")
    public Response disapproveRoles(String object) {
	
        EntityManager em = DoeServletContextListener.createEntityManager();
        EmailRequest request;
        
        try {
                request = mapper.readValue(object, EmailRequest.class);
        } catch (IOException e) {
                log.error("Error in register: ",e);
                return ErrorResponse
                        .status(Response.Status.INTERNAL_SERVER_ERROR, "Unable to process request.")
                        .build();
        }

        User user = em.find(User.class,request.getEmail());
        
        try {
            em.getTransaction().begin();
            
            user.setPendingRoles(new HashSet<>());
            
            em.getTransaction().commit();
            
            return Response
                    .ok()
                    .entity(mapper.createObjectNode().put("status", "success").toString())
                    .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
                    .build();
        } finally {
            em.close();  
        }
    }

    /**
     * Acquire a listing of all records by OWNER.
     * 
     * @return the Metadata information in the desired format
     * @throws JsonProcessingException 
     */
    @GET
    @Path ("/requests")
    @RequiresRoles("OSTI")
    @Produces (MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    public Response loadRequests() throws JsonProcessingException {
        EntityManager em = DoeServletContextListener.createEntityManager();
        ArrayList<RequestNode> requests = new ArrayList<>();

        try {
            // acquire a list of Users with PENDING ROLES
            TypedQuery<User> query = em.createQuery("SELECT DISTINCT u FROM User u JOIN u.pendingRoles p", User.class);
            List<User> users = query.getResultList();

            for (User u : users) {
                requests.add(new RequestNode(u.getEmail(), u.getPendingRoles()));
            }
            return Response
                    .ok()
                    .entity(mapper.createObjectNode().putPOJO("requests", mapper.valueToTree(requests)).toString())
                    .build();
        } finally {
            em.close();
        }
    }

    // ObjectMapper instance for metadata interchange
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * an Email request; for approve/disapprove endpoints.
     */
    @JsonIgnoreProperties (ignoreUnknown = true)
    private static class EmailRequest implements Serializable {
        private String email;
        
        public EmailRequest() {
            
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getEmail() {
            return this.email;
        }
    }
    
    /**
     * Administrative request, activate or deactivate Users based on EMAIL.
     */
    @JsonIgnoreProperties (ignoreUnknown=true)
    private static class AdminRequest extends EmailRequest {
        private boolean activate;

        /**
         * @return the activate
         */
        public boolean isActivate() {
            return activate;
        }

        /**
         * @param activate the activate to set
         */
        public void setActivate(boolean activate) {
            this.activate = activate;
        }
        
    }
    
    /**
     * Static class to define the input properties of a Login request.
     * 
     * email - the login name/email address
     * password - the password
     * confirmation_code - optional CONFIRMATION CODE JWT for one-time login
     */
    @JsonIgnoreProperties (ignoreUnknown = true)
    private static class LoginRequest implements Serializable {
        private String email;
        private String password;
        private String confirmation_code;
        
        public LoginRequest() {
            
        }

        /**
         * @return the email
         */
        public String getEmail() {
            return email;
        }

        /**
         * @param email the email to set
         */
        public void setEmail(String email) {
            this.email = email;
        }

        /**
         * @return the password
         */
        public String getPassword() {
            return password;
        }

        /**
         * @param password the password to set
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * @return the confirmation_code
         */
        public String getConfirmationCode() {
            return confirmation_code;
        }

        /**
         * @param confirmation_code the confirmation_code to set
         */
        public void setConfirmationCode(String confirmation_code) {
            this.confirmation_code = confirmation_code;
        }
    }
    
    /**
     * Password Request -- for registration requests.
     * 
     * email - desired email address/login name
     * first_name - desired first name
     * last_name - desired last name
     * password - desired password
     * confirm_password - should be same as password
     */
    @JsonIgnoreProperties (ignoreUnknown = true)
    private static class RegistrationRequest implements Serializable {
        private String email;
        private String firstName;
        private String lastName;
        private String password;
        private String confirmPassword;
        private String contractNumber;
        
        public RegistrationRequest() {
            
        }

        /**
         * @return the email
         */
        public String getEmail() {
            return email;
        }

        /**
         * @param email the email to set
         */
        public void setEmail(String email) {
            this.email = email;
        }

        /**
         * @return the password
         */
        public String getPassword() {
            return password;
        }

        /**
         * @param password the password to set
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * @return the confirmPassword
         */
        public String getConfirmPassword() {
            return confirmPassword;
        }

        /**
         * @param confirmPassword the confirmPassword to set
         */
        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }

        /**
         * @return the firstName
         */
        public String getFirstName() {
            return firstName;
        }

        /**
         * @param firstName the firstName to set
         */
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        /**
         * @return the lastName
         */
        public String getLastName() {
            return lastName;
        }

        /**
         * @param lastName the lastName to set
         */
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        /**
         * @return the contractNumber
         */
        public String getContractNumber() {
            return contractNumber;
        }

        /**
         * @param contractNumber the contractNumber to set
         */
        public void setContractNumber(String contractNumber) {
            this.contractNumber = contractNumber;
        }
        
    }
    
    private class RequestNode {
	private String user;
	private List<String> requested_roles = new ArrayList<>();
	
	protected RequestNode(String user, String requested_role) {
		this.user = user;
                this.requested_roles.add(requested_role);
	}
        
        protected RequestNode(String user, List<String> roles) {
            this.user = user;
            this.requested_roles = roles;
        }
        
        protected RequestNode(String user, Set<String> roles) {
            this.user = user;
            this.requested_roles.addAll(roles);
        }

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public List<String> getRequestedRoles() {
		return requested_roles;
	}

	public void setRequestedRoles(List<String> roles) {
		this.requested_roles = roles;
	}
        
        public void setRequestedRoles(Set<String> roles) {
            this.requested_roles.addAll(roles);
        }
    }

    /**
     * Perform confirmation from previously-sent email.
     * 
     * Responses:
     * 200 - confirmation successful
     * 400 - user account already verified
     * 401 - confirmation code invalid or expired
     * 404 - user account not found by confirmation code
     * 500 - internal error in database or token updates
     * 
     * @param jwt the JWT token to access the confirmation account
     * @return Responses according to above information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path ("/confirm")
    public Response confirmUser(@QueryParam("confirmation") String jwt) {
        User currentUser = null;
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            // attempt to parse the JWT token to get the confirmation code decrypted
            Claims claims = DOECodeCrypt.parseJWT(jwt);
            String confirmationCode = claims.getId();
            String email = claims.getSubject();

            currentUser = em.find(User.class, email);

            if (currentUser == null) {
                //no user matched, return with error
                return ErrorResponse
                        .status(Response.Status.NOT_FOUND, "User not on file.")
                        .build();
            }

            if (currentUser.isVerified()) {
                //return and note that user is already verified
                return ErrorResponse
                        .status(Response.Status.BAD_REQUEST, "User is already verified.")
                        .build();
            }


            if (!StringUtils.equals(confirmationCode, currentUser.getConfirmationCode())) {
                return ErrorResponse
                        .status(Response.Status.UNAUTHORIZED, "Request is not authorized.")
                        .build();
            }
            //if we got here, we're good. Verify (and activate) and clear the confirmation code
            currentUser.setVerified(true);
            currentUser.setActive(true);
            currentUser.setConfirmationCode("");

            em.getTransaction().begin();

            em.merge(currentUser);
            em.getTransaction().commit();

            return Response
                .ok()
                .entity(mapper
                        .createObjectNode()
                        .put("apiKey", currentUser.getApiKey()).toString())
                .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();

            //we'll deal with duplicate user name here as well...
            log.error("Error on confirmation", e);
            return ErrorResponse
                    .status(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
                    .build();
        } finally {
            em.close();  
        }

    }

    /**
     * Send a confirmation request for new user registrations.
     * 
     * @param confirmationCode the confirmation code associated with the user account
     * @param userEmail the user email address to send to
     */
    private static void sendRegistrationConfirmation(String confirmationCode, String userEmail) {
        HtmlEmail email = new HtmlEmail();
        email.setHostName(EMAIL_HOST);

        try {
                email.setFrom(EMAIL_FROM);
                String confirmation_url = SITE_URL + "/confirmuser?confirmation=" + DOECodeCrypt.generateConfirmationJwt(confirmationCode, userEmail);
                email.setSubject("Confirm DOE Code Registration");
                email.addTo(userEmail);


                String msg = "<html> Thank you for registering for a DOE Code Account. Please click the link below or paste it into your browser to confirm your account. <br/> ";
                msg += "<a href=\"" + confirmation_url + "\">" + confirmation_url + "</a></html>";
                email.setHtmlMsg(msg);
                email.send();

        } catch (EmailException e) {
                log.error("Email error: " + e.getMessage());
        }
    }
    
    /**
     * Send a "forgot-my-password" email for resetting the password.  The included link is a one-time 
     * login key based on the confirmation code key.
     * 
     * @param confirmationCode the confirmation code associated with the user
     * @param userEmail the user email address
     */
    private static void sendForgotPassword(String confirmationCode, String userEmail) {
        HtmlEmail email = new HtmlEmail();
        email.setHostName(EMAIL_HOST);
        
        try {
            email.setFrom(EMAIL_FROM);
            String loginEmail = SITE_URL + "/account?passcode=" + DOECodeCrypt.generateConfirmationJwt(confirmationCode, userEmail);
            email.setSubject("Forgotten Password");
            email.addTo(userEmail);
            
            String msg = "<html> A request to reset the password associated with your account on DOECode was recently issued.  In order to change your password, please log in by<br> ";
            msg += "logging in to the link below.<p>";
            msg += "<a href=\"" + loginEmail + "\">" + loginEmail + "</a></html>";
            email.setHtmlMsg(msg);
            email.send();
            
        } catch ( EmailException e ) {
            log.error("Email Error: " + e.getMessage());
        }
    }
    
    /**
     * Send an email message to the User should their account become inactivated
     * due to repeated password failures.
     * 
     * @param userEmail the user email in question
     */
    private static void sendLockedAccountEmail(String userEmail) {
        HtmlEmail email = new HtmlEmail();
        email.setHostName(EMAIL_HOST);
        
        try {
            email.setFrom(EMAIL_FROM);
            email.setSubject("DOECode User Account Locked Out");
            email.addTo(userEmail);
            
            email.setHtmlMsg("<html>Your account has been automatically deactivated due to after 3 unsuccessful logon attempts.  "
                    + "<p>Please contact doecode@osti.gov as an administrator will need to reactivate your account before you "
                    + "can sign-in to DOE CODE.</html>");
            
            email.send();
        } catch ( EmailException e ) {
            log.error("Email Error: ",e);
        }
    }
    
    /**
     * Send an email message when a User password expires.
     * @param userEmail the user account email address
     */
    private static void sendPasswordExpiredEmail(String userEmail) {
        HtmlEmail email = new HtmlEmail();
        email.setHostName(EMAIL_HOST);
        
        try {
            email.setFrom(EMAIL_FROM);
            email.setSubject("DOECode User Account Password Expired");
            email.addTo(userEmail);
            
            email.setHtmlMsg("<html>Your account password has expired.  Please submit a forgotten password request from DOECode in order to change it.  "
                    + "<p>Please contact doecode@osti.gov if you have any questions about this message or trouble processing any requests.</html>");
            email.send();
            
        } catch ( EmailException e ) {
            log.error("Email Error: ",e);
        }
    }

    /**
     * Locate a User record by EMAIL address.
     * 
     * @param email the EMAIL to look for
     * @return a User object if possible or null if not found or errors
     */
    private static User findUserByEmail(String email) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            return em.find(User.class, email);
        } catch ( Exception e ) {
            log.warn("Error locating user : " + email, e);
            return null;
        } finally {
            em.close();
        }
    }
    
    /**
     * Process success or failure of a user password login.
     * 
     * On success, reset the failure count to 0.  On failure, increment failed
     * count; if that reaches 3, deactivate the user account.
     * 
     * @param email the EMAIL address
     * @param failure whether or not the attempt failed
     */
    private static void processUserLogin(String email, boolean failure) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            // find the User
            User user = em.find(User.class, email);
            
            // this shouldn't happen
            if (null==user) 
                throw new NotFoundException("User not on file!");
            
            em.getTransaction().begin();
            
            if ( failure ) {
                // add up a failure mark
                user.setFailedCount( user.getFailedCount()+1 );

                // if it exceeds the threshold, mark the user inactive
                if (user.getFailedCount()>=3) {
                    // mark account inactive and unable to log in
                    user.setActive(false);
                    // inform the user
                    sendLockedAccountEmail(email);
                }
            } else {
                // success
                user.setFailedCount(0);
            }
            
            // store it
            em.getTransaction().commit();
            
        } catch ( Exception e ) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Unable to mark failed user login for: " + email, e);
        } finally {
            em.close();
        }
    }
    
    /**
     * Reset the "user token" (confirmation code) values.  Also, update number
     * of failed logins, in case there are any, for that account.  This presumes
     * to be called as the result of a successful access attempt.
     * 
     * @param email the email address to affect changes
     */
    private static void resetUserToken(String email) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            User user = em.find(User.class, email);
            
            if (null==user)
                throw new NotFoundException("Unable to locate user " + email);
            
            em.getTransaction().begin();
            
            user.setConfirmationCode("");
            user.setFailedCount(0);
            
            em.getTransaction().commit();
        } catch ( Exception e ) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            log.warn("Unable to clear token for " + email, e);
        } finally {
            em.close();
        }
    }
    
}
