/*
 */
package gov.osti.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import javax.persistence.EntityManager;
import gov.osti.listeners.DoeServletContextListener;
import javax.persistence.TypedQuery;

/**
 * A Generic Identifier for role; includes Admin and Standard roles.
 * 
 * @author sowerst
 */
@JsonIgnoreProperties ( ignoreUnknown = true )
public class UserRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Enumeration of valid Admin Roles for a Role.
     */
    public static enum RoleType {
        ADMIN, STANDARD, HQ;
    }

    private String value;
    private String label;
    private String description;

    public UserRole() {

    }

    public UserRole(String value, String label, String description) {
        this.setValue(value);
        this.setLabel(label);
        this.setDescription(description);
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param value the value to set
     */
    public void setLabel(String value) {
        this.label = value;
    }

    /**
     * @return the value
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param value the value to set
     */
    public void setDescription(String value) {
        this.description = value;
    }

    public static List<UserRole> GetRoles(RoleType roleType) {
        List<UserRole> roles = new ArrayList<>();
        if (RoleType.ADMIN.equals(roleType)) {
            roles.add(new UserRole("RecordAdmin", "Record Admin", "Permission to edit any project, regardless of Site affiliation."));
            roles.add(new UserRole("SiteAdmin", "Site Admin", "Permission to edit any Site and POC notifications."));
            roles.add(new UserRole("UserAdmin", "User Admin", "Permission to edit any User information and Role associations."));
            roles.add(new UserRole("ApprovalAdmin", "Approval Admin", "Permission to approve any project for biblio indexing."));
            roles.add(new UserRole("ContentAdmin", "Content Admin", "Permission to access content controls, such as Refresh, Reindex, etc."));
        }
        else if (RoleType.STANDARD.equals(roleType) || RoleType.HQ.equals(roleType)) {
            String namedQuery = "Site.find" + (RoleType.HQ.equals(roleType) ? "HQ" : "Standard");
            EntityManager em = DoeServletContextListener.createEntityManager();        
            try {
                // get ALL SITES
                TypedQuery<Site> query = em.createNamedQuery(namedQuery, Site.class);
                List<Site> siteList = query.getResultList();
    
                for (Site site:siteList) {
                    String code = site.getSiteCode();
                    String lab = site.getLabName();
                    roles.add(new UserRole(code, code, lab));
                }
            } finally {
                em.close();
            }
        }

        return roles;
    }

    public static List<String> GetRoleList(RoleType roleType) {
        List<String> roles = new ArrayList<>();
        if (RoleType.ADMIN.equals(roleType)) {
            roles.add("RecordAdmin");
            roles.add("SiteAdmin");
            roles.add("UserAdmin");
            roles.add("ApprovalAdmin");
            roles.add("ContentAdmin");
        }
        else if (RoleType.STANDARD.equals(roleType) || RoleType.HQ.equals(roleType)) {
            String namedQuery = "Site.find" + (RoleType.HQ.equals(roleType) ? "HQ" : "Standard");
            EntityManager em = DoeServletContextListener.createEntityManager();        
            try {
                // get ALL SITES
                TypedQuery<Site> query = em.createNamedQuery(namedQuery, Site.class);
                List<Site> siteList = query.getResultList();
    
                for (Site site:siteList) {
                    roles.add(site.getSiteCode());
                }
            } finally {
                em.close();
            }
        }
    
        return roles;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof UserRole ) {
            return ((UserRole)o).getValue().equals(getValue());
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.value);
        return hash;
    }

}
