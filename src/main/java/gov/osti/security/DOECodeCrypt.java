package gov.osti.security;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class DOECodeCrypt {

    private static final SecureRandom random = new SecureRandom();

    public static String nextRandomString() {
        return new BigInteger(130, random).toString(32);
    }
	
    public static String nextUniqueString() {
    	return nextRandomString() + "x" +  System.currentTimeMillis(); 
    }
	public static String generateLoginJWT(String userID, String xsrfToken) {
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, 30);
	    return Jwts.builder().setIssuer("doecode").claim("xsrfToken", xsrfToken).setSubject(userID).setExpiration(c.getTime()).signWith(SignatureAlgorithm.HS256,"Secret").compact();
	       
		
	}
	
	public static String generateConfirmationJwt(String confirmationCode, String email) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, 30);
	    return Jwts.builder().setIssuer("doecode").setId(confirmationCode).setSubject(email).setExpiration(c.getTime()).signWith(SignatureAlgorithm.HS256,"Secret").compact();
	}
	
	public static Claims parseJWT(String jwt) {
		Claims claims = Jwts.parser().setSigningKey("Secret").parseClaimsJws(jwt).getBody();
		return claims;
	}
	
	
	public static NewCookie generateNewCookie(String accessToken) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, 30);
		
		Cookie cookie = new Cookie("accessToken", accessToken, "/", "");

		return new NewCookie(cookie, "", 60*30, c.getTime(),false,true);
	}
}
