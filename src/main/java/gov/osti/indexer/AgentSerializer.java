/*
 */
package gov.osti.indexer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.osti.entity.Agent;
import gov.osti.entity.Contributor;
import java.io.IOException;

/**
 * Special serialization for Agents for indexing purposes.
 * 
 * @author ensornl
 */
public class AgentSerializer extends StdSerializer<Agent> {
    
    public AgentSerializer() {
        this(null);
    }
    
    public AgentSerializer(Class<Agent> a) {
        super(a);
    }

    /**
     * Only serialize the desired portions of the Agent class.  Add 
     * contribution for Contributor types.
     * 
     * @param agent the Agent to serialize
     * @param jg the JsonGenerator to use
     * @param sp a SerializerProvider instance
     * @throws IOException on IO errors
     */
    @Override
    public void serialize(Agent agent, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("name", agent.toString());
        if (agent instanceof Contributor) {
            Contributor c = (Contributor) agent;
            if (null!=c.getContributorType())
                jg.writeStringField("contributorType", c.getContributorType().toString());
        }
        jg.writeEndObject();
    }
    
}
