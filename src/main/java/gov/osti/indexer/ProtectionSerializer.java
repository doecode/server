/*
 */
package gov.osti.indexer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.osti.entity.OfficialUseOnly;
import java.io.IOException;

/**
 * Custom serializer for Protection Enumeration.
 * 
 * @author sowerst
 */
public class ProtectionSerializer extends StdSerializer<OfficialUseOnly.Protection> {
    
    public ProtectionSerializer() {
        this(null);
    }
    
    public ProtectionSerializer(Class<OfficialUseOnly.Protection> type) {
        super(type);
    }

    @Override
    public void serialize(OfficialUseOnly.Protection t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("label", t.label());
        jg.writeStringField("value", t.name());
        jg.writeEndObject();
    }
    
}
