/*
 */
package gov.osti.indexer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.osti.entity.DOECodeMetadata;
import java.io.IOException;

/**
 * Custom serializer for Accessibility Enumeration.
 * 
 * @author ensornl
 */
public class AccessibilitySerializer extends StdSerializer<DOECodeMetadata.Accessibility> {
    
    public AccessibilitySerializer() {
        this(null);
    }
    
    public AccessibilitySerializer(Class<DOECodeMetadata.Accessibility> type) {
        super(type);
    }

    @Override
    public void serialize(DOECodeMetadata.Accessibility t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("label", t.label());
        jg.writeStringField("value", t.name());
        jg.writeEndObject();
    }
    
}
