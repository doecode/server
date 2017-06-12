package gov.osti.security;

import java.math.BigInteger;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;

public class JWTCrypt {
	private static final long minute = 60000;
	private static final long timeout = 30;
    private static final SecureRandom random = new SecureRandom();

    public static String nextRandomString() {
        return new BigInteger(130, random).toString(32);
    }
	
	public static String generateJWT(String userID, String xsrfToken) {
		
		   Calendar date = Calendar.getInstance();
		   long time = date.getTimeInMillis();
		   Date expiration = new Date(time + (timeout * minute));
	       return Jwts.builder().setIssuer("doecode").claim("xsrfToken", xsrfToken).setSubject(userID).setExpiration(expiration).signWith(SignatureAlgorithm.HS256,"Secret").compact();
	       
		
	}
	
	public static Claims parseJWT(String jwt) {
		Claims claims = Jwts.parser().setSigningKey("Secret").parseClaimsJws(jwt).getBody();
		return claims;
	}
	
	public static NewCookie generateNewCookie(String accessToken) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, 30);
		
		Cookie cookie = new Cookie("accessToken", accessToken, "/", "localhost:8081");

		return new NewCookie(cookie, "", 60*30, c.getTime(),true,true);
	}
}
