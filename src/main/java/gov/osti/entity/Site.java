package gov.osti.entity;

@Entity
@Table(name="sites")
public class Site {

	
	private String lab;
	private List<String> emailDomains;
	private String siteCode;
    
	public Site() {
		
	}
	
	@Id
	public String getLab() {
		return lab;
	}
	public void setLab(String lab) {
		this.lab = lab;
	}
	
    @ElementCollection
    @CollectionTable(
            name = "EMAIL_DOMAINS",
            joinColumns=@JoinColumn(name="CODE_ID")
    )
    @Column (name="EMAIL_DOMAIN")
	public List<String> getEmailDomains() {
		return emailDomains;
	}
	public void setEmailDomains(List<String> emailDomains) {
		this.emailDomains = emailDomains;
	}
	
	@Column (name="SITE_CODE")
	public String getSiteCode() {
		return siteCode;
	}
	public void setSiteCode(String siteCode) {
		this.siteCode = siteCode;
	}
	
	
}
