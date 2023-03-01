/*
 */
package gov.osti.indexer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.osti.entity.ContributingOrganization.Type;
import java.io.IOException;

/**
 * Customize JSON output for ContributingOrganization.Type enumeration.
 */
public class ContributingOrgTypeSerializer extends StdSerializer<Type> {
    
    public ContributingOrgTypeSerializer() {
        this(null);
    }
    
    public ContributingOrgTypeSerializer(Class<Type> type) {
        super(type);
    }

    @Override
    public void serialize(Type t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("label", t.label());
        jg.writeStringField("value", t.name());
        jg.writeEndObject();
    }
    
}
