package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
@JsonIgnoreProperties ( ignoreUnknown = true )
@Table(name = "official_use_only")

public class OfficialUseOnly implements Serializable {
    public enum Protection {
        CRADA("CRADA"),
        EPACT("EPACT"),
        Other("Other");
        
        private final String label;
        
        private Protection(String label) {
            this.label = label;
        }
        
        public String label() {
            return this.label;
        }
    }

    private String exemptionNumber;
    private Protection protection;
    private String protectionOther;
    private Date ouoReleaseDate;
    private String programOffice;
    private String protectionReason;

    public OfficialUseOnly() {
    }


	@Column(name = "exemption_number", table = "official_use_only")
	public String getExemptionNumber() {
		return exemptionNumber;
	}

	public void setExemptionNumber(String value) {
		this.exemptionNumber = value;
	}


    @Enumerated (EnumType.STRING)
    @Column (name = "protection", table = "official_use_only")
    public Protection getProtection() {
        return protection;
    }

    public void setProtection(Protection value) {
        this.protection = value;
    }


	@Column(name = "protection_other", table = "official_use_only")
    public String getProtectionOther() {
        return protectionOther;
    }

    public void setProtectionOther(String value) {
        this.protectionOther = value;
    }


	@Column(name = "ouo_release_date", table = "official_use_only")
    @Temporal(javax.persistence.TemporalType.DATE)
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	public Date getOuoReleaseDate() {
		return ouoReleaseDate;
	}

	public void setOuoReleaseDate(Date value) {
		this.ouoReleaseDate = value;
	}


	@Column(name = "program_office", table = "official_use_only")
	public String getProgramOffice() {
		return programOffice;
	}

	public void setProgramOffice(String value) {
		this.programOffice = value;
	}


	@Column(name = "protection_reason", table = "official_use_only")
    public String getProtectionReason() {
        return protectionReason;
    }

    public void setProtectionReason(String value) {
        this.protectionReason = value;
    }
}
