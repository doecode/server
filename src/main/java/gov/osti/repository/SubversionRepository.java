/*
 */
package gov.osti.repository;

import java.util.Date;
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

            long rev = repository.getDatedRevision(new Date());

            // if we passed the dated revision check, we are valid
            return true;
        } catch ( Exception e ) {
            log.warn("SVN Error for " + url + ": " + e.getMessage());
            return false;
        } finally {
            if (null!=repository)
                repository.closeSession();
        }
    }
}
