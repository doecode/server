/*
 */
package gov.osti.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * a SOLR Search Result class.
 * 
 * @author ensornl
 */
@JsonIgnoreProperties (ignoreUnknown = true)
public class SolrResponse implements Serializable {
    @JsonProperty (value = "docs")
    private SolrDocument[] documents;
    @JsonProperty (value = "numFound")
    private int numFound;
    @JsonProperty (value = "start")
    private int start;

    /**
     * Get any SearchDocument results found.
     * 
     * @return the documents found
     */
    public SolrDocument[] getDocuments() {
        return documents;
    }

    /**
     * Set some SearchDocument Objects.
     * 
     * @param documents the documents to set
     */
    public void setDocuments(SolrDocument[] documents) {
        this.documents = documents;
    }

    /**
     * The number of records found by this search.
     * 
     * @return the numFound the number of records found
     */
    public int getNumFound() {
        return numFound;
    }

    /**
     * Set the number of records found.
     * 
     * @param numFound the numFound to set
     */
    public void setNumFound(int numFound) {
        this.numFound = numFound;
    }

    /**
     * Starting record number.
     * 
     * @return the start the starting row number from 0.
     */
    public int getStart() {
        return start;
    }

    /**
     * Set the starting row number.
     * 
     * @param start the start to set
     */
    public void setStart(int start) {
        this.start = start;
    }
    
    /**
     * Determine whether or not there's any search results.
     * @return true if there are results, false if not
     */
    public boolean isEmpty() {
        return (null==documents || documents.length==0);
    }
}
