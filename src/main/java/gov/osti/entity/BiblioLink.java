/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Link for Biblio, for SEO usage.
 * 
 * @author sowerst
 */
@JsonIgnoreProperties ( ignoreUnknown = true )
public class BiblioLink implements Serializable {
    private static final long serialVersionUID = 1L;

    @JacksonXmlProperty(isAttribute = true)
    private String rel;
    @JacksonXmlProperty(isAttribute = true)
    private String href;

    public BiblioLink() {

    }

    public BiblioLink(String rel, String href) {
        this.setRel(rel);
        this.setHref(href);
    }

    /**
     * @return the rel
     */
    public String getRel() {
        return rel;
    }

    /**
     * @param rel the rel to set
     */
    public void setRel(String rel) {
        this.rel = rel;
    }

    /**
     * @return the href
     */
    public String getHref() {
        return href;
    }

    /**
     * @param href the value to set
     */
    public void setHref(String href) {
        this.href = href;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BiblioLink ) {
            return (((BiblioLink)o).getRel().equals(getRel()) && ((BiblioLink)o).getHref().equals(getHref()));
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.rel);
        hash = 83 * hash + Objects.hashCode(this.href);
        return hash;
    }

}
