/*
 */
package gov.osti.search;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * Custom deserialization of SOLR "dates" facets.
 * 
 * @author ensornl
 */
public class FacetDeserializer extends StdDeserializer<SolrFacet> {
    
    public FacetDeserializer() {
        this(null);
    }

    public FacetDeserializer(Class<?> result) {
        super(result);
    }

    @Override
    public SolrFacet deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);

        JsonNode dates = node.get("dates");
        JsonNode buckets = (null!=dates) ? dates.get("buckets") : null;
        SolrFacet facet = new SolrFacet();

        if (null!=buckets && buckets.isArray()) {
            for ( JsonNode bucket : buckets ) {
                facet.add(bucket.get("val").asText(), bucket.get("count").asInt());
            }
        }

        return facet;
    }
}
