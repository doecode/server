package gov.osti.services;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
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
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.osti.entity.User;
import gov.osti.listeners.DoeServletContextListener;
import gov.osti.security.DOECodeCrypt;
import io.jsonwebtoken.Claims;

@Path("user")
public class UserServices {

private static Logger log = LoggerFactory.getLogger(UserServices.class);
private static final PasswordService PASSWORD_SERVICE = new DefaultPasswordService();

	
public UserServices() {
	
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Consumes (MediaType.APPLICATION_JSON)
@Path ("/login")
public Response login(String object) {
	System.out.println("Logging in");
	ObjectMapper mapper = new ObjectMapper();
	ObjectNode returnNode = mapper.createObjectNode();
	JsonNode node = null;
	try {
		node = mapper.readTree(object);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
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
        System.out.println(e);
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

@POST
@Produces(MediaType.APPLICATION_JSON)
@Consumes (MediaType.APPLICATION_JSON)
@Path ("/register")
public Response register(String object) {
	
	System.out.println("Kick off register");
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
			return Response.status(400).entity(returnNode.put("errors", "An account with this email address already exists.").toString()).build();
	    }
	    	
		
		if (!StringUtils.equals(password, confirmPassword)) {
			return Response.status(400).entity(returnNode.put("errors", "Password Not Matching").toString()).build();
		}
		
		String encryptedPassword = PASSWORD_SERVICE.encryptPassword(password);
		
		
		
		//check if the email is related to a valid site and assign site ID, for now just hardcoding as ORNL
		String siteId = "ORNL";
		
		String apiKey = DOECodeCrypt.nextUniqueString();
		String confirmationCode = DOECodeCrypt.nextUniqueString();
		
		HashSet<String> roles = new HashSet<>();
		roles.add("Admin");
		User newUser = new User(email,encryptedPassword,apiKey,siteId, confirmationCode);
		newUser.setRoles(roles);
        	
        	
            em.getTransaction().begin();
            
            if (merge) {
            	em.merge(newUser);
            } else {
            	em.persist(newUser);
            }
          
            em.getTransaction().commit();
            
            System.out.println("Completed register");
            sendRegistrationConfirmation(newUser.getConfirmationCode(), newUser.getEmail());
            return Response.ok(returnNode.put("apiKey", newUser.getApiKey()).toString()).build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            System.out.println(e);
            //we'll deal with duplicate user name here as well...
            log.error("Persistence Error Registering User", e);
            throw new InternalServerErrorException(e.getMessage());
        } finally {
            em.close();  
        }
}


@GET
@Produces(MediaType.APPLICATION_JSON)
@Path ("/confirm")
public Response confirmUser(@QueryParam("confirmation") String jwt) {
	System.out.println("Confirming user");
	ObjectMapper mapper = new ObjectMapper();
	ObjectNode returnNode = mapper.createObjectNode();


    User currentUser = null;
    
    Claims claims = DOECodeCrypt.parseJWT(jwt);
    String confirmationCode = claims.getId();
    String email = claims.getSubject();
    System.out.println(confirmationCode);
    System.out.println(email);
    
    EntityManager em = DoeServletContextListener.createEntityManager();
    try {        
    	
    currentUser = em.find(User.class, email);
    
    
    if (currentUser == null) {
    	//no user matched, return with error
    	return Response.status(400).build();
    }
    
    if (currentUser.isVerified()) {
    	//return and note that user is already verified
    	return Response.status(400).build();
    }
    
    
    if (!StringUtils.equals(confirmationCode, currentUser.getConfirmationCode())) {
    	return Response.status(401).build();
    }
    
	Date now = new Date();
	if (now.after(claims.getExpiration())) {
		//note that claim has expired, maybe give them the option to get another token?
    	return Response.status(401).build();
	}
	
	
	//if we got here, we're good. Verify and then set the confirmation code
	currentUser.setVerified(true);
	currentUser.setConfirmationCode("");
	
    em.getTransaction().begin();

	em.merge(currentUser);
	em.getTransaction().commit();
        
    } catch ( Exception e ) {
        if ( em.getTransaction().isActive())
            em.getTransaction().rollback();
        
        System.out.println(e);
        //we'll deal with duplicate user name here as well...
        log.error("Error on confirmation", e);
        throw new InternalServerErrorException(e.getMessage());
    } finally {
        em.close();  
    }
	
	
	

    return Response.status(200).entity(returnNode.put("apiKey", currentUser.getApiKey()).toString()).build();

}

private void sendRegistrationConfirmation(String confirmationCode, String userEmail) {
	HtmlEmail email = new HtmlEmail();
	email.setHostName("mx1.osti.gov");
	
	try {
		email.setFrom("welscht@osti.gov");
		String confirmation_url = "http://localhost:8081/confirmuser?confirmation=" + DOECodeCrypt.generateConfirmationJwt(confirmationCode, userEmail);
		email.setSubject("Confirm DOE Code Registration");
		email.addTo(userEmail);
		
		
		String msg = "<html> Thank you for registering for a DOE Code Account. Please click the link below or paste it into your browser to confirm your account. <br/> ";
		msg += "<a href=\"" + confirmation_url + "\">" + confirmation_url + "</a></html>";
		email.setHtmlMsg(msg);
		email.send();

	} catch (EmailException e) {
		log.error("Email error: " + e.getMessage());
		System.out.println(e);
	}
}



}
