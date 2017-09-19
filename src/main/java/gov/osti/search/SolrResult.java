/*
 */
package gov.osti.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for SOLR searching results.
 * 
 * @author ensornl
 */
@JsonIgnoreProperties (ignoreUnknown = true)
public class SolrResult {
    // only define the search response
    @JsonProperty (value = "response")
    private SolrResponse response;
    
    /**
     * Get the SearchResponse from this search.
     * 
     * @return the SearchResponse found
     */
    public SolrResponse getSearchResponse() {
        return response;
    }
    
    /**
     * Set the SearchResponse.
     * @param r the SearchResponse to set
     */
    public void setSearchResponse(SolrResponse r) {
        response = r;
    }
}
