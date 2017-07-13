package gov.osti.entity;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class SearchData implements Serializable {

	private static final long serialVersionUID = 978828434590378134L;
	private static final Logger log = LoggerFactory.getLogger(SearchData.class.getName());
	
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(Include.NON_NULL);
    
    private String allFields = null;
    private String softwareTitle = null;
    private String developersContributors = null;
    private String biblioData = null;
    private String identifiers = null;
    private Date dateEarliest = null;
    private Date dateLatest = null;
    private String availability = null;
    private String researchOrganization = null;
    private String sponsoringOrganization = null;
    private String sort = null;
    
    /**
     * Parses JSON in the request body of the reader into a SearchDaa object.
     * @param reader - A request reader containing JSON in the request body.
     * @return A SearchData object representing the data of the JSON in the request body.
     * @throws IOException on JSON parsing errors (IO errors)
     */
    public static SearchData parseJson(Reader reader) throws IOException {
        return mapper.readValue(reader, SearchData.class);
    }

    public String getAllFields() {
        return allFields;
    }

	public void setAllFields(String allFields) {
		this.allFields = allFields;
	}

	public String getSoftwareTitle() {
		return softwareTitle;
	}

	public void setSoftwareTitle(String softwareTitle) {
		this.softwareTitle = softwareTitle;
	}

	public String getDevelopersContributors() {
		return developersContributors;
	}

	public void setDevelopersContributors(String developersContributors) {
		this.developersContributors = developersContributors;
	}

	public String getBiblioData() {
		return biblioData;
	}

	public void setBiblioData(String biblioData) {
		this.biblioData = biblioData;
	}

	public String getIdentifiers() {
		return identifiers;
	}

	public void setIdentifiers(String identifiers) {
		this.identifiers = identifiers;
	}

	public Date getDateEarliest() {
		return dateEarliest;
	}

	public void setDateEarliest(Date dateEarliest) {
		this.dateEarliest = dateEarliest;
	}

	public Date getDateLatest() {
		return dateLatest;
	}

	public void setDateLatest(Date dateLatest) {
		this.dateLatest = dateLatest;
	}

	public String getAvailability() {
		return availability;
	}

	public void setAvailability(String availability) {
		this.availability = availability;
	}

	public String getResearchOrganization() {
		return researchOrganization;
	}

	public void setResearchOrganization(String researchOrganization) {
		this.researchOrganization = researchOrganization;
	}

	public String getSponsoringOrganization() {
		return sponsoringOrganization;
	}

	public void setSponsoringOrganization(String sponsoringOrganization) {
		this.sponsoringOrganization = sponsoringOrganization;
	}

	public String getSort() {
            return (null==sort) ? "" : sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}
        
    /**
     * Translate this Bean into a SOLR query parameter set.
     * 
     * @return a SOLR query parameter "q" for these attributes; default to "*:*"
     * (everything) if nothing is set
     */
    public String toQ() {
        StringBuilder q = new StringBuilder();
        DateTimeFormatter SOLR_DATE_FORMAT = DateTimeFormatter.ISO_INSTANT;
        
        if (null!=getAllFields()) {
            if (q.length()>0) q.append(" ");
            q.append("_text_:(").append(getAllFields()).append(")");
        }
        if (null!=getAvailability()) {
            if (q.length()>0) q.append(" ");
            q.append("accessibility:").append(getAvailability());
        }
        if (null!=getBiblioData()) {
            if (q.length()>0) q.append(" ");
            q.append("_text_:(").append(getBiblioData()).append(")");
        }
        if (null!=getDevelopersContributors()) {
            if (q.length()>0) q.append(" ");
            q.append("_names:(").append(getDevelopersContributors()).append(")");
        }
        if (null!=getIdentifiers()) {
            if (q.length()>0) q.append(" ");
            q.append("_id_numbers:(").append(getIdentifiers()).append(")");
        }
        if (null!=getResearchOrganization()) {
            if (q.length()>0) q.append(" ");
            q.append("researchOrganization.name:(").append(getResearchOrganization()).append(")");
        }
        if (null!=getSponsoringOrganization()) {
            if (q.length()>0) q.append(" ");
            q.append("sponsoringOrganization.name:(").append(getSponsoringOrganization()).append(")");
        }
        if (null!=getSoftwareTitle()) {
            if (q.length()>0) q.append(" ");
            q.append("softwareTitle:(").append(getSoftwareTitle()).append(")");
        }
        if (null!=getDateEarliest()) {
            if (q.length()>0) q.append(" ");
            q.append("releaseDate:[")
                    .append(SOLR_DATE_FORMAT.format(
                            getDateEarliest()
                                    .toInstant()
                                    .atOffset(ZoneOffset.UTC)
                                    .withHour(0)
                                    .withMinute(0)
                                    .withSecond(0)))
                    .append(" TO *]");
        }
        if (null!=getDateLatest()) {
            if (q.length()>0) q.append(" ");
            q.append("releaseDate:[* TO ")
                    .append(SOLR_DATE_FORMAT.format(
                    getDateLatest()
                        .toInstant()
                        .atOffset(ZoneOffset.UTC)
                        .withHour(23)
                        .withMinute(59)
                        .withSecond(59)))
                    .append("]");
        }
        
        return (0==q.length()) ? "*:*" : q.toString();
    }
    
		
    
}
