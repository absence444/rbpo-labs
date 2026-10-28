package ru.rbpo.museum.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exhibit_placements")
public class ExhibitPlacement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibit_id", nullable = false)
    private Exhibit exhibit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private ExhibitionHall hall;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "mounting_status", nullable = false, length = 20)
    private MountingStatus mountingStatus;

    @Column(name = "mounted_at")
    private Instant mountedAt;

    public Exhibit getExhibit() { return exhibit; }
    public void setExhibit(Exhibit exhibit) { this.exhibit = exhibit; }

    public Exhibition getExhibition() { return exhibition; }
    public void setExhibition(Exhibition exhibition) { this.exhibition = exhibition; }

    public ExhibitionHall getHall() { return hall; }
    public void setHall(ExhibitionHall hall) { this.hall = hall; }

    public MountingStatus getMountingStatus() { return mountingStatus; }
    public void setMountingStatus(MountingStatus mountingStatus) { this.mountingStatus = mountingStatus; }

    public Instant getMountedAt() { return mountedAt; }
    public void setMountedAt(Instant mountedAt) { this.mountedAt = mountedAt; }
}
