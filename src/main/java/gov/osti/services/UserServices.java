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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import gov.osti.entity.Site;

import gov.osti.entity.User;
import gov.osti.listeners.DoeServletContextListener;
import gov.osti.security.DOECodeCrypt;
import io.jsonwebtoken.Claims;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.TypedQuery;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;

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
        if ( StringUtils.length(password)<=8 || null==password )
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
     * Process login requests.
     * 
     * Response Codes:
     * 200 - login OK, sets token and cookie
     * 401 - authentication failed
     * 500 - internal system error, unable to read JSON, etc.
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
                    .create(Response.Status.INTERNAL_SERVER_ERROR, "Error processing request.")
                    .build();
	}
        User currentUser = null;

        //String encryptedPassword = PASSWORD_SERVICE.encryptPassword(password);
        EntityManager em = DoeServletContextListener.createEntityManager();
        try {        
            currentUser = em.find(User.class, request.getEmail());
        } catch ( Exception e ) {
            log.warn("Error Retrieving User",e);
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .build();
        } finally {
            em.close();  
        }

        if (currentUser == null || !currentUser.isVerified() ||
                !PASSWORD_SERVICE.passwordsMatch(request.getPassword(), currentUser.getPassword())) {
            //no user matched, return with error
            return Response
                    .status(Response.Status.UNAUTHORIZED)
                    .build();
        }
    
	String xsrfToken = DOECodeCrypt.nextRandomString();
	String accessToken = DOECodeCrypt.generateLoginJWT(currentUser.getApiKey(), xsrfToken);
	NewCookie cookie = DOECodeCrypt.generateNewCookie(accessToken);
	
        return Response
                .status(Response.Status.OK)
                .entity(mapper.createObjectNode().put("xsrfToken", xsrfToken).toString())
                .cookie(cookie)
                .build();
    }

    /**
     * Process a registration request.
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
        PasswordRequest request;
        
        try {
            request = mapper.readValue(object, PasswordRequest.class);
        } catch (IOException e) {
                log.error("Error in register: ",e);
                return ErrorResponse
                        .create(Response.Status.INTERNAL_SERVER_ERROR, "Unable to process JSON.")
                        .build();
        }
        
        try {
            User user = em.find(User.class, request.getEmail());

            // if there's already a user on file, cannot re-register if VERIFIED
            if ( user != null && user.isVerified() ) {
                return ErrorResponse
                        .create(Response.Status.BAD_REQUEST, "An account with this email address already exists.")
                        .build();
            }	

            // ensure passwords sent match up
            if (!StringUtils.equals(request.getPassword(), request.getConfirmPassword())) {
                return ErrorResponse
                        .create(Response.Status.BAD_REQUEST, "Password does not match.")
                        .build();
            }
            // ensure password is acceptable
            if (!validatePassword(request.getEmail(), request.getPassword())) 
                return ErrorResponse
                        .create(Response.Status.BAD_REQUEST, "Password is not acceptable.")
                        .build();

            String encryptedPassword = PASSWORD_SERVICE.encryptPassword(request.getPassword());

            //check if the email is related to a valid site and assign site ID, for now just hardcoding as ORNL

            String apiKey = DOECodeCrypt.nextUniqueString();
            String confirmationCode = DOECodeCrypt.nextUniqueString();

            User newUser = new User(request.getEmail(),encryptedPassword,apiKey, confirmationCode);
        	
            em.getTransaction().begin();
            
            // if USER already exists in the persistence context, just update
            // its values; if not, need to create and persist a new one
            if (null==user) {
                user = new User(request.getEmail(), encryptedPassword, apiKey, confirmationCode);
                em.persist(user);
            } else {
                user.setApiKey(apiKey);
                user.setPassword(encryptedPassword);
                user.setConfirmationCode(confirmationCode);
            }
          
            em.getTransaction().commit();
            
            // send email to user
            sendRegistrationConfirmation(newUser.getConfirmationCode(), newUser.getEmail());
            
            //  return an OK response
            return Response
                    .ok(mapper
                            .createObjectNode()
                            .put("apiKey", newUser.getApiKey()).toString())
                    .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .create(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
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
    @Path ("/update")
    public Response editUser(String object) {
	
        EntityManager em = DoeServletContextListener.createEntityManager();
        RoleRequest request;
        try {
            request = mapper.readValue(object, RoleRequest.class);
        } catch (IOException e) {
                log.error("Error in register: ",e);
                return ErrorResponse
                        .create(Response.Status.INTERNAL_SERVER_ERROR, "Error processing request.")
                        .build();
        }
        
        // no roles, no operation?
        if (null==request.getPendingRoles() && null==request.getPendingRole())
            return ErrorResponse
                    .create(Response.Status.BAD_REQUEST, "No roles specified to set.")
                    .build();
        
        // may specify a single role or multiple
        Set<String> pendingRoles = new HashSet<>();
        if (null!=request.getPendingRoles()) {
            pendingRoles.addAll(Arrays.asList(request.getPendingRoles()));
        } else {
            pendingRoles.add(request.getPendingRole());
        }
		
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        	
        try {
            em.getTransaction().begin();

            user.setPendingRoles(pendingRoles);
            
            em.merge(user);
            
            em.getTransaction().commit();

            return Response.ok().build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .create(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
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
        PasswordRequest request;
        try {
            request = mapper.readValue(object, PasswordRequest.class);
        } catch (IOException e) {
            log.error("Error in register: ",e);
            return ErrorResponse
                    .create(Response.Status.INTERNAL_SERVER_ERROR, "Unable to process request.")
                    .build();
        }

        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        
        if (!StringUtils.equals(request.getPassword(), request.getConfirmPassword())) {
            return ErrorResponse
                    .create(Response.Status.BAD_REQUEST, "Passwords do not match.")
                    .build();
        }
        if (!validatePassword(user.getEmail(), request.getPassword()))
            return ErrorResponse
                    .create(Response.Status.BAD_REQUEST, "Password is not acceptable.")
                    .build();
        
        try {
            User u = em.find(User.class, user.getEmail());

            if (null==u) {
                return ErrorResponse
                        .create(Response.Status.BAD_REQUEST, "Unable to update user information.")
                        .build();
            }
            
            //  set the new password
            em.getTransaction().begin();

            u.setPassword(PASSWORD_SERVICE.encryptPassword(request.getPassword()));
            
            em.getTransaction().commit();

            return Response
                    .ok()
                    .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .create(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
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
                        .create(Response.Status.INTERNAL_SERVER_ERROR, "Unable to process request.")
                        .build();
        }

        User user = em.find(User.class,request.getEmail());
        
        if (null==user)
            return ErrorResponse
                    .create(Response.Status.NOT_FOUND, "User is not on file.")
                    .build();
        	        	
        try {
            em.getTransaction().begin();

            user.setRoles(user.getPendingRoles());
            user.setPendingRoles(new HashSet<>());

            em.getTransaction().commit();
            
            return Response
                    .ok()
                    .entity(mapper.createObjectNode().put("success", "success").toString())
                    .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .create(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
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
                        .create(Response.Status.INTERNAL_SERVER_ERROR, "Unable to process request.")
                        .build();
        }

        User user = em.find(User.class,request.getEmail());
        
        try {
            em.getTransaction().begin();
            
            user.setPendingRoles(new HashSet<>());
            
            em.getTransaction().commit();
            
            return Response
                    .ok()
                    .entity(mapper.createObjectNode().put("success", "success").toString())
                    .build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            return ErrorResponse
                    .create(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
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

    @JsonIgnoreProperties (ignoreUnknown = true)
    private static class RoleRequest implements Serializable {
        private String email;
        private String pendingRole;
        private String[] pendingRoles;

        public RoleRequest() {
            
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
         * @return the pendingRole
         */
        public String getPendingRole() {
            return pendingRole;
        }

        /**
         * @param pendingRole the pendingRole to set
         */
        public void setPendingRole(String pendingRole) {
            this.pendingRole = pendingRole;
        }

        /**
         * @return the pendingRoles
         */
        public String[] getPendingRoles() {
            return pendingRoles;
        }

        /**
         * @param pendingRoles the pendingRoles to set
         */
        public void setPendingRoles(String[] pendingRoles) {
            this.pendingRoles = pendingRoles;
        }
    }
    
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
    
    @JsonIgnoreProperties (ignoreUnknown = true)
    private static class LoginRequest implements Serializable {
        private String email;
        private String password;
        
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
    }
    
    @JsonIgnoreProperties (ignoreUnknown = true)
    private static class PasswordRequest implements Serializable {
        private String email;
        private String password;
        private String confirmPassword;
        
        public PasswordRequest() {
            
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

        Claims claims = DOECodeCrypt.parseJWT(jwt);
        String confirmationCode = claims.getId();
        String email = claims.getSubject();

        EntityManager em = DoeServletContextListener.createEntityManager();
        try {        

        currentUser = em.find(User.class, email);


        if (currentUser == null) {
            //no user matched, return with error
            return ErrorResponse
                    .create(Response.Status.NOT_FOUND, "User not on file.")
                    .build();
        }

        if (currentUser.isVerified()) {
            //return and note that user is already verified
            return ErrorResponse
                    .create(Response.Status.BAD_REQUEST, "User is already verified.")
                    .build();
        }


        if (!StringUtils.equals(confirmationCode, currentUser.getConfirmationCode())) {
            return ErrorResponse
                    .create(Response.Status.UNAUTHORIZED, "Request is not authorized.")
                    .build();
        }

        String domain = email.substring(email.indexOf("@"));
        TypedQuery<Site> query = em.createQuery("SELECT s FROM Site s join s.emailDomains d WHERE d = :domain", Site.class);
        query.setParameter("domain", domain);

        // look up the Site and set CODE, or CONTR if not found
        List<Site> sites = query.getResultList();
        currentUser.setSiteId((sites.isEmpty()) ? "CONTR" : sites.get(0).getSiteCode());

        //if we got here, we're good. Verify and then set the confirmation code
        currentUser.setVerified(true);
        currentUser.setConfirmationCode("");

        em.getTransaction().begin();

            em.merge(currentUser);
            em.getTransaction().commit();

        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();

            //we'll deal with duplicate user name here as well...
            log.error("Error on confirmation", e);
            return ErrorResponse
                    .create(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage())
                    .build();
        } finally {
            em.close();  
        }

        return Response
                .ok()
                .entity(mapper
                        .createObjectNode()
                        .put("apiKey", currentUser.getApiKey()).toString())
                .build();

    }

    /**
     * Send a confirmation request for new user registrations.
     * 
     * @param confirmationCode the confirmation code associated with the user account
     * @param userEmail the user email address to send to
     */
    private void sendRegistrationConfirmation(String confirmationCode, String userEmail) {
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

}
