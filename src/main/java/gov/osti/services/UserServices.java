package gov.osti.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.osti.entity.DOECodeMetadata;
import gov.osti.entity.Site;

import gov.osti.entity.User;
import gov.osti.listeners.DoeServletContextListener;
import gov.osti.security.DOECodeCrypt;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Set;

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
    
	ObjectMapper mapper = new ObjectMapper();
	ObjectNode returnNode = mapper.createObjectNode();
    // return an OK if authenticated, otherwise authentication services will handle status
    return Response
            .status(Response.Status.OK)
            .entity(returnNode.put("email", user.getEmail()).toString())
            .build();
}

/**
 * Process login requests.
 * 
 * @param object JSON containing "email" and "password" to authenticate.
 * @return an appropriate Response based on whether or not authentication succeeded
 */
@POST
@Produces(MediaType.APPLICATION_JSON)
@Consumes (MediaType.APPLICATION_JSON)
@Path ("/login")
public Response login(String object) {
	ObjectMapper mapper = new ObjectMapper();
	ObjectNode returnNode = mapper.createObjectNode();
	JsonNode node = null;
	try {
		node = mapper.readTree(object);
	} catch (IOException e) {
		// TODO Auto-generated catch block
                log.warn("JSON Mapper error: " + e.getMessage());
	}
	String email= node.get("email").asText();
	String password = node.get("password").asText();
    User currentUser = null;
    
    //String encryptedPassword = PASSWORD_SERVICE.encryptPassword(password);
    EntityManager em = DoeServletContextListener.createEntityManager();
    try {        
    	currentUser = em.find(User.class, email);
    } catch ( Exception e ) {
        log.warn("Error Retrieving User",e);
        throw new InternalServerErrorException(e.getMessage());
    } finally {
        em.close();  
    }
    
    if (currentUser == null || !PASSWORD_SERVICE.passwordsMatch(password, currentUser.getPassword())) {
    	//no user matched, return with error
    	return Response.status(401).build();
    }
    
    //if (!currentUser.isVerified())
    
    
	String xsrfToken = DOECodeCrypt.nextRandomString();
	String accessToken = DOECodeCrypt.generateLoginJWT(currentUser.getApiKey(), xsrfToken);
	NewCookie cookie = DOECodeCrypt.generateNewCookie(accessToken);
	
    return Response.ok(returnNode.put("xsrfToken", xsrfToken).toString()).cookie(cookie).build();

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
	
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode returnNode = mapper.createObjectNode();
		boolean merge = true;
        EntityManager em = DoeServletContextListener.createEntityManager();
		JsonNode node = null;
		try {
			node = mapper.readTree(object);
		} catch (IOException e) {
			log.error("Error in register: ",e);
		}
		
		String email = node.get("email").asText();
		String password = node.get("password").asText();
		String confirmPassword = node.get("confirm_password").asText();

		try {
			
	    User previousUser = em.find(User.class, email);
	    
	    if (previousUser == null) {
	    	merge = false;
	    }
	    else if(previousUser.isVerified()) {
			return Response
                                .status(Response.Status.BAD_REQUEST)
                                .entity(returnNode.put("errors", "An account with this email address already exists.").toString())
                                .build();
	    }
	    	
		
		if (!StringUtils.equals(password, confirmPassword)) {
			return Response
                                .status(Response.Status.BAD_REQUEST)
                                .entity(returnNode.put("errors", "Password Not Matching").toString())
                                .build();
		}
		
		String encryptedPassword = PASSWORD_SERVICE.encryptPassword(password);
		
		
		
		//check if the email is related to a valid site and assign site ID, for now just hardcoding as ORNL
		
		String apiKey = DOECodeCrypt.nextUniqueString();
		String confirmationCode = DOECodeCrypt.nextUniqueString();
		
		User newUser = new User(email,encryptedPassword,apiKey, confirmationCode);
		Set<String> pendingRoles = new HashSet<>();
		pendingRoles.add("ORNL");
        newUser.setPendingRoles(pendingRoles);
        	
            em.getTransaction().begin();
            
            if (merge) {
            	em.merge(newUser);
            } else {
            	em.persist(newUser);
            }
          
            em.getTransaction().commit();
            
            sendRegistrationConfirmation(newUser.getConfirmationCode(), newUser.getEmail());
            return Response.ok(returnNode.put("apiKey", newUser.getApiKey()).toString()).build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            throw new InternalServerErrorException(e.getMessage());
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
	
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode returnNode = mapper.createObjectNode();
        EntityManager em = DoeServletContextListener.createEntityManager();
		JsonNode node = null;
		try {
			node = mapper.readTree(object);
		} catch (IOException e) {
			log.error("Error in register: ",e);
		}
		
		String email = node.get("email").asText();
		
		//For now, we just support one role...
		String role = node.get("pending_role").asText();
		Set<String> pendingRoles = new HashSet<>();
		pendingRoles.add(role);
		
		
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
	    	

        if (StringUtils.isNotBlank(email))
        	user.setEmail(email);
        
		user.setPendingRoles(pendingRoles);

		
		
        	
		try {
        	
        em.getTransaction().begin();

        em.merge(user);  
        em.getTransaction().commit();
            
            return Response.ok().build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            throw new InternalServerErrorException(e.getMessage());
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
	
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode returnNode = mapper.createObjectNode();
        EntityManager em = DoeServletContextListener.createEntityManager();
		JsonNode node = null;
		try {
			node = mapper.readTree(object);
		} catch (IOException e) {
			log.error("Error in register: ",e);
		}
		
		String password = node.get("password").asText();
		String confirmPassword = node.get("confirm_password").asText();
			
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
	    	
		
		if (!StringUtils.equals(password, confirmPassword)) {
			return Response
                                .status(Response.Status.BAD_REQUEST)
                                .entity(returnNode.put("errors", "Password Not Matching").toString())
                                .build();
		}
		
		String encryptedPassword = PASSWORD_SERVICE.encryptPassword(password);
		user.setPassword(encryptedPassword);
		        	
		try {
        	
        em.getTransaction().begin();

        em.merge(user);
           
          
            em.getTransaction().commit();
            
            return Response.ok().build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            throw new InternalServerErrorException(e.getMessage());
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
	
		ObjectMapper mapper = new ObjectMapper();
        EntityManager em = DoeServletContextListener.createEntityManager();
        ObjectNode returnNode = mapper.createObjectNode();
		JsonNode node = null;
		try {
			node = mapper.readTree(object);
		} catch (IOException e) {
			log.error("Error in register: ",e);
		}
		
		String email = node.get("email").asText();
        User user = em.find(User.class,email);
	    user.setRoles(user.getPendingRoles());
	    user.setPendingRoles(new HashSet<String>());
		        	
		try {
        	
        em.getTransaction().begin();

        em.merge(user);
           
          
            em.getTransaction().commit();
            
            return Response.ok().entity(returnNode.put("success", "success").toString()).build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            throw new InternalServerErrorException(e.getMessage());
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
	
		ObjectMapper mapper = new ObjectMapper();
        EntityManager em = DoeServletContextListener.createEntityManager();
        ObjectNode returnNode = mapper.createObjectNode();
		JsonNode node = null;
		try {
			node = mapper.readTree(object);
		} catch (IOException e) {
			log.error("Error in register: ",e);
		}
		
		String email = node.get("email").asText();
        User user = em.find(User.class,email);
	    user.setPendingRoles(new HashSet<String>());
		        	
		try {
        	
        em.getTransaction().begin();

        em.merge(user);
           
          
            em.getTransaction().commit();
            
            return Response.ok().entity(returnNode.put("success", "success").toString()).build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            throw new InternalServerErrorException(e.getMessage());
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
	ObjectMapper mapper = new ObjectMapper();
	ArrayList<RequestNode> requests = new ArrayList<>();
    // get the security user in context
    Subject subject = SecurityUtils.getSubject();
    User user = (User) subject.getPrincipal();
    
    try {
    	TypedQuery<User> query = em.createQuery("SELECT u FROM User u", User.class);
    	List<User> users = query.getResultList();
    	
    	
    	for (User u : users) {
    		if (!u.getPendingRoles().isEmpty()) {		
    		String email = u.getEmail();
    		String pendingRole = "";
    		for (String role : u.getPendingRoles()) {
    		pendingRole = role;
    		}
    		
    		RequestNode requestNode = new RequestNode(email, pendingRole);
    		requests.add(requestNode);
    		}
    		
    	}
    	
    	RequestsList req = new RequestsList(requests);
                return Response
                        .status(Response.Status.OK)
                        .entity(mapper.createObjectNode().putPOJO("requests", req.toJson()).toString())
                        .build();
    } finally {
        em.close();
    }
}

// ObjectMapper instance for metadata interchange
private static final ObjectMapper mapper = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

private class RequestsList {
	private List<RequestNode> requests;
	
	RequestsList(List<RequestNode> requests) {
		this.requests = requests;
	}

	public List<RequestNode> getRequests() {
		return requests;
	}

	public void setRequests(List<RequestNode> requests) {
		this.requests = requests;
	}
	
    public JsonNode toJson() {
        return mapper.valueToTree(this);
    }

	
	
}

private class RequestNode {
	private String user;
	private String requested_role;
	
	protected RequestNode(String user, String requested_role) {
		this.user = user;
		this.requested_role = requested_role;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getRequested_role() {
		return requested_role;
	}

	public void setRequested_role(String requested_role) {
		this.requested_role = requested_role;
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
	ObjectMapper mapper = new ObjectMapper();
	ObjectNode returnNode = mapper.createObjectNode();


    User currentUser = null;
    
    Claims claims = DOECodeCrypt.parseJWT(jwt);
    String confirmationCode = claims.getId();
    String email = claims.getSubject();
    
    EntityManager em = DoeServletContextListener.createEntityManager();
    try {        
    	
    currentUser = em.find(User.class, email);
    
    
    if (currentUser == null) {
    	//no user matched, return with error
    	return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    if (currentUser.isVerified()) {
    	//return and note that user is already verified
    	return Response.status(Response.Status.BAD_REQUEST).build();
    }
    
    
    if (!StringUtils.equals(confirmationCode, currentUser.getConfirmationCode())) {
    	return Response.status(Response.Status.UNAUTHORIZED).build();
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
        throw new InternalServerErrorException(e.getMessage());
    } finally {
        em.close();  
    }

    return Response.status(Response.Status.OK).entity(returnNode.put("apiKey", currentUser.getApiKey()).toString()).build();

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
