/*
 */
package gov.osti.connectors.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structure to hold GitLab Action information when committing files via the API.
 *
 * @author sowerst
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action {

    /** public attributes * */
    private String action;
    @JsonProperty("file_path")
    private String filePath = null;
    private String encoding = "base64";
    private String content = null;

    public Action() {

    }

    /**
     * Get the action value.
     *
     * @return action the Action
     */
    public String getAction() {
        return action;
    }

    /**
     * Set an action value
     *
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the filePath
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @param filePath the filePath to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Get the content encoding.
     *
     * @return encoding the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Set a content encoding.
     *
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Get the content.
     *
     * @return content the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Set content.
     *
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }
}
