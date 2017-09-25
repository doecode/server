/*
 */
package gov.osti.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * SOLR facet result section.
 * 
 * @author ensornl
 */
@JsonIgnoreProperties (ignoreUnknown = true)
public class SolrFacet implements Serializable {
    private Integer count;
    private LinkedHashMap<String,Integer> values = new LinkedHashMap<>();
    
    /**
     * Add a key-value pair to the facet values.
     * 
     * @param key the KEY
     * @param value the VALUE
     * @return this Object for chaining
     */
    public SolrFacet add(String key, Integer value) {
        values.put(key, value);
        
        return this;
    }
    
    public LinkedHashMap<String,Integer> getValues() {
        return values;
    }

    /**
     * @return the count
     */
    public Integer getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(Integer count) {
        this.count = count;
    }
}
