package gov.osti.security;

import java.security.Key;
import java.util.Calendar;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;

public class JWTCrypt {
	private static final long minute = 60000;
	private static final long timeout = 30;
	
	public static String generateJWT(String userID) {
		
		   Calendar date = Calendar.getInstance();
		   long time = date.getTimeInMillis();
		   Date expiration = new Date(time + (timeout * minute));
	       return Jwts.builder().setIssuer("doecode").setSubject(userID).setExpiration(expiration).signWith(SignatureAlgorithm.HS256,"Secret").compact();
	       
		
	}
	
	public static Claims parseJWT(String jwt) {
		Claims claims = Jwts.parser().setSigningKey("Secret").parseClaimsJws(jwt).getBody();
		return claims;
	}
}
