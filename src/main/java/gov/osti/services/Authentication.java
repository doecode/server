package gov.osti.services;

import javax.servlet.http.Cookie;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.osti.security.JWTCrypt;

@Path("authentication")
public class Authentication {

private static Logger log = LoggerFactory.getLogger(Authentication.class);

	
public Authentication() {
	
}

@GET
@Path ("/check")
@RequiresPermissions("Admin")
public Response check() {

	Subject subject = SecurityUtils.getSubject();
    System.out.println(subject.getPrincipal() + " " + subject.isAuthenticated());

    try {
    	subject.checkRole("Admin");
    	System.out.println("Easy Win");
    } catch (AuthorizationException e) {
    	System.out.println(e);
    }

    return Response.status(Response.Status.OK).header("Access-Control-Allow-Origin","*").header("Access-Control-Allow-Credentials","true")
    		.header("Access-Control-Allow-Headers","Content-Type, Accept, X-Requested-With").header("Access-Control-Allow-Methods","GET,POST,DELETE,PUT,OPTIONS,HEAD").build();
}
}
