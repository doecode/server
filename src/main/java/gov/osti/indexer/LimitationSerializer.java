/*
 */
package gov.osti.indexer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.osti.entity.DOECodeMetadata.Limitation;
import java.io.IOException;

/**
 * Custom serializer for Limitation Enumeration.
 * 
 * @author samarj
 */
public class LimitationSerializer extends StdSerializer<Limitation> {
    
    public LimitationSerializer() {
        this(null);
    }
    
    public LimitationSerializer(Class<Limitation> type) {
        super(type);
    }

    @Override
    public void serialize(Limitation t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("label", t.label());
        jg.writeStringField("value", t.name());
        jg.writeEndObject();
    }
    
}
