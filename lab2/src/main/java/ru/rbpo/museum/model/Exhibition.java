package ru.rbpo.museum.model;

import java.time.LocalDate;

public class Exhibition extends BaseEntity {

    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExhibitionStatus status;
    /** Куратор выставки (ссылка на User), может отсутствовать. */
    private Long curatorId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public ExhibitionStatus getStatus() { return status; }
    public void setStatus(ExhibitionStatus status) { this.status = status; }

    public Long getCuratorId() { return curatorId; }
    public void setCuratorId(Long curatorId) { this.curatorId = curatorId; }
}
