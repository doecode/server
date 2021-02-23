package gov.osti.entity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.io.Serializable;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * POJO for interpreting AWARD information from related identifier values of that 
 * RELATED_IDENTIFIER_TYPE.  The VALUE will be an array of these in JSON.
 */
@Embeddable
@JsonIgnoreProperties ( ignoreUnknown = true )
public class Award {
    // logger
    private static final Logger log = LoggerFactory.getLogger(Award.class);
    // data attributes for an Award
    private String funderName;
    private String awardDoi;

    public Award() {

    }

    public Award(String doi) {
        this.awardDoi = doi;
    }

    public Award(String doi, String funder) {
        this.awardDoi = doi;
        this.funderName = funder;
    }

    public Award(Award a) {
        this(a.getAwardDoi(), a.getFunderName());
    }

    // Jackson object mapper
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(Include.NON_NULL);


    @Column (name = "AWARD_DOI")
    public String getAwardDoi() {
        return awardDoi;
    }

    public void setAwardDoi(String doi) {
        this.awardDoi = doi;
    }

    @Column (name = "FUNDER_NAME")
    public String getFunderName() {
        return funderName;
    }

    public void setFunderName(String funderName) {
        this.funderName = StringUtils.trimToEmpty(funderName);
    }

    /**
     * Considered "empty" if the fields are all empty.
     * 
     * @return true if empty, false if not
     */
    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isEmpty(getAwardDoi()) && 
            StringUtils.isEmpty(getFunderName());
    }

    /**
     * Instantiates an Award from a String of JSON.  If unable to, returns an empty Award.
     * 
     * @param in the JSON to process
     * @return an Award from that JSON
     */
    public static Award fromJson(String in) {
        try {
            return mapper.readValue(in, Award.class);
        } catch ( IOException e ) {
            log.warn("Unable to convert {} to Award: {}", in, e.getMessage());
            return new Award();
        }
    }

    /**
     * Convert this object to JSON String if possible. Deliberately
     * suppress IO errors and sends back empty String if fails.
     * 
     * @return JSON of this entity
     */
    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch ( JsonProcessingException e ) {
            log.warn("JSON conversion error in award {}: {}", this.toString(), e.getMessage());
            return "";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Award ) {
            return ((Award)o).getAwardDoi().equalsIgnoreCase(getAwardDoi()) &&
                   ((Award)o).getFunderName().equals(getFunderName());
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.awardDoi);
        hash = 83 * hash + Objects.hashCode(this.funderName);
        return hash;
    }
}
