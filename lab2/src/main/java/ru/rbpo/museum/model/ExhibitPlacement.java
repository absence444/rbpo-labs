package ru.rbpo.museum.model;

public class ExhibitPlacement extends BaseEntity {

    private Long exhibitId;
    private Long exhibitionId;
    private Long hallId;
    private MountingStatus mountingStatus;

    public Long getExhibitId() { return exhibitId; }
    public void setExhibitId(Long exhibitId) { this.exhibitId = exhibitId; }

    public Long getExhibitionId() { return exhibitionId; }
    public void setExhibitionId(Long exhibitionId) { this.exhibitionId = exhibitionId; }

    public Long getHallId() { return hallId; }
    public void setHallId(Long hallId) { this.hallId = hallId; }

    public MountingStatus getMountingStatus() { return mountingStatus; }
    public void setMountingStatus(MountingStatus mountingStatus) { this.mountingStatus = mountingStatus; }
}
