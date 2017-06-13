package gov.osti.services;

import java.io.IOException;
import java.util.HashSet;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
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

@Path("user")
public class Login {

private static Logger log = LoggerFactory.getLogger(Login.class);
private static final PasswordService PASSWORD_SERVICE = new DefaultPasswordService();

	
public Login() {
	
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Consumes (MediaType.APPLICATION_JSON)
@Path ("/login")
public Response login(String object) {
	
	ObjectMapper mapper = new ObjectMapper();
	//ObjectNode returnNode = mapper.createObjectNode();
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
        TypedQuery<User> getUserByUserName = em.createQuery("SELECT u FROM User u WHERE u.email = ?1", User.class);
        currentUser = getUserByUserName.setParameter(1, email).getSingleResult();
    } catch ( Exception e ) {
        log.warn("Error Retrieving User: " + e.getMessage());
        System.out.println(e);
        throw new InternalServerErrorException("IO Error: " + e.getMessage());
    } finally {
        em.close();  
    }
    
    if (currentUser == null || !PASSWORD_SERVICE.passwordsMatch(password, currentUser.getPassword())) {
    	//no user matched, return with error
    	return Response.status(403).build();
    }
    
    
	String xsrfToken = DOECodeCrypt.nextRandomString();
	String accessToken = DOECodeCrypt.generateJWT(currentUser.getApiKey(), xsrfToken);
	String xsrfTokenJson = "{\"xsrfToken\": \"" + xsrfToken + "\" }";
	NewCookie cookie = DOECodeCrypt.generateNewCookie(accessToken);
	System.out.println(accessToken);
        return Response.ok(xsrfTokenJson).cookie(cookie).build();

}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Consumes (MediaType.APPLICATION_JSON)
@Path ("/register")
public Response register(String object) {
	
	System.out.println("Kick off register");
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode returnNode = mapper.createObjectNode();
		JsonNode node = null;
		try {
			node = mapper.readTree(object);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String email = node.get("email").asText();
		String password = node.get("password").asText();
		String confirmPassword = node.get("confirm_password").asText();

		
		
		if (!StringUtils.equals(password, confirmPassword)) {
			
			//returnNode.put
			//return that they don't match...
		}
		String encryptedPassword = PASSWORD_SERVICE.encryptPassword(password);
		
		//check if the email is related to a valid site and assign site ID
		String siteId = "ORNL";
		
		String apiKey = DOECodeCrypt.nextRandomString();
		
		HashSet<String> roles = new HashSet<>();
		roles.add("Admin");
		User newUser = new User(email,encryptedPassword,apiKey,siteId);
		newUser.setRoles(roles);
		
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            em.getTransaction().begin();
            
            em.persist(newUser);
          
            // commit it
            em.getTransaction().commit();
            
            System.out.println("Completed register");
        	String apiKeyJson = "{\"apiKey\": \"" + newUser.getApiKey() + "\" }";
            // we are done here
            return Response.ok(apiKeyJson).build();
        } catch ( Exception e ) {
            if ( em.getTransaction().isActive())
                em.getTransaction().rollback();
            
            System.out.println(e);
            //we'll deal with duplicate user name here as well...
            log.warn("Persistence Error Registering User: " + e.getMessage());
            throw new InternalServerErrorException("IO Error: " + e.getMessage());
        } finally {
            em.close();  
        }
}



}
