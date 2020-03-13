/*
 */
package gov.osti.repository;

import java.util.Collection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ensornl
 */
public class GitRepository {
    private static final Logger log = LoggerFactory.getLogger(GitRepository.class);

    public static boolean isValid(String url) {
        // for GIT repos, append ".git" as a suffix
        url = url.replaceFirst("(?:\\/|[.]git)?$", ".git");

        try {
            Collection<Ref> references = Git
                    .lsRemoteRepository()
                    .setHeads(true)
                    .setTags(true)
                    .setRemote(url)
                    .call();

            // if we get here with no EXCEPTION, assume it's a VALID REPOSITORY.
            return true;
        } catch ( Exception e ) {
            // jgit occasionally throws sloppy runtime exceptions
            log.warn("Repository URL " + url + " failed: " + e.getMessage());
            return false;
        }
    }
}
