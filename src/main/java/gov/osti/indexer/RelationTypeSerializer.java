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
public class RelationTypeSerializer extends StdSerializer<RelatedIdentifier.RelationType> {
    public RelationTypeSerializer() {
        this(null);
    }
    
    public RelationTypeSerializer(Class<RelatedIdentifier.RelationType> type) {
        super(type);
    }

    @Override
    public void serialize(RelatedIdentifier.RelationType t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("label", t.label());
        jg.writeStringField("value", t.name());
        jg.writeEndObject();
    }
}
