package gov.osti.entity;

import javax.persistence.Entity;

/**
 * The Developer Agent mapping.
 * 
 * @author ensornl
 */
@Entity (name = "DEVELOPERS")
public class Developer extends Agent {

    public Developer() {
        super();
    }
}
