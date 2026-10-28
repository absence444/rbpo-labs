package ru.rbpo.museum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "exhibition_halls")
public class ExhibitionHall extends BaseEntity {

    @Column(nullable = false, length = 200, unique = true)
    private String name;

    /** Площадь зала, м². */
    @Column(nullable = false)
    private Double area;

    @Column(nullable = false)
    private Boolean available;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}
