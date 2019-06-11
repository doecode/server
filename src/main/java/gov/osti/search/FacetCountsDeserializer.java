package gov.osti.search;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.osti.services.SearchService;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom deserialization of SOLR facets counts.
 */
public class FacetCountsDeserializer extends StdDeserializer<SolrFacetCounts> {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    public FacetCountsDeserializer() {
        this(null);
    }

    public FacetCountsDeserializer(Class<?> result) {
        super(result);
    }

    @Override
    public SolrFacetCounts deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);

        SolrFacetCounts facetCounts = new SolrFacetCounts();
        // queries -- TODO

        // fields
        JsonNode fields = node.get("facet_fields");

        if (null != fields && !fields.isNull()) {
            Iterator<Entry<String, JsonNode>> nodes;
            nodes = fields.fields();

            while (nodes.hasNext()) {
                Map<String, Integer> fieldCounts = new LinkedHashMap<>();

                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes.next();
                String fieldName = entry.getKey();
                JsonNode fieldCountArray = entry.getValue();

                // get counts for all facet fields
                if (null != fieldCountArray && fieldCountArray.isArray()) {
                    int arraySize = fieldCountArray.size();
                    for (int i = 0; i < arraySize; i = i + 2) {
                        // this array is always key value pairs, string/int repeat
                        String key = fieldCountArray.get(i).asText();
                        int value = fieldCountArray.get(i + 1).asInt();

                        if (value > 0)
                            fieldCounts.put(key, value);
                    }
                }

                // store counts to object
                facetCounts.addField(fieldName);
                facetCounts.addFieldCounts(fieldName, fieldCounts);
            }
        }

        // ranges -- TODO
        // intervals -- TODO
        // heatmaps -- TODO
        return facetCounts;
    }
}
