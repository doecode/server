package gov.osti.services;

import java.util.Calendar;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.osti.security.JWTCrypt;

@Path("login")
public class Login {

private static Logger log = LoggerFactory.getLogger(Login.class);

	
public Login() {
	
}


@POST
@Produces(MediaType.APPLICATION_JSON)
@Consumes (MediaType.APPLICATION_JSON)
@Path ("/login")
public Response login(String object) {
	String xsrfToken = JWTCrypt.nextRandomString();
	String accessToken = "{\"accessToken\": \"" + JWTCrypt.generateJWT("123", xsrfToken) + "\" }";
	String xsrfTokenJson = "{\"xsrfToken\": \"" + xsrfToken + "\" }";
	Calendar c = Calendar.getInstance();
	c.add(Calendar.MINUTE, 30);

	NewCookie cookie = new NewCookie(new Cookie("accessToken", accessToken), "", 60*30, c.getTime(),true,true);
	System.out.println(accessToken);
        return Response.ok(xsrfTokenJson).cookie(cookie).build();

}

}
