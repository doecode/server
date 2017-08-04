/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.File;
import java.io.IOException;

/**
 * Serialize the "file name" attribute WITHOUT any absolute path value stored
 * in the database.
 * 
 * @author ensornl
 */
public class FileNameSerializer extends StdSerializer<String> {

    public FileNameSerializer() {
        this(null);
    }
    
    public FileNameSerializer(Class<String> value) {
        super(value);
    }
    
    /**
     * Write out the "file name" String as the base name without any path information.
     * If null, skip this entirely.
     * 
     * @param fileName the FileName value to serialize
     * @param jg the JsonGenerator context to use
     * @param sp the SerializerProvider to use
     * @throws IOException on unexpected write errors
     */
    @Override
    public void serialize(String fileName, JsonGenerator jg, SerializerProvider sp) throws IOException {
        if (null!=fileName) {
            jg.writeString(fileName.substring(fileName.lastIndexOf(File.separator)+1));
        }
    }
    
}
