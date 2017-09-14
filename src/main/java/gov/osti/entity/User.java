package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="users")
@NamedQueries ({
    @NamedQuery (name = "User.findAllUsers", query = "SELECT u FROM User u ORDER BY u.lastName"),
    @NamedQuery (name = "User.findUser", query = "SELECT u FROM User u WHERE u.email=:email")
})
public class User implements Serializable {
    
    // number of days before password is considered to be expired
    public static final int PASSWORD_DATE_EXPIRATION_IN_DAYS = 180;
	
    public User() {

    }

    public User(String email, String password, String apiKey, String confirmationCode) {
            this.password = password;
            this.apiKey = apiKey;
            this.email = email;
            this.confirmationCode = confirmationCode;
            // new users are blank slate
            this.failedCount = 0;
            this.active = false;
            this.verified = false;
    }
    
    // email address is primary key for Users
    @Id
    private String email = null;
    private String password = null;
    private String apiKey = null;
    private String confirmationCode = null;
    private String siteId = null;

    // whether or not the account has been VERIFIED/CONFIRMED via email
    private Boolean verified;
    // if the account has been administratively DISABLED or not
    private Boolean active;

    private String firstName;
    private String lastName;

    // for CONTRACTOR entries; required and validated
    private String contractNumber;

    // administrative dates
    @Basic(optional = false)
    @Column(name = "date_record_added", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "EST")
    private Date dateRecordAdded;
    @Basic(optional = false)
    @Column(name = "date_record_updated", insertable = true, updatable = true)
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "EST")
    private Date dateRecordUpdated;
    @Column (name = "date_password_changed")
    @Temporal (TemporalType.TIMESTAMP)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "EST")
    private Date datePasswordChanged;

    // count of failed logins
    private Integer failedCount;

    @ElementCollection
    private Set<String> roles = null;

    @ElementCollection 
    private Set<String> pendingRoles = null;

    /**
     * Do NOT output this on JSON Object requests.
     * @return the Password
     */
    @JsonIgnore
    public String getPassword() {
            return password;
    }

    public void setPassword(String password) {
            this.password = password;
    }

    public String getApiKey() {
            return apiKey;
    }

    public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
    }


    public String getConfirmationCode() {
            return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
            this.confirmationCode = confirmationCode;
    }

    public String getEmail() {
            return email;
    }

    public void setEmail(String email) {
            this.email = email;
    }

    public String getSiteId() {
            return siteId;
    }

    public void setSiteId(String siteId) {
            this.siteId = siteId;
    }

    public Boolean isVerified() {
            return verified;
    }
    
    /**
     * Extra get method for Beans.
     * @return the state of the VERIFIED; may be null
     */
    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
            this.verified = verified;
    }

    public Boolean isActive() {
        return active;
    }
    
    /**
     * Extra get method for Beans.
     * 
     * @return the state of ACTIVE; may be null
     */
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * Get the currently approved ROLES
     * @return a Set of ROLES for this User
     */
    public Set<String> getRoles() {
            return roles;
    }

    /**
     * Set the approved ROLES for this User
     * @param roles the Set of approved ROLES
     */
    public void setRoles(Set<String> roles) {
            this.roles = roles;
    }

    /**
     * Retrieve a Set of unapproved/pending ROLES.
     * 
     * @return a Set of pending ROLES
     */
    public Set<String> getPendingRoles() {
            return pendingRoles;
    }

    /**
     * Set the PENDING ROLES
     * @param pendingRoles 
     */
    public void setPendingRoles(Set<String> pendingRoles) {
            this.pendingRoles = pendingRoles;
    }

    /**
     * Get the FIRST NAME
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Set the FIRST NAME
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Get the LAST NAME
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Set the LAST NAME
     * @param lastName the lastName to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Get the CONTRACT NUMBER for this User.
     * 
     * @return the contractNumber a CONTRACT NUMBER if any
     */
    public String getContractNumber() {
        return contractNumber;
    }

    /**
     * Set the CONTRACT NUMBER for this User
     * @param contractNumber the contractNumber to set
     */
    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    /**
     * @return the dateRecordAdded
     */
    public Date getDateRecordAdded() {
        return dateRecordAdded;
    }

    /**
     * @param dateRecordAdded the dateRecordAdded to set
     */
    public void setDateRecordAdded(Date dateRecordAdded) {
        this.dateRecordAdded = dateRecordAdded;
    }
    
    public void setDateRecordAdded () {
        setDateRecordAdded(new Date());
    }

    /**
     * @return the dateRecordUpdated
     */
    public Date getDateRecordUpdated() {
        return dateRecordUpdated;
    }

    /**
     * @param dateRecordUpdated the dateRecordUpdated to set
     */
    public void setDateRecordUpdated(Date dateRecordUpdated) {
        this.dateRecordUpdated = dateRecordUpdated;
    }
    
    public void setDateRecordUpdated() {
        setDateRecordUpdated(new Date());
    }
    
    /**
     * Method called when a record is first created.  Sets dates added and
     * updated.
     */
    @PrePersist
    void createdAt() {
        setDateRecordAdded();
        setDateRecordUpdated();
    }
    
    /**
     * Method called when the record is updated.
     */
    @PreUpdate
    void updatedAt() {
        setDateRecordUpdated();
    }
    
    /**
     * Set the Date the password was last changed
     * @param date the date to set
     */
    public void setDatePasswordChanged(Date date) {
        this.datePasswordChanged = date;
    }
    
    /**
     * Sets the date password was last changed to now.
     */
    public void setDatePasswordChanged() {
        setDatePasswordChanged(new Date());
    }
    
    /**
     * Get the date the password was last changed (might be null)
     * @return the date the password was last changed
     */
    public Date getDatePasswordChanged() {
        return this.datePasswordChanged;
    }

    /**
     * Determine whether or not the password has expired (reached maximum age 
     * since changed.)  If this date is NOT set, assume it is EXPIRED.
     * 
     * @return true if expired, false if not
     */
    public boolean isPasswordExpired() {
        // no date, assume expired
        if (null==getDatePasswordChanged())
            return true;
        // figure out if this Date is expired
        Calendar expired = Calendar.getInstance();
        expired.add(Calendar.DAY_OF_YEAR, -1 * PASSWORD_DATE_EXPIRATION_IN_DAYS);
        
        return getDatePasswordChanged().before(expired.getTime());
    }
    
    /**
     * Get the number of failed logins on this account.
     * 
     * @return the number of times password failed
     */
    public Integer getFailedCount() {
        return this.failedCount;
    }
    
    /**
     * Set the number of failed login attempts on this account.
     * 
     * @param count the count to set
     */
    public void setFailedCount(Integer count) {
        this.failedCount = count;
    }
    
    /**
     * Determine whether or not this User has the indicated Role.
     * 
     * Null-safe method of checking for ROLE presence.
     * 
     * @param role the ROLE CODE to check
     * @return true if the ROLE is present, false if not
     */
    public boolean hasRole(String role) {
        return (null==roles) ? 
                false :
                roles.contains(role);
    }
    
    public boolean hasRoles() {
        return (null!=roles);
    }
}
