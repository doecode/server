package gov.osti.connectors.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Mimic the structure of the GitLab Commit API body, to simplify file commits.
 *
 * @author sowerst
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Commit {

    /** public attributes * */
    private String branch;
    @JsonProperty("commit_message")
    private String commitMessage = null;
    private final List<Action> actions = new ArrayList<>();

    public Commit() {

    }

    /**
     * Get the branch value.
     *
     * @return branch the Branch to commit to
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Set an branch value
     *
     * @param branch the branch to commit to
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * @return the commitMessage
     */
    public String getCommitMessage() {
        return commitMessage;
    }

    /**
     * @param commitMessage the commitMessage to set
     */
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    /**
     * Get the actions list.
     *
     * @return actions the actions
     */
    public List<Action> getActions() {
        return actions;
    }

    /**
     * Clear actions.
     */
    public void clearAction() {
        this.actions.clear();
    }

    /**
     * Add action.
     *
     * @param action
     */
    public void addAction(Action action) {
        this.actions.add(action);
    }

    /**
     * Add base64 content action, by values.
     *
     * @param action
     * @param filePath
     * @param base64content
     */
    public void addBase64ActionByValues(String action, String filePath, String base64content) {
        Action newAction = new Action();
        newAction.setAction(action);
        newAction.setFilePath(filePath);
        newAction.setContent(base64content);
        addAction(newAction);
    }
}
