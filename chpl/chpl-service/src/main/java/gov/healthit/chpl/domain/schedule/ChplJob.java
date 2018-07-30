package gov.healthit.chpl.domain.schedule;

import java.io.Serializable;

import org.quartz.JobDetail;

import gov.healthit.chpl.domain.concept.ScheduleFrequencyConcept;

/**
 * Represents the data for a Quartz job, along with other meta data for the job.
 * @author TYoung
 *
 */
public class ChplJob implements Serializable{
    private static final long serialVersionUID = -7634197357525761051L;
    private String description;
    private String group;
    private String name;
    private ScheduleFrequencyConcept frequency;

    /**
     * Default (empty) constructor.
     */
    public ChplJob() { }

    /**
     * Constructor that build a ChplJob based on a Quartz Job object.
     * @param jobDetails the Quartz JobDetail
     */
    public ChplJob(final JobDetail jobDetails) {
        this.description = jobDetails.getDescription();
        this.group = jobDetails.getKey().getGroup();
        this.name = jobDetails.getKey().getName();
        if (jobDetails.getJobDataMap().containsKey("frequency")) {
            this.frequency = ScheduleFrequencyConcept.valueOf(jobDetails.getJobDataMap().get("frequency").toString());
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public ScheduleFrequencyConcept getFrequency() {
        return frequency;
    }

    public void setFrequency(final ScheduleFrequencyConcept frequency) {
        this.frequency = frequency;
    }

}
