/*
 */
package gov.osti.indexer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.osti.entity.DOECodeMetadata.License;
import java.io.IOException;

/**
 *
 * @author ensornl
 */
public class LicenseSerializer extends StdSerializer<License> {
    
    public LicenseSerializer() {
        this(null);
    }
    
    public LicenseSerializer(Class<License> license) {
        super(license);
    }

    @Override
    public void serialize(License t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("label", t.label());
        jg.writeStringField("value", t.value());
        jg.writeStringField("key", t.name());
        jg.writeEndObject();
    }
    
}
