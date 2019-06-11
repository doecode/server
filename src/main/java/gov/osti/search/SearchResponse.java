/*
 */
package gov.osti.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import gov.osti.entity.DOECodeMetadata;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DOE CODE Search Response Object.
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
    private Map<String,Integer> facets = new LinkedHashMap<>();
    private final Map<String, Map<String, Map<String, Integer>>> facetCounts = new LinkedHashMap<>();

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

    /**
     * @return the facets
     */
    public Map<String,Integer> getFacets() {
        return facets;
    }

    /**
     * @param facets the facets to set
     */
    public void setFacets(Map<String,Integer> facets) {
        this.facets = facets;
    }

    /**
     * @return the facet counts
     */
    public Map<String, Map<String, Map<String, Integer>>> getFacetCounts() {
        return facetCounts;
    }

    /**
     * @param facetFieldCounts the facet counts to set
     */
    public void setFacetFieldCounts(Map<String, Map<String, Integer>> facetFieldCounts) {
        this.facetCounts.put("facet_fields", facetFieldCounts);
    }
}
