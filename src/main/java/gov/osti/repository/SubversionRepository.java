/*
 */
package gov.osti.repository;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

/**
 *
 * @author ensornl
 */
public class SubversionRepository {
    private static Logger log = LoggerFactory.getLogger(SubversionRepository.class);
    
    /**
     * Assert validity of a URL as a GIT REPOSITORY.
     * 
     * @param url the URL to check
     * @return true if this URL points to a git repository, false if not, or unable to tell
     */
    public static boolean isValid(String url) {
        SVNRepository repository = null;
        
        try {
            SVNURL repoUrl = SVNURL.parseURIEncoded(url);
            repository = SVNRepositoryFactory.create(repoUrl);
            
            Collection logs = repository.log(new String[] { "" }, null, 0, -1, true, true);
            
            // if we have some sort of log entry (even initial import), we are valid
            return !logs.isEmpty();
        } catch ( Exception e ) {
            log.warn("SVN Error for " + url + ": " + e.getMessage());
            return false;
        } finally {
            if (null!=repository)
                repository.closeSession();
        }
    }
}
