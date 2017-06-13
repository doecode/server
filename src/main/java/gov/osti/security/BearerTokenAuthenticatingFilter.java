package gov.osti.security;

import java.util.Date;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.NewCookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
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
		HttpServletRequest req = (HttpServletRequest) request;
		//go through cookies and pull out accessToken
		Cookie[] cookies = req.getCookies();
		String cookieVal = null;
		for (Cookie c : cookies) {
			if (StringUtils.equals(c.getName(),"accessToken"))
				cookieVal = c.getValue();
		}
		String authorizationHeader = req.getHeader("Authorization");
		
		if (cookieVal == null &&  ( authorizationHeader == null || !authorizationHeader.startsWith("Basic "))) {
			throw new AuthenticationException("Authentication method not provided");
		}
		

		if (cookieVal != null) {
			System.out.println("processing cookie");
			Claims claims = DOECodeCrypt.parseJWT(cookieVal);
			String xsrfToken = (String) claims.get("xsrfToken");
			String xsrfHeader = req.getHeader("X-XSRF-TOKEN");
			System.out.println(xsrfToken);
			
			if (!StringUtils.equals(xsrfHeader,xsrfToken)) {
				throw new AuthenticationException("XSRF Tokens did not match");			
			}
			
			Date now = new Date();
			if (now.after(claims.getExpiration()))
				throw new AuthenticationException("Token is expired");
			
			return new BearerAuthenticationToken(claims.getSubject(), xsrfToken);
			
		} else {
			return new BearerAuthenticationToken(authorizationHeader.substring("Basic".length()).trim());
		}
		
	
	}
	
	//for now, throw a forbidden and let the front end handle it
	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
		System.out.println("Denied access");
		HttpServletResponse res = (HttpServletResponse) response;
		res.setStatus(HttpServletResponse.SC_FORBIDDEN);
		return false;
	}
	
	@Override	
	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
		try {
			return executeLogin(request,response);
		} catch (Exception e) {
			System.out.println(e);
			return false;
		}
	}
	
	
	//update cookie/jwt expiration and reissue if there is, this time with updated expiration info
	@Override
	protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
		HttpServletResponse res = (HttpServletResponse) response;
		BearerAuthenticationToken bat = (BearerAuthenticationToken) token;
		if (StringUtils.isNotBlank(bat.getXsrfToken())) {
			String accessToken = DOECodeCrypt.generateJWT((String) bat.getPrincipal(), bat.getXsrfToken());
			NewCookie cookie = DOECodeCrypt.generateNewCookie(accessToken);
			res.setHeader("SET-COOKIE", cookie.toString());
		}
		System.out.println("Success");
		return true;
	}

}
