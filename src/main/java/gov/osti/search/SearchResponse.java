/*
 */
package gov.osti.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import gov.osti.entity.DOECodeMetadata;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DOECode Search Response Object.
 * 
 * @author ensornl
 */
@XmlRootElement (name="query")
@JsonIgnoreProperties (ignoreUnknown = true)
public class SearchResponse {
    @JacksonXmlProperty (isAttribute = true)
    private Integer numFound;
    @JacksonXmlProperty (isAttribute = true)
    private Integer start;
    @JacksonXmlElementWrapper (localName = "docs")
    @JacksonXmlProperty (localName = "doc")
    private List<DOECodeMetadata> docs = new ArrayList<>();

    /**
     * @return the numFound
     */
    public Integer getNumFound() {
        return numFound;
    }

    /**
     * @param numFound the numFound to set
     */
    public void setNumFound(Integer numFound) {
        this.numFound = numFound;
    }

    /**
     * @return the start
     */
    public Integer getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(Integer start) {
        this.start = start;
    }
    
    public List<DOECodeMetadata> add(DOECodeMetadata m) {
        docs.add(m);
        return docs;
    }

    /**
     * @return the docs
     */
    public List<DOECodeMetadata> getDocs() {
        return docs;
    }

    /**
     * @param docs the docs to set
     */
    public void setDocs(List<DOECodeMetadata> docs) {
        this.docs = docs;
    }
}
