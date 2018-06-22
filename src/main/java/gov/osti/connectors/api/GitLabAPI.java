package gov.osti.connectors.api;

import com.fasterxml.jackson.databind.JsonNode;
import gov.osti.connectors.gitlab.Project;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.osti.connectors.gitlab.Commit;
import gov.osti.connectors.gitlab.GitLabFile;
import gov.osti.connectors.gitlab.Namespace;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.listeners.DoeServletContextListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GitLab metadata scraper class, to acquire/write relevant data
 * from/to the GitLab public API (or local OSTI API for OSTI Hosted).
 *
 * @author sowerst
 */
public final class GitLabAPI {

    /** a logger implementation * */
    private final Logger log = LoggerFactory.getLogger(GitLabAPI.class);
    /** authentication information for accessing GitLab API * */
    private String API_TOKEN = "";
    private String GITLAB_OSTI_BASE_URL = "";
    private String GITLAB_OSTI_NAMESPACE = "doecode";
    /** GitLab API base URL * */
    private static final String GITLAB_BASE_URL = "https://gitlab.com";
    private static final String API_PATH = "/api/v4/";

    private static final JsonNodeFactory FACTORY_INSTANCE = JsonNodeFactory.instance;
    private static final ObjectMapper MAPPER = new ObjectMapper().setTimeZone(TimeZone.getDefault());

    private String currentProjectName = "";
    private String currentApiBase = "";
    private String currentBranch = "master";
    private Namespace currentNamespace = null;

    private final Set<Integer> goodResponses = new HashSet<>(Arrays.asList(HttpServletResponse.SC_OK, HttpServletResponse.SC_CREATED, HttpServletResponse.SC_NO_CONTENT));

    public static enum HttpType {
        GET, POST, PUT;
    }

    /**
     * Initialize and read the properties for configuration purposes.
     *
     * Obtains authentication information from properties files.
     *
     * @throws IOException on file IO errors
     */
    public GitLabAPI() throws IOException {
        this(null);
    }

    /**
     * Initialize and read the properties for configuration purposes, based on a specific GitLab URL.
     *
     * @param url
     * @throws IOException
     */
    public GitLabAPI(String url) throws IOException {
        GITLAB_OSTI_BASE_URL = DoeServletContextListener.getConfigurationProperty("gitlab.osti.baseurl");
        API_TOKEN = DoeServletContextListener.getConfigurationProperty("gitlab.osti.token");
        GITLAB_OSTI_NAMESPACE = DoeServletContextListener.getConfigurationProperty("gitlab.osti.namespace");
        GITLAB_OSTI_NAMESPACE = StringUtils.isBlank(GITLAB_OSTI_NAMESPACE) ? "doecode" : GITLAB_OSTI_NAMESPACE;

        if (!StringUtils.isBlank(url))
            initViaUrl(url);

        // if no API Base is set, set to OSTI if it is set, otherwise set to GitLab
        if (StringUtils.isBlank(currentApiBase))
            setApiBaseViaUrl(StringUtils.isBlank(GITLAB_OSTI_BASE_URL) ? GITLAB_BASE_URL : GITLAB_OSTI_BASE_URL);

        if (currentApiBase.contains(GITLAB_OSTI_BASE_URL))
            setNamespace(fetchNamespace(GITLAB_OSTI_NAMESPACE));
    }

    // encode value for URL usage
    private String encodeValue(String value) {
        String encodedValue;
        try {
            encodedValue = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            encodedValue = value;
        }
        return encodedValue;
    }

    /**
     * Retrieve just the String content from a given HttpGet request.
     *
     * @param get the GET to execute
     * @return String contents of the results
     * @throws IOException on IO errors
     */
    @SuppressWarnings("ConvertToTryWithResources")
    private String fetch(HttpGet get) throws IOException {
        // create an HTTP client to request through
        CloseableHttpClient hc
                = HttpClientBuilder
                        .create()
                        .setDefaultRequestConfig(RequestConfig
                                .custom()
                                .setConnectTimeout(60000)
                                .setConnectionRequestTimeout(60000)
                                .setSocketTimeout(60000)
                                .build())
                        .build();

        try {
            // only return if response is OK
            HttpResponse response = hc.execute(get);
            processResponse(response);

            return EntityUtils.toString(response.getEntity());
        } finally {
            hc.close();
        }
    }

    /**
     * Retrieve just the String content from a given HttpPost request.
     *
     * @param post the POST to execute
     * @return String contents of the results
     * @throws IOException on IO errors
     */
    @SuppressWarnings("ConvertToTryWithResources")
    private String post(HttpPost post) throws IOException {
        // create an HTTP client to request through
        CloseableHttpClient hc
                = HttpClientBuilder
                        .create()
                        .setDefaultRequestConfig(RequestConfig
                                .custom()
                                .setConnectTimeout(60000)
                                .setConnectionRequestTimeout(60000)
                                .setSocketTimeout(60000)
                                .build())
                        .build();

        try {
            // only return if response is OK
            HttpResponse response = hc.execute(post);
            processResponse(response);

            return EntityUtils.toString(response.getEntity());
        } finally {
            hc.close();
        }
    }

    /**
     * Retrieve just the String content from a given HttpPut request.
     *
     * @param put the PUT to execute
     * @return String contents of the results
     * @throws IOException on IO errors
     */
    @SuppressWarnings("ConvertToTryWithResources")
    private String put(HttpPut put) throws IOException {
        // create an HTTP client to request through
        CloseableHttpClient hc
                = HttpClientBuilder
                        .create()
                        .setDefaultRequestConfig(RequestConfig
                                .custom()
                                .setConnectTimeout(60000)
                                .setConnectionRequestTimeout(60000)
                                .setSocketTimeout(60000)
                                .build())
                        .build();

        try {
            // only return if response is OK
            HttpResponse response = hc.execute(put);
            processResponse(response);

            return EntityUtils.toString(response.getEntity());
        } finally {
            hc.close();
        }
    }

    /**
     * Process the API response, based on API validation/error documentation.
     *
     * @param response the HTTP Response to process
     * @return String consolidated error message contents, if not successful
     * @throws IOException on IO errors
     */
    private String processResponse(HttpResponse response) throws IOException {
        // get status code
        int statusCode = response.getStatusLine().getStatusCode();

        // if successful, just return, no errors to parse
        if (goodResponses.contains(statusCode))
            return null;

        // if not found, throw specific error for special handling
        if (HttpServletResponse.SC_NOT_FOUND == statusCode)
            throw new IOException("404 Not Found");

        // otherwise, get API response body data
        String responseString = EntityUtils.toString(response.getEntity());
        JsonNode root = MAPPER.readTree(responseString);

        // if response has an error, use it
        if (root.has("error"))
            responseString = root.get("error").asText();
        // otherwise, use message info, if exists
        else if (root.has("message"))
            responseString = parseErrors(root.get("message"));

        throw new IOException(responseString);
    }

    /**
     * Recurse into JsonNode pulling out error messages, based on v4 API documentation
     *
     * @param node the JsonNode to iterate on
     * @return String of error message.
     */
    private String parseErrors(JsonNode node) {
        String errors = "";
        Iterator<Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) fields.next();

            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (value.isArray()) {
                List<String> msgs = new ArrayList<>();
                for (final JsonNode msg : value)
                    if (!StringUtils.isBlank(msg.asText()))
                        msgs.add(msg.asText());

                errors += (StringUtils.isBlank(errors) ? "" : "; ") + "'" + key + "' - " + String.join("|", msgs);
            } else if (value.isObject())
                errors += (StringUtils.isBlank(errors) ? "" : "; ") + key + ":" + parseErrors(value);
        }

        return errors;
    }

    /**
     * Construct a GET request to the GitLab API.
     *
     * @param url the base URL to use
     * @return HttpGet Object with any needed authentication
     */
    private HttpGet gitLabAPIGet(String url) {
        HttpGet get = new HttpGet(url);
        // if there is a token and we are calling the OSTI GitLab, authenticate via header information
        // prevents API access limitations if authenticated
        if (!"".equals(API_TOKEN) && url.toLowerCase().contains(GITLAB_OSTI_BASE_URL.toLowerCase()))
            get.addHeader("Private-Token", API_TOKEN);

        return get;
    }

    /**
     * Construct a POST request to the GitLab API.
     *
     * @param url the base URL to use
     * @return HttpPost Object with any needed authentication
     */
    private HttpPost gitLabAPIPost(String url) {
        HttpPost post = new HttpPost(url);
        // if there is a token and we are calling the OSTI GitLab, authenticate via header information
        // prevents API access limitations if authenticated
        if (!"".equals(API_TOKEN) && url.toLowerCase().contains(GITLAB_OSTI_BASE_URL.toLowerCase()))
            post.addHeader("Private-Token", API_TOKEN);

        post.addHeader("Content-Type", "application/json");

        return post;
    }

    /**
     * Construct a PUT request to the GitLab API.
     *
     * @param url the base URL to use
     * @return HttpPost Object with any needed authentication
     */
    private HttpPut gitLabAPIPut(String url) {
        HttpPut put = new HttpPut(url);
        // if there is a token and we are calling the OSTI GitLab, authenticate via header information
        // prevents API access limitations if authenticated
        if (!"".equals(API_TOKEN) && url.toLowerCase().contains(GITLAB_OSTI_BASE_URL.toLowerCase()))
            put.addHeader("Private-Token", API_TOKEN);

        put.addHeader("Content-Type", "application/json");

        return put;
    }

    /**
     * Attempt to identify the PROJECT NAME from a given URL.
     *
     * Criteria: URL host should contain "gitlab.com" or the OSTI GitLab repo; the project is assumed
     * to be the first two components of the PATH, splitting on the slash.
     * (owner/project)
     *
     * @param url the URL to process
     * @return the PROJECT NAME if able to parse; null if not, or unrecognized
     * URL
     */
    private String getProjectFromUrl(String url) {
        try {
            String safeUrl = (null == url) ? "" : url.trim();
            // no longer assuming protocol, must be provided
            URI uri = new URI(safeUrl);

            String host = uri.getHost().toLowerCase();

            // protection against bad URL input
            if (null != host)
                if (host.contains("gitlab.com") || safeUrl.toLowerCase().contains(GITLAB_OSTI_BASE_URL)) {
                    String path = uri.getPath();
                    return path.substring(path.indexOf("/") + 1)
                            .replaceAll("/$", ""); // remove the trailing slash if present
                }
        } catch (URISyntaxException e) {
            // warn that URL is not a valid URI
            log.warn("Not a valid URI: " + url + " message: " + e.getMessage());
        } catch (Exception e) {
            // some unexpected error happened
            log.warn("Unexpected Error from " + url + " message: " + e.getMessage());
        }

        return null;
    }

    /**
     * Attempt to identify the BASE URL from the given URL. (supports OSTI and non-OSTI GitLab)
     *
     * Criteria: URL host should be a valid GitLab
     *
     * @param url the URL to process
     * @return the BASE URL API path if able to parse; Default GitLab BASE if not, or unrecognized
     * URL
     */
    private String getGitLabBaseFromUrl(String url) {
        try {
            String safeUrl = (null == url) ? "" : url.trim();
            // no longer assuming protocol, must be provided
            URL aUrl = new URL(safeUrl);

            String protocol = aUrl.getProtocol();
            String host = aUrl.getHost();

            // if main GitLab is host, then no need to build a URL from parts.
            if (host.toLowerCase().contains("gitlab.com"))
                return GITLAB_BASE_URL + API_PATH;

            // protection against bad URL input
            if (null != aUrl.getHost())
                return protocol + "://" + host + API_PATH;
        } catch (MalformedURLException e) {
            // warn that URL is not a valid URI
            log.warn("Not a valid URL: " + url + " message: " + e.getMessage());
        } catch (Exception e) {
            // some unexpected error happened
            log.warn("Unexpected Error from " + url + " message: " + e.getMessage());
        }

        return GITLAB_BASE_URL + API_PATH;
    }

    // shortcut for setting both Project and Base API from a URL
    private void initViaUrl(String url) {
        setProjectNameViaUrl(url);
        setApiBaseViaUrl(url);
    }

    /**
     * Get the current branch being used when interacting with files via the API.
     *
     * @return branch as a string
     */
    public String getBranch() {
        return StringUtils.isBlank(currentBranch) ? "master" : currentBranch;
    }

    /**
     * Set the GitLab branch to use when interacting with files via the API.
     *
     * @param branch
     */
    public void setBranch(String branch) {
        currentBranch = StringUtils.isBlank(branch) ? "master" : branch;
    }

    /**
     * Get the current Project being used for API commands.
     *
     * @return project name as string
     */
    public String getProjectName() {
        return currentProjectName;
    }

    /**
     * Set the Project for which to target API calls.
     *
     * @param projectName
     */
    public void setProjectName(String projectName) {
        currentProjectName = projectName;
    }

    /**
     * Set the Project for which to target API calls, based on a GitLab URL.
     *
     * @param url
     */
    public void setProjectNameViaUrl(String url) {
        setProjectName(getProjectFromUrl(url));
    }

    /**
     * Get the current API Base being used for API commands.
     *
     * @return
     */
    public String getApiBase() {
        return currentApiBase;
    }

    /**
     * Set the API Base for use when calling the GitLab API (could be local or not).
     *
     * @param apiBase
     */
    public void setApiBase(String apiBase) {
        currentApiBase = apiBase;
    }

    /**
     * Set the API Base regardless if it is local or not, based on a GitLab URL.
     *
     * @param url
     */
    public void setApiBaseViaUrl(String url) {
        setApiBase(getGitLabBaseFromUrl(url));
    }

    /**
     * Get namespace to use when updating projects that are in a group.
     *
     * @return Namespace object containing GitLab Namespace information
     */
    public Namespace getNamespace() {
        return currentNamespace;
    }

    /**
     * Set namespace to use when updating projects that are in a group.
     *
     * @param namespace
     */
    public void setNamespace(Namespace namespace) {
        currentNamespace = namespace;
    }

    // generate a valid GitLab API "tree" command for the currently set GitLab and Project
    private String acquireTreeCmd(String path) {
        return acquireProjectCmd() + "/repository/tree/?recursive=true&ref=" + encodeValue(getBranch()) + (StringUtils.isBlank(path) ? "" : "&path="
                + encodeValue(path));
    }

    /**
     * Fetch an array of GitLabFile objects for a specific GitLab repo.
     * Will use the Project defined by the setProject method.
     *
     * @return GitLabFile[] array of objects with GitLab file information.
     * @throws IOException
     */
    public GitLabFile[] fetchTree() throws IOException {
        return fetchTree(null);
    }

    /**
     * Fetch an array of GitLabFile objects for a specific GitLab tree path.
     * Will use the Project defined by the setProject method.
     *
     * @param path
     * @return GitLabFile[] array of objects with GitLab file information.
     * @throws IOException
     */
    public GitLabFile[] fetchTree(String path) throws IOException {
        String response = "";
        try {
            response = fetch(gitLabAPIGet(acquireTreeCmd(path)));
        } catch (IOException e) {
            // if we didn't find Files on GET, its okay, they just doesn't exist.  Return NULL.
            if (!e.getMessage().equals("404 Not Found"))
                throw e;

            return null;
        }
        return MAPPER.readValue(response, GitLabFile[].class);
    }

    // generate a valid GitLab API "raw file" command for the currently set GitLab and Project
    private String acquireRawFileCmd(String fileName) {
        return acquireProjectCmd() + "/repository/files/" + encodeValue(fileName) + "/raw?ref=" + encodeValue(getBranch());
    }

    /**
     * Generate an HttpGet object for use with this GitLab repo.
     * Will use the Project defined by the setProject method.
     *
     * @param fileName
     * @return HttpGet with any needed authentication
     */
    public HttpGet acquireRawFileGet(String fileName) {
        return gitLabAPIGet(acquireRawFileCmd(fileName));
    }

    /**
     * Fetch the raw data of a file from GitLab with the Repositories API of GitLab.
     * Will use the Project defined by the setProject method.
     *
     * @param fileName
     * @return raw file as string
     * @throws IOException
     */
    public String fetchRawFile(String fileName) throws IOException {
        return fetch(gitLabAPIGet(acquireRawFileCmd(fileName)));
    }

    // generate a valid GitLab API "namespace" command for the currently set GitLab and Project
    private String acquireNamespaceCmd(String namespace) {
        String defaultNamespace = "";

        String encodedNamespace;
        namespace = StringUtils.isBlank(namespace) ? defaultNamespace : namespace;
        try {
            encodedNamespace = URLEncoder.encode(namespace, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            encodedNamespace = defaultNamespace;
        }

        encodedNamespace = (StringUtils.isBlank(encodedNamespace) ? "" : "/") + encodedNamespace;

        return getApiBase() + "namespaces" + encodedNamespace;
    }

    // generate a valid GitLab API "create project" command for the currently set GitLab and Project
    private String acquireCreateProjectCmd() {
        return getApiBase() + "projects";
    }

    // generate a valid GitLab API "project" command for the currently set GitLab and Project
    private String acquireProjectCmd() {
        String namespace = "";
        if (getNamespace() != null)
            namespace = getNamespace().getFullPath() + "/";

        return getApiBase() + "projects/" + encodeValue(namespace + getProjectName());
    }

    /**
     * Create a new Project in GitLab, based on a DOECodeMetadata object.
     *
     * @param md
     * @return Project object containing GitLab Project information
     * @throws IOException
     */
    public Project createProject(DOECodeMetadata md) throws IOException {
        return interfaceWithProject(HttpType.POST, generateProductBody(md));
    }

    /**
     * Update the Project data in GitLab, based on a DOECodeMetadata object.
     *
     * @param md
     * @return Project object containing GitLab Project information
     * @throws IOException
     */
    public Project updateProject(DOECodeMetadata md) throws IOException {
        return interfaceWithProject(HttpType.PUT, generateProductBody(md));
    }

    // generate POST/PUT body JSON as a String, for a DOECodeMetadata object.
    private String generateProductBody(DOECodeMetadata md) throws IOException {
        ObjectNode body = new ObjectNode(FACTORY_INSTANCE);

        body.put("path", getProjectName().toLowerCase());
        body.put("visibility", "private");
        body.put("request_access_enabled", "true");

        if (md != null) {
            // GitLab API only allows a narrow case of characters
            body.put("name", getProjectName().toUpperCase() + " - " + md.getSoftwareTitle().replaceAll("(?i)((?![A-Z]|[0-9]|[_.\\- ]).)+", ""));
            body.put("description", md.getDescription());
        }

        if (getNamespace() != null)
            body.put("namespace_id", getNamespace().getId());

        return MAPPER.writeValueAsString(body);
    }

    /**
     * Central logic for interfacing with the Project API of GitLab.
     * Will use the Project defined by the setProject method.
     *
     * @return Project object containing GitLab Project information
     * @throws IOException
     */
    public Project fetchProject() throws IOException {
        return interfaceWithProject(HttpType.GET);
    }

    private Project interfaceWithProject(HttpType type) throws IOException {
        return interfaceWithProject(type, null);
    }

    private Project interfaceWithProject(HttpType type, String body) throws IOException {
        String response = "";

        switch (type) {
            case GET:
                try {
                    response = fetch(gitLabAPIGet(acquireProjectCmd()));
                } catch (IOException e) {
                    // if we didn't find Project on GET, its okay, it just doesn't exist.  Return NULL.
                    if (!e.getMessage().equals("404 Not Found"))
                        throw e;

                    return null;
                }
                break;
            case POST:
                HttpPost post = gitLabAPIPost(acquireCreateProjectCmd());
                if (!StringUtils.isBlank(body)) {
                    HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
                    post.setEntity(entity);
                }
                response = post(post);
                break;
            case PUT:
                HttpPut put = gitLabAPIPut(acquireProjectCmd());
                if (!StringUtils.isBlank(body)) {
                    HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
                    put.setEntity(entity);
                }
                response = put(put);
                break;
            default:
                break;
        }

        return MAPPER.readValue(response, Project.class);
    }

    /**
     * Fetch data for a specific namespace from GitLab
     *
     * @param namespace
     * @return Namespace object containing GitLab Namespace information
     * @throws IOException
     */
    public Namespace fetchNamespace(String namespace) throws IOException {
        Namespace ns = null;

        try {
            String response = fetch(gitLabAPIGet(acquireNamespaceCmd(namespace)));
            return MAPPER.readValue(response, Namespace.class);
        } catch (IOException e) {
            // if we didn't find Project, its okay.
            if (!e.getMessage().equals("404 Not Found"))
                throw e;
        }

        return ns;
    }

    private String acquireCommitCmd() {
        return acquireProjectCmd() + "/repository/commits";
    }

    /**
     * Commit the files store in the Commit class to the project.
     *
     * @param commit
     * @throws IOException
     */
    public void commitFiles(Commit commit) throws IOException {
        HttpPost post = gitLabAPIPost(acquireCommitCmd());
        String body = MAPPER.writeValueAsString(commit);

        if (!StringUtils.isBlank(body)) {
            HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
            post.setEntity(entity);
        }
        post(post);
    }

}
