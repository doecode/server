package gov.osti.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "limitations")
@NamedQueries({
    @NamedQuery(name = "Limitation.findByGroup", query = "SELECT l FROM Limitation l WHERE l.accessGroup = lower(:group)")
    ,
    @NamedQuery(name = "Limitation.findByAccessCode", query = "SELECT l FROM Limitation l WHERE l.accessCode = :code")
    ,
    @NamedQuery(name = "Limitation.findAll", query = "SELECT l FROM Limitation l ORDER BY l.displayOrder, l.accessCode")
})
public class Limitation implements Serializable {

    private String accessCode;
    private String accessDescription;
    private String accessGroup;
    private Integer displayOrder;

    public Limitation() {
    }

    @Id
    @Column(name = "ACCESS_CODE")
    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String code) {
        this.accessCode = code;
    }

    @Column(name = "ACCESS_DESCRIPTION")
    public String getAccessDescription() {
        return accessDescription;
    }

    public void setAccessDescription(String descript) {
        this.accessDescription = descript;
    }

    @Column(name = "ACCESS_GROUP")
    public String getAccessGroup() {
        return accessGroup;
    }

    public void setAccessGroup(String lab) {
        this.accessGroup = lab;
    }

    @Column(name = "DISPLAY_ORDER")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer ord) {
        this.displayOrder = ord;
    }
}
