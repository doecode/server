package gov.osti.connectors.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds the namespace data from the GitLab Namespaces API.
 *
 * @author sowerst
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Namespace {

    /** public attributes * */
    private Long id;
    private String name = null;
    private String path = null;
    @JsonProperty("full_path")
    private String fullPath = null;
    private String kind = null;
    @JsonProperty("parent_id")
    private Long parentId = null;
    @JsonProperty("members_count_with_descendants")
    private Integer members = null;

    public Namespace() {

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
     * Get the namespace name.
     *
     * @return name the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set a namespace name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the namespace path.
     *
     * @return path the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set a namespace path.
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
     * Get the namespace kind.
     *
     * @return kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Set a namespace kind.
     *
     * @param kind the kind to set
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * Get the parent namespace ID.
     *
     * @return parentId the id of the parent
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * Set a parent ID value
     *
     * @param parentId the id to set
     */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * @return members
     */
    public Integer getMembers() {
        return members;
    }

    /**
     * @param members the members to set
     */
    public void setMembers(Integer members) {
        this.members = members;
    }
}
