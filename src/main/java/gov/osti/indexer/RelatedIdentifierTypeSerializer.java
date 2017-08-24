/*
 */
package gov.osti.indexer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.osti.entity.RelatedIdentifier;
import java.io.IOException;

/**
 *
 * @author ensornl
 */
public class RelatedIdentifierTypeSerializer extends StdSerializer<RelatedIdentifier.Type> {
    
    public RelatedIdentifierTypeSerializer() {
        this(null);
    }
    
    public RelatedIdentifierTypeSerializer(Class<RelatedIdentifier.Type> type) {
        super(type);
    }

    @Override
    public void serialize(RelatedIdentifier.Type t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("label", t.label());
        jg.writeStringField("value", t.name());
        jg.writeEndObject();
    }
    
}
