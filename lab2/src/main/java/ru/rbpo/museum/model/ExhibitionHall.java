package ru.rbpo.museum.model;

public class ExhibitionHall extends BaseEntity {

    private String name;
    /** Площадь зала, м². */
    private Double area;
    private Boolean available;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}
