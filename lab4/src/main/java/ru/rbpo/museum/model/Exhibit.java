package ru.rbpo.museum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exhibits")
public class Exhibit extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ExhibitCondition condition;

    /** Требуемая площадь, м². */
    @Column(name = "required_area", nullable = false)
    private Double requiredArea;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ExhibitCondition getCondition() { return condition; }
    public void setCondition(ExhibitCondition condition) { this.condition = condition; }

    public Double getRequiredArea() { return requiredArea; }
    public void setRequiredArea(Double requiredArea) { this.requiredArea = requiredArea; }
}
