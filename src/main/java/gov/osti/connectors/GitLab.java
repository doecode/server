package gov.osti.connectors;

import gov.osti.connectors.gitlab.Project;
import gov.osti.connectors.api.GitLabAPI;
import com.fasterxml.jackson.databind.JsonNode;
import gov.osti.entity.DOECodeMetadata;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GitLab metadata scraper class, to acquire/write relevant data
 * from/to the GitLab public API (or local OSTI API for OSTI Hosted).
 *
 * @author sowerst
 */
public class GitLab implements ConnectorInterface {

    /** a logger implementation * */
    private static final Logger log = LoggerFactory.getLogger(GitLab.class);

    /** authentication information for accessing GitLab API * */
    /**
     * Initialize and read the properties for configuration purposes.
     *
     * Obtains connector authentication information from properties files.
     *
     * @throws IOException on file IO errors
     */
    @Override
    public void init() throws IOException {
    }

    /**
     * Obtain the connection-driven metadata elements from GitLab public API
     * requests, or OSTI GitLab authenticated requests.
     *
     * @param url the URL to process
     *
     * @return a JsonElement of the DOECodeMetadata filled in as possible from
     * the API
     */
    @Override
    public JsonNode read(String url) {
        DOECodeMetadata md = new DOECodeMetadata();

        try {
            GitLabAPI glApi = new GitLabAPI(url);

            // try to identify the NAME of the project
            if (null == glApi.getProjectName())
                return null;

            // try to get the metadata YAML file first
            JsonNode yaml = HttpUtil.readMetadataYaml(glApi.acquireRawFileGet("metadata.yml"));
            // if it's not empty, use that
            if (null != yaml)
                return yaml;
            // try alternate metadata name
            yaml = HttpUtil.readMetadataYaml(glApi.acquireRawFileGet(".metadata.yml"));
            if (null != yaml)
                return yaml;
            // try alternate name
            yaml = HttpUtil.readMetadataYaml(glApi.acquireRawFileGet("doecode.yml"));
            if (null != yaml)
                return yaml;
            // try alternate doecode name
            yaml = HttpUtil.readMetadataYaml(glApi.acquireRawFileGet(".doecode.yml"));
            if (null != yaml)
                return yaml;

            // Convert the JSON into an Object we can handle
            Project project = glApi.fetchProject();

            // parse the relevant response parts into Metadata
            md.setSoftwareTitle(project.getName());
            md.setAcronym(project.getFullPath());
            md.setDescription(project.getDescription());

            /*
             * TODO:
             * Scrape Contributors/Developers
             * name/email/affiliations
             */
            return md.toJson();
        } catch (IOException e) {
            // here's where you'd warn about the IO error
            log.warn("IO Error reading GitLab information: " + e.getMessage());
            log.warn("Read from " + url);
        }
        // unable to process this one
        return null;
    }

}
