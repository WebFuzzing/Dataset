package bdp.sample.notebookmanager.entities;

import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.hateoas.RepresentationModel;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
public class NoteBook extends RepresentationModel {
    private Integer ID;
    private String name;
    private double currentPrice;
    private Timestamp lastUpdate;

    public NoteBook(){

    }

    public NoteBook(Integer ID, String name, double currentPrice){
        this.ID = ID;
        this.name = name;
        this.currentPrice = currentPrice;
    }
    public NoteBook(String name, double currentPrice){
        this.name = name;
        this.currentPrice = currentPrice;
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY) // id generate by the database native approach
    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    @Basic
    @Column(name = "name", unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "currentPrice")
    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    @Basic
    @Column(name = "lastUpdate")
    @UpdateTimestamp

    public Timestamp getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Timestamp lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteBook notebook = (NoteBook) o;
        return ID == notebook.ID && currentPrice == notebook.currentPrice && Objects.equals(name, notebook.name) && Objects.equals(lastUpdate, notebook.lastUpdate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, name, currentPrice, lastUpdate);
    }
}
