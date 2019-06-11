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
    @JsonProperty (value = "facets")
    private SolrFacet facet;
    @JsonProperty(value = "facet_counts")
    private SolrFacetCounts facetCounts;

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

    public void setSolrFacetCounts(SolrFacetCounts f) {
        facetCounts = f;
    }

    public SolrFacetCounts getSolrFacetCounts() {
        return facetCounts;
    }

    public void setSolrFacet(SolrFacet f) {
        facet = f;
    }

    public SolrFacet getSolrFacet() {
        return facet;
    }
}
