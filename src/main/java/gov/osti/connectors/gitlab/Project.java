package gov.osti.connectors.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * Object for GotLab project data, to simplify API interface.
 *
 * @author sowerst
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

    /** public attributes * */
    private Long id;
    private String description = null;
    private String name = null;
    @JsonProperty("name_with_namespace")
    private String fullName = null;
    private String path = null;
    @JsonProperty("path_with_namespace")
    private String fullPath = null;
    @JsonProperty("default_branch")
    private String defaultBranch = null;
    @JsonProperty("web_url")
    private String webUrl = null;
    @JsonProperty("star_count")
    private Integer stars = null;
    @JsonProperty("forks_count")
    private Integer forks = 0;
    private transient Boolean isFork = false;

    public Project() {

    }

    /**
     * Get the unique gitlab ID.
     *
     * @return id the ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Set a unique ID value
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * A description of the gitlab repository project.
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set a description for the repository.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the repository name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set a repository name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return fullName
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * @param fullName the fullName to set
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Get the repository path.
     *
     * @return path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set a repository path.
     *
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return fullPath
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * @param fullPath the fullPath to set
     */
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    /**
     * @return defaultBranch
     */
    public String getDefaultBranch() {
        return StringUtils.isBlank(defaultBranch) ? "master" : defaultBranch;
    }

    /**
     * @param defaultBranch the defaultBranch to set
     */
    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = StringUtils.isBlank(defaultBranch) ? "master" : defaultBranch;
    }

    /**
     * @return webUrl
     */
    public String getWebUrl() {
        return webUrl;
    }

    /**
     * @param webUrl the webUrl to set
     */
    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    /**
     * @return stars
     */
    public Integer getStars() {
        return stars;
    }

    /**
     * @param stars the stars to set
     */
    public void setStars(Integer stars) {
        this.stars = stars;
    }

    /**
     * @return forks
     */
    public Integer getForks() {
        return forks;
    }

    /**
     * @param forks the forks to set
     */
    public void setForks(Integer forks) {
        this.forks = forks;
        setIsFork(forks > 0);
    }

    /**
     * Has this project been forked?
     *
     * @return isFork true if forked, false if not
     */
    public Boolean getIsFork() {
        return isFork;
    }

    /**
     * Set whether or not this project has been forked.
     *
     * @param isFork set whether or not the project was forked
     */
    private void setIsFork(Boolean isFork) {
        this.isFork = isFork;
    }

}
