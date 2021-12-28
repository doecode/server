/*
 */
package gov.osti.listeners;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web application lifecycle listener.
 *
 * @author ensornl
 */
public class DoeServletContextListener implements ServletContextListener {
    // a Logger instance
    private static final Logger log = LoggerFactory.getLogger(DoeServletContextListener.class);
    
    // get an instance of EntityManagerFactory for persistence
    private static EntityManagerFactory emf = null;
    
    // Map of configured service parameters
    private static Properties configuration;
    
    /**
     * Obtain the named configuration property from the "doecode.properties"
     * configuration file, if possible.
     * 
     * @param key the KEY name requested
     * @return the VALUE if found in the configuration properties, or blank
     * if not found or not set
     */
    public static String getConfigurationProperty(String key) {
        // lazy-load first time
        if (null==configuration) {
            configuration = new Properties(); // create a new instance
            InputStream in; // read from the ClassLoader
            
            try {
                in = DoeServletContextListener.class.getClassLoader().getResourceAsStream("doecode.properties");
                if (null!=in) configuration.load(in);
                if (in != null) try{in.close();} catch (Exception e) {}
            } catch ( IOException e ) {
                log.warn("Context Initialization Failure: " + e.getMessage());
            }
        }
        // if the KEY is present, and DOES NOT start with "$", return it
        // otherwise, get an empty String
        return  configuration.containsKey(key) ?
                configuration.getProperty(key).startsWith("$") ?
                "" : configuration.getProperty(key) :
                "";
    }
    
    /**
     * Start up the services on deployment.
     * 
     * @param sce the ServletContextEvent triggering this function
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // attempt to load the persistence layer
        String persistence_unit = sce.getServletContext().getInitParameter("persistence_unit");
        emf = Persistence.createEntityManagerFactory(persistence_unit);
        
        log.info("DOE CODE instance started.");
    }

    /**
     * Free resources appropriately before undeployment.
     * @param sce the ServletContextEvent triggering the call
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // close down the Entity Manager
        log.info("Shutting down DOE CODE application.");
        if (null!=emf)
            emf.close();
    }
    
    /**
     * Acquire an EntityManager for persistence operations.  Handling the resulting
     * EntityManager is the responsibility of the caller.  Make sure it is closed
     * appropriately.
     * 
     * @return an EntityManager from the Factory if possible
     */
    public static EntityManager createEntityManager() {
        if (null==emf)
            throw new IllegalStateException("Context not initialized!");
        
        return emf.createEntityManager();
    }

    /**
     * Refresh the caches.
     */
    public static void refreshCaches() {
        if (null == emf)
            throw new IllegalStateException("Context not initialized!");

        emf.getCache().evictAll();
    }
}
