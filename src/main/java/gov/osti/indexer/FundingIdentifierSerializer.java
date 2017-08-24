/*
 */
package gov.osti.indexer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.osti.entity.FundingIdentifier;
import java.io.IOException;

/**
 * Custom JSON serializer for Funding Identifier Types.
 * 
 * @author ensornl
 */
public class FundingIdentifierSerializer extends StdSerializer<FundingIdentifier.Type> {
    public FundingIdentifierSerializer() {
        this(null);
    }
    
    public FundingIdentifierSerializer(Class<FundingIdentifier.Type> type) {
        super(type);
    }

    @Override
    public void serialize(FundingIdentifier.Type t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("label", t.label());
        jg.writeStringField("value", t.name());
        jg.writeEndObject();
    }
}
