/*
 */
package gov.osti.security;

import gov.osti.entity.User;
import gov.osti.listeners.DoeServletContextListener;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a Basic HTTP authentication filter implementation for DOECode.
 * 
 * @author ensornl
 */
public class BasicAuthenticationFilter extends AuthenticatingFilter {
    private static final PasswordService PASSWORD_SERVICE = new DefaultPasswordService();
    private static final Logger log = LoggerFactory.getLogger(BasicAuthenticationFilter.class);

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        return true;
    }

    /**
     * Perform basic HTTP authentication based on the Authorization HTTP header.
     * If no account or password match failure, return a null token, interpreted 
     * as an authentication failure.
     * 
     * @param request the incoming request to process
     * @param response the response to issue
     * @return an AuthenticationToken if possible, or null if login failed
     * @throws Exception on unhandled cases
     */
    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        if ( request instanceof HttpServletRequest ) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String authHeader = httpRequest.getHeader("Authorization");
            if (null!=authHeader) {
                String token = authHeader.substring("Basic".length()).trim();
                String decoded = new String(Base64.decodeBase64(token));
                String[] parts = decoded.split(":");
                
                if (2!=parts.length)
                    throw new IllegalArgumentException("Incorrect header value format.");
                
                EntityManager em = DoeServletContextListener.createEntityManager();
                
                try {
                    TypedQuery<User> getUserByLogin = em.createQuery("SELECT u FROM User u WHERE u.email=:email", User.class)
                            .setParameter("email", parts[0]);
                    User user = getUserByLogin.getSingleResult();
                    
                    // user is not on file
                    if (null==user)
                        throw new AuthenticationException ("User not authorized");
                    // ensure passwords match
                    if ( !PASSWORD_SERVICE.passwordsMatch(parts[1], user.getPassword()) ) {
                        throw new AuthenticationException ("Invalid password.");
                    }
                    // found and matched, return a Bearer Token for this account
                    return new BearerAuthenticationToken(user, user.getApiKey());
                } catch ( IllegalArgumentException e ) {
                    throw new AuthenticationException ("Unable to find User.");
                } finally {
                    em.close();
                }
            }
        }
        
        return null;
    }
    
    @Override
    public boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        try {
            return executeLogin(request, response);
        } catch ( Exception e ) {
            log.warn("Error: " + e.getMessage());
        }
        return false;
    }
}
