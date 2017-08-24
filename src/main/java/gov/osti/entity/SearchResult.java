/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for SOLR searching results.
 * 
 * @author ensornl
 */
@JsonIgnoreProperties (ignoreUnknown = true)
public class SearchResult {
    // only define the search response
    @JsonProperty (value = "response")
    private SearchResponse response;
    
    /**
     * Get the SearchResponse from this search.
     * 
     * @return the SearchResponse found
     */
    public SearchResponse getSearchResponse() {
        return response;
    }
    
    /**
     * Set the SearchResponse.
     * @param r the SearchResponse to set
     */
    public void setSearchResponse(SearchResponse r) {
        response = r;
    }
}
