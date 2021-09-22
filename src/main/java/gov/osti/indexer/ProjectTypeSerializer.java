/*
 */
package gov.osti.indexer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.osti.entity.DOECodeMetadata;
import java.io.IOException;

/**
 * Custom serializer for ProjectType Enumeration.
 * 
 * @author ensornl
 */
public class ProjectTypeSerializer extends StdSerializer<DOECodeMetadata.ProjectType> {
    
    public ProjectTypeSerializer() {
        this(null);
    }
    
    public ProjectTypeSerializer(Class<DOECodeMetadata.ProjectType> type) {
        super(type);
    }

    @Override
    public void serialize(DOECodeMetadata.ProjectType t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("label", t.label());
        jg.writeStringField("value", t.name());
        jg.writeEndObject();
    }
    
}
