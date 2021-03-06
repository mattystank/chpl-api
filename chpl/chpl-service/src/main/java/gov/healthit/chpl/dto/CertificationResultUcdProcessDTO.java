package gov.healthit.chpl.dto;

import java.io.Serializable;

import gov.healthit.chpl.entity.listing.CertificationResultUcdProcessEntity;

public class CertificationResultUcdProcessDTO implements Serializable {
    private static final long serialVersionUID = 6026116684333913764L;
    private Long id;
    private Long certificationResultId;
    private Long ucdProcessId;
    private String ucdProcessName;
    private String ucdProcessDetails;

    public CertificationResultUcdProcessDTO() {
    }

    public CertificationResultUcdProcessDTO(CertificationResultUcdProcessEntity entity) {
        this.id = entity.getId();
        this.certificationResultId = entity.getCertificationResultId();
        this.ucdProcessId = entity.getUcdProcessId();
        this.ucdProcessDetails = entity.getUcdProcessDetails();
        if (entity.getUcdProcess() != null) {
            this.ucdProcessName = entity.getUcdProcess().getName();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getCertificationResultId() {
        return certificationResultId;
    }

    public void setCertificationResultId(final Long certificationResultId) {
        this.certificationResultId = certificationResultId;
    }

    public Long getUcdProcessId() {
        return ucdProcessId;
    }

    public void setUcdProcessId(final Long ucdProcessId) {
        this.ucdProcessId = ucdProcessId;
    }

    public String getUcdProcessName() {
        return ucdProcessName;
    }

    public void setUcdProcessName(final String ucdProcessName) {
        this.ucdProcessName = ucdProcessName;
    }

    public String getUcdProcessDetails() {
        return ucdProcessDetails;
    }

    public void setUcdProcessDetails(final String ucdProcessDetails) {
        this.ucdProcessDetails = ucdProcessDetails;
    }
}
