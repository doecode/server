package gov.osti.security;

import java.util.Date;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.osti.services.Login;
import io.jsonwebtoken.Claims;

public class BearerTokenAuthenticatingFilter extends AuthenticatingFilter {
	private static Logger log = LoggerFactory.getLogger(BearerTokenAuthenticatingFilter.class);
	@Override
	protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
		System.out.println("Creating token");
		HttpServletRequest req = (HttpServletRequest) request;
		String cookieVal = req.getHeader("Cookie");
		System.out.println(cookieVal);
		String authorizationHeader = req.getHeader("Authorization");
		
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			return null;
		}
		
		String jwt = authorizationHeader.substring("Bearer".length()).trim();
		
		Claims claims = JWTCrypt.parseJWT(jwt);
		Date now = new Date();
		if (now.after(claims.getExpiration()))
				return null;
		System.out.println(claims.getSubject());
		return new BearerAuthenticationToken(claims.getSubject());
	}
	
	//do we redirect them to login? that would be my guess, but maybe we do that from the front end and stash away the store there...l
	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("Denied access, likely because login is not yet correctly configured");
		return false;
	}
	
	@Override
	
	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
		System.out.println("Say something");
		log.info("hello");
		try {
			return executeLogin(request,response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Hit exception");
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	
	//add updated bearer token header, this time with updated expiration info
	@Override
	protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
		HttpServletResponse h = (HttpServletResponse) response;
		String userId = (String) subject.getPrincipal();
		System.out.println("User ID after success is... " + userId);
		Cookie cookie = new Cookie("accessToken", JWTCrypt.generateJWT(userId));
		h.addCookie(cookie);
		System.out.println("Bingo");
		return true;
	}

}
