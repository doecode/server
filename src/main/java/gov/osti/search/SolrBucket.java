/*
 */
package gov.osti.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 *
 * @author ensornl
 */
@JsonIgnoreProperties (ignoreUnknown = true)
public class SolrBucket {
    private static class ValueMap {
        private String val;
        private Integer count;

        /**
         * @return the val
         */
        public String getVal() {
            return val;
        }

        /**
         * @param val the val to set
         */
        public void setVal(String val) {
            this.val = val;
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
    
    @JsonProperty (value = "buckets")
    private ValueMap[] counts;
    
    public void setCount(ValueMap[] map) {
        counts = map;
    }
    
    public ValueMap[] getCount() {
        return counts;
    }
    
    @JsonIgnore
    public Map<String,Integer> getCounts() {
        Map<String,Integer> map = new LinkedHashMap<>();
        
        if (null!=counts) {
            for ( ValueMap value : counts ) {
                map.put(value.getVal(), value.getCount());
            }
        }
        
        return map;
    }
}
