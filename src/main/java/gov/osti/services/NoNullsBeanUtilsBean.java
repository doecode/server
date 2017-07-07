/*
 */
package gov.osti.services;

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.beanutils.BeanUtilsBean;

/**
 * Extend Apache's BeanUtilsBean to copy only non-null properties for merging
 * purposes.
 * 
 * @author ensornl
 */
public class NoNullsBeanUtilsBean extends BeanUtilsBean {
    
    /**
     * Copy Bean properties where they are NOT NULL.
     * 
     * @param dest the DESTINATION bean
     * @param name the PROPERTY NAME
     * @param value the VALUE to copy
     * @throws IllegalAccessException on unavailable method names
     * @throws InvocationTargetException on bad targets
     */
    @Override
    public void copyProperty(Object dest, String name, Object value)
            throws IllegalAccessException, InvocationTargetException {
        // skip any NULLs
        if (null!=value) {
            super.copyProperty(dest, name, value);
        }
    }
}
