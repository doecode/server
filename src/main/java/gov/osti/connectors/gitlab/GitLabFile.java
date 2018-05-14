package gov.osti.connectors.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents the file data returned in a GitLab Repositories Tree request.
 *
 * @author sowerst
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabFile {

    /** public attributes * */
    private String id;
    private String name = null;
    private String type = null;
    private String path = null;
    private String mode = null;

    public GitLabFile() {

    }

    /**
     * Get the id value.
     *
     * @return id the Action
     */
    public String getId() {
        return id;
    }

    /**
     * Set an id value
     *
     * @param id the action to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the file name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set a file name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the file type.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Set a file type.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(String mode) {
        this.mode = mode;
    }
}
