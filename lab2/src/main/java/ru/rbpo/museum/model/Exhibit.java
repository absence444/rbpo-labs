package ru.rbpo.museum.model;

public class Exhibit extends BaseEntity {

    private String name;
    private ExhibitCondition condition;
    /** Требуемая площадь, м². */
    private Double requiredArea;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ExhibitCondition getCondition() { return condition; }
    public void setCondition(ExhibitCondition condition) { this.condition = condition; }

    public Double getRequiredArea() { return requiredArea; }
    public void setRequiredArea(Double requiredArea) { this.requiredArea = requiredArea; }
}
