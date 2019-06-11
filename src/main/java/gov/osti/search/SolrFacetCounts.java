package gov.osti.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SOLR facet counts result section.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolrFacetCounts implements Serializable {

    //private final Map<String,Integer> facetQueries = new LinkedHashMap<>(); -- TODO
    private final Map<String, Map<String, Integer>> facetFields = new LinkedHashMap<>();
    //private final Map<String,Integer> facetRanges = new LinkedHashMap<>(); -- TODO
    //private final Map<String,Integer> facetIntervals = new LinkedHashMap<>(); -- TODO
    //private final Map<String,Integer> facetHeatmaps = new LinkedHashMap<>(); -- TODO

    /**
     * Add a key-value pair to the facet FIELD values.
     *
     * @param field
     * @return this Object for chaining
     */
    public SolrFacetCounts addField(String field) {
        facetFields.put(field, new LinkedHashMap<>());

        return this;
    }

    public SolrFacetCounts addFieldCounts(String field, Map<String, Integer> fieldCounts) {
        facetFields.put(field, fieldCounts);

        return this;
    }

    public Map<String, Map<String, Integer>> getFields() {
        return facetFields;
    }

    /**
     * @return the count
     */
    public Integer getFieldsCount() {
        return facetFields.size();
    }
}
