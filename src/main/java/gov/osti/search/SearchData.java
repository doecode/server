package gov.osti.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

@JsonIgnoreProperties ( ignoreUnknown = true )
public class SearchData implements Serializable {

	private static final long serialVersionUID = 978828434590378134L;
	private static final Logger log = LoggerFactory.getLogger(SearchData.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(Include.NON_NULL);

    // set of special characters to be escaped before sending to SOLR
    protected static Pattern TEXT_REGEX_CHARACTERS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
    // set of special characters applying to TOKENS in SOLR
    protected static Pattern TOKEN_REGEX_CHARACTERS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|\"]");

    private String allFields = null;
    private String softwareTitle = null;
    private String developersContributors = null;
    private String biblioData = null;
    private String identifiers = null;
    private Date dateEarliest = null;
    private Date dateLatest = null;
    private String[] accessibility = null;
    private String[] licenses;
    private String[] researchOrganization = null;
    private String[] sponsoringOrganization = null;
    private String orcid;
    private String sort = null;
    private Integer rows;
    private Integer start;

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

	public String[] getAccessibility() {
		return accessibility;
	}

	public void setAccessibility(String[] accessibility) {
		this.accessibility = accessibility;
	}

	public String[] getResearchOrganization() {
		return researchOrganization;
	}

	public void setResearchOrganization(String[] researchOrganization) {
		this.researchOrganization = researchOrganization;
	}

	public String[] getSponsoringOrganization() {
		return sponsoringOrganization;
	}

	public void setSponsoringOrganization(String[] sponsoringOrganization) {
		this.sponsoringOrganization = sponsoringOrganization;
	}

  public void setOrcid(String orcid) {
      this.orcid = orcid;
  }

  public String getOrcid() {
      return this.orcid;
  }

	public String getSort() {
            return (null==sort) ? "" : sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

        /**
         * Escape SOLR special characters in search expressions.
         *
         * @param in the input String
         * @return the String with any special characters escaped
         */
        protected static String escape(String in) {
            return (null==in) ?
                    "" :
                    TEXT_REGEX_CHARACTERS.matcher(in).replaceAll("\\\\$0");
        }
        
        /**
         * Escape SOLR special characters in TOKEN expressions.
         * 
         * @param in the incoming search expression
         * @return a String with special characters escaped
         */
        protected static String escapeToken(String in) {
            return (null==in) ? 
                    "" :
                    TOKEN_REGEX_CHARACTERS.matcher(in).replaceAll("\\\\$0");
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

        if (!StringUtils.isEmpty(getAllFields())) {
            if (q.length()>0) q.append(" ");
            q.append("_text_:(").append(escape(getAllFields())).append(")");
        }
        if (null!=getAccessibility()) {
            StringBuilder codes = new StringBuilder();
            for ( String code : getAccessibility()) {
                if (codes.length()>0) codes.append(" OR ");
                codes.append("accessibility:").append(code);
            }
            if ( codes.length()>0 ) {
                if (q.length()>0) q.append(" ");
                q.append("(").append(codes.toString()).append(")");
            }
        }
        if (null!=getLicenses()) {
            StringBuilder values = new StringBuilder();
            for ( String license : getLicenses() ) {
                if (values.length()>0) values.append(" OR ");
                values.append("licenses:\"").append(escapeToken(license)).append("\"");
            }
            if (values.length()>0) {
                if (q.length()>0) q.append(" ");
                q.append("(").append(values.toString()).append(")");
            }
        }
        if (!StringUtils.isEmpty(getBiblioData())) {
            if (q.length()>0) q.append(" ");
            q.append("_text_:(").append(escape(getBiblioData())).append(")");
        }
        if (!StringUtils.isEmpty(getOrcid())) {
            if (q.length()>0) q.append(" ");
            q.append("_orcids:(").append(escape(getOrcid())).append(")");
        }
        if (!StringUtils.isEmpty(getDevelopersContributors())) {
            if (q.length()>0) q.append(" ");
            q.append("_names:(").append(escape(getDevelopersContributors())).append(")");
        }
        if (!StringUtils.isEmpty(getIdentifiers())) {
            if (q.length()>0) q.append(" ");
            q.append("_id_numbers:(").append(escape(getIdentifiers())).append(")");
        }
        if (null!=getResearchOrganization()) {
                        StringBuilder values = new StringBuilder();
                        for ( String org : getResearchOrganization() ) {
                                        if (values.length()>0) values.append(" OR ");
                                        values.append("researchOrganizations.organizationName:\"").append(escape(org)).append("\"");
                        }
                        if (values.length()>0) {
                                        if (q.length()>0) q.append(" ");
                                        q.append("(").append(values.toString()).append(")");
                        }
        }
        if (null!=getSponsoringOrganization()) {
                        StringBuilder values = new StringBuilder();
                        for ( String org : getSponsoringOrganization() ) {
                                        if (values.length()>0) values.append(" OR ");
                                        values.append("sponsoringOrganizations.organizationName:\"").append(escape(org)).append("\"");
                        }
                        if (values.length()>0) {
                                        if (q.length()>0) q.append(" ");
                                        q.append("(").append(values.toString()).append(")");
                        }
        }
        if (!StringUtils.isEmpty(getSoftwareTitle())) {
            if (q.length()>0) q.append(" ");
            q.append("softwareTitle:(").append(escape(getSoftwareTitle())).append(")");
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

    /**
     * The number of rows to return in a single page.
     * @return the rows
     */
    public Integer getRows() {
        return rows;
    }

    /**
     * Set the number of rows desired.
     * @param rows the rows to set
     */
    public void setRows(Integer rows) {
        this.rows = rows;
    }

    /**
     * The starting row number (0-based).
     * @return the start row number
     */
    public Integer getStart() {
        return start;
    }

    /**
     * Set the starting row number of results (0-based)
     * @param start the start to set
     */
    public void setStart(Integer start) {
        this.start = start;
    }

    /**
     * @return the licenses
     */
    public String[] getLicenses() {
        return licenses;
    }

    /**
     * @param licenses the licenses to set
     */
    public void setLicenses(String[] licenses) {
        this.licenses = licenses;
    }

}
