/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.osti.listeners.DoeServletContextListener;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single Entity containing a sequence of sorts for constructing DOI values.
 *
 * Based on defined PREFIX value, "dc.yyyyMMdd.n" pattern.  n resets to 1
 * each day.
 *
 * @author ensornl
 */
@Table (name = "DOI_RESERVATION")
@Entity
@JsonIgnoreProperties (ignoreUnknown = true)
public class DoiReservation implements Serializable {
    // logger
    private static Logger log = LoggerFactory.getLogger(DoiReservation.class);
    // get the defined DATACITE DOI PREFIX value
    private static String DATACITE_PREFIX = DoeServletContextListener.getConfigurationProperty("datacite.prefix");
    // static TYPE for this reservation value
    public static final String TYPE = "DOI";
    // attributes
    @Id
    @Column (length = 12, name = "TYPE")
    private String type = TYPE;
    @Column (length = 12, name = "DATE_PATTERN")
    private String datePattern;
    @Column (name = "INDEX")
    private Integer index;

    /**
     * @return the type
     */
    @JsonIgnore
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    protected void setType(String type) {
        this.type = type;
    }

    /**
     * @return the datePattern
     */
    @JsonIgnore
    public String getDatePattern() {
        return datePattern;
    }

    /**
     * @param datePattern the date to set
     */
    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    /**
     * @return the index
     */
    @JsonIgnore
    public Integer getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(Integer index) {
        this.index = index;
    }

    /**
     * Obtain a new DOI Reservation value.
     * If not set presently, take TODAY as a base date pattern, with 1 as the
     * index.  If set, and the date is NOT today's date, so the same. Otherwise,
     * increment the index value.
     *
     * Entity attributes are modified by this call, and should be persisted
     * outside this Bean context.
     */
    public synchronized void reserve() {
        String now = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());

        if (!StringUtils.startsWith(getDatePattern(), now)) {
            setDatePattern(now);
            setIndex(1);
        } else {
            setIndex(++index);
        }
    }

    /**
     * Obtain the RESERVED DOI value.  Call reserve() first.
     * @return a DOI reservation value
     */
    @JsonProperty (value = "doi")
    public String getReservation() {
        return DATACITE_PREFIX + "/dc." + getDatePattern() + "." + getIndex();
    }
}
