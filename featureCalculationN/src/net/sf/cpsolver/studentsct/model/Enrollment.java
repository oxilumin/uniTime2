package net.sf.cpsolver.studentsct.model;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.extension.TimeOverlapsCounter;
import net.sf.cpsolver.studentsct.reservation.Reservation;

/**
 * Representation of an enrollment of a student into a course. A student needs
 * to be enrolled in a section of each subpart of a selected configuration. When
 * parent-child relation is defined among sections, if a student is enrolled in
 * a section that has a parent section defined, he/she has be enrolled in the
 * parent section as well. Also, the selected sections cannot overlap in time. <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */

public class Enrollment extends Value<Request, Enrollment> {
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private Request iRequest = null;
    private Config iConfig = null;
    private Course iCourse = null;
    private Set<? extends Assignment> iAssignments = null;
    private Double iCachedPenalty = null;
    private int iPriority = 0;
    private Reservation iReservation = null;
    private Long iTimeStamp = null;
    private String iApproval = null;

    /**
     * Constructor
     * 
     * @param request
     *            course / free time request
     * @param priority
     *            zero for the course, one for the first alternative, two for the second alternative
     * @param course
     *            selected course
     * @param config
     *            selected configuration
     * @param assignments
     *            valid list of sections
     */
    public Enrollment(Request request, int priority, Course course, Config config, Set<? extends Assignment> assignments, Reservation reservation) {
        super(request);
        iRequest = request;
        iConfig = config;
        iAssignments = assignments;
        iPriority = priority;
        iCourse = course;
        if (iConfig != null && iCourse == null)
            for (Course c: ((CourseRequest)iRequest).getCourses()) {
                if (c.getOffering().getConfigs().contains(iConfig)) {
                    iCourse = c;
                    break;
                }
            }
        iReservation = reservation;
    }
    
    /**
     * Constructor
     * 
     * @param request
     *            course / free time request
     * @param priority
     *            zero for the course, one for the first alternative, two for the second alternative
     * @param config
     *            selected configuration
     * @param assignments
     *            valid list of sections
     */
    public Enrollment(Request request, int priority, Config config, Set<? extends Assignment> assignments) {
        this(request, priority, null, config, assignments, null);
        if (assignments != null)
            guessReservation(true);
    }
    
    /**
     * Guess the reservation based on the enrollment
     */
    public void guessReservation(boolean onlyAvailable) {
        if (iCourse != null) {
            Reservation best = null;
            boolean canAssignOverTheLimit = (variable().getModel() == null || ((StudentSectioningModel)variable().getModel()).getReservationCanAssignOverTheLimit());
            for (Reservation reservation: ((CourseRequest)iRequest).getReservations(iCourse)) {
                if (reservation.isIncluded(this)) {
                    if (onlyAvailable && reservation.getReservedAvailableSpace(iRequest) < iRequest.getWeight() &&
                       (!reservation.canAssignOverLimit() || !canAssignOverTheLimit))
                        continue;
                    if (best == null || best.getPriority() > reservation.getPriority()) {
                        best = reservation;
                    } else if (best.getPriority() == reservation.getPriority() &&
                        best.getReservedAvailableSpace(iRequest) < reservation.getReservedAvailableSpace(iRequest)) {
                        best = reservation;
                    }
                }
            }
            iReservation = best;
        }
    }
    
    /** Student */
    public Student getStudent() {
        return iRequest.getStudent();
    }

    /** Request */
    public Request getRequest() {
        return iRequest;
    }

    /** True if the request is course request */
    public boolean isCourseRequest() {
        return iConfig != null;
    }

    /** Offering of the course request */
    public Offering getOffering() {
        return (iConfig == null ? null : iConfig.getOffering());
    }

    /** Config of the course request */
    public Config getConfig() {
        return iConfig;
    }
    
    /** Course of the course request */
    public Course getCourse() {
        return iCourse;
    }

    /** List of assignments (selected sections) */
    @SuppressWarnings("unchecked")
    public Set<Assignment> getAssignments() {
        return (Set<Assignment>) iAssignments;
    }

    /** List of sections (only for course request) */
    @SuppressWarnings("unchecked")
    public Set<Section> getSections() {
        if (isCourseRequest())
            return (Set<Section>) iAssignments;
        return new HashSet<Section>();
    }

    /** True when this enrollment is overlapping with the given enrollment */
    public boolean isOverlapping(Enrollment enrl) {
        if (enrl == null || isAllowOverlap() || enrl.isAllowOverlap())
            return false;
        for (Assignment a : getAssignments()) {
            if (a.isOverlapping(enrl.getAssignments()))
                return true;
        }
        return false;
    }

    /** Percent of sections that are wait-listed */
    public double percentWaitlisted() {
        if (!isCourseRequest())
            return 0.0;
        CourseRequest courseRequest = (CourseRequest) getRequest();
        int nrWaitlisted = 0;
        for (Section section : getSections()) {
            if (courseRequest.isWaitlisted(section))
                nrWaitlisted++;
        }
        return ((double) nrWaitlisted) / getAssignments().size();
    }

    /** Percent of sections that are selected */
    public double percentSelected() {
        if (!isCourseRequest())
            return 0.0;
        CourseRequest courseRequest = (CourseRequest) getRequest();
        int nrSelected = 0;
        for (Section section : getSections()) {
            if (courseRequest.isSelected(section))
                nrSelected++;
        }
        return ((double) nrSelected) / getAssignments().size();
    }

    /** Percent of sections that are initial */
    public double percentInitial() {
        if (!isCourseRequest())
            return 0.0;
        if (getRequest().getInitialAssignment() == null)
            return 0.0;
        Enrollment inital = getRequest().getInitialAssignment();
        int nrInitial = 0;
        for (Section section : getSections()) {
            if (inital.getAssignments().contains(section))
                nrInitial++;
        }
        return ((double) nrInitial) / getAssignments().size();
    }

    /** True if all the sections are wait-listed */
    public boolean isWaitlisted() {
        if (!isCourseRequest())
            return false;
        CourseRequest courseRequest = (CourseRequest) getRequest();
        for (Iterator<? extends Assignment> i = getAssignments().iterator(); i.hasNext();) {
            Section section = (Section) i.next();
            if (!courseRequest.isWaitlisted(section))
                return false;
        }
        return true;
    }

    /** True if all the sections are selected */
    public boolean isSelected() {
        if (!isCourseRequest())
            return false;
        CourseRequest courseRequest = (CourseRequest) getRequest();
        for (Section section : getSections()) {
            if (!courseRequest.isSelected(section))
                return false;
        }
        return true;
    }

    /**
     * Enrollment penalty -- sum of section penalties (see
     * {@link Section#getPenalty()})
     */
    public double getPenalty() {
        if (iCachedPenalty == null) {
            double penalty = 0.0;
            if (isCourseRequest()) {
                for (Section section : getSections()) {
                    penalty += section.getPenalty();
                }
            }
            iCachedPenalty = new Double(penalty / getAssignments().size());
        }
        return iCachedPenalty.doubleValue();
    }

    /** Enrollment value */
    @Override
    public double toDouble() {
        return toDouble(true);
    }
    
    /** Enrollment value
     * @param precise if false, distance conflicts and time overlaps are ignored (i.e., much faster, but less precise computation)
     **/
    public double toDouble(boolean precise) {
        if (precise)
            return - getRequest().getWeight() * ((StudentSectioningModel)variable().getModel()).getStudentWeights().getWeight(this, distanceConflicts(), timeOverlappingConflicts());
        else {
            if (getExtra() != null) return - (Double) getExtra();
            return - getRequest().getWeight() * ((StudentSectioningModel)variable().getModel()).getStudentWeights().getWeight(this);
        }
    }
    
    /** Enrollment name */
    @Override
    public String getName() {
        if (getRequest() instanceof CourseRequest) {
            Course course = null;
            CourseRequest courseRequest = (CourseRequest) getRequest();
            for (Course c : courseRequest.getCourses()) {
                if (c.getOffering().getConfigs().contains(getConfig())) {
                    course = c;
                    break;
                }
            }
            String ret = (course == null ? getConfig() == null ? "" : getConfig().getName() : course.getName());
            for (Iterator<? extends Assignment> i = getAssignments().iterator(); i.hasNext();) {
                Section assignment = (Section) i.next();
                ret += "\n  " + assignment.getLongName() + (i.hasNext() ? "," : "");
            }
            return ret;
        } else if (getRequest() instanceof FreeTimeRequest) {
            return "Free Time " + ((FreeTimeRequest) getRequest()).getTime().getLongName();
        } else {
            String ret = "";
            for (Iterator<? extends Assignment> i = getAssignments().iterator(); i.hasNext();) {
                Assignment assignment = i.next();
                ret += assignment.toString() + (i.hasNext() ? "," : "");
                if (i.hasNext())
                    ret += "\n  ";
            }
            return ret;
        }
    }

    @Override
    public String toString() {
        if (getAssignments().isEmpty()) return "not assigned";
        Set<DistanceConflict.Conflict> dc = distanceConflicts();
        Set<TimeOverlapsCounter.Conflict> toc = timeOverlappingConflicts();
        int share = 0;
        if (toc != null)
            for (TimeOverlapsCounter.Conflict c: toc)
                share += c.getShare();
        String ret = sDF.format(toDouble()) + "/" + sDF.format(getRequest().getBound())
                + (getPenalty() == 0.0 ? "" : "/" + sDF.format(getPenalty()))
                + (dc == null || dc.isEmpty() ? "" : "/dc:" + dc.size())
                + (share <= 0 ? "" : "/toc:" + share);
        if (getRequest() instanceof CourseRequest) {
            ret += " ";
            for (Iterator<? extends Assignment> i = getAssignments().iterator(); i.hasNext();) {
                Assignment assignment = i.next();
                ret += assignment + (i.hasNext() ? ", " : "");
            }
        }
        if (getReservation() != null) ret = "(r) " + ret;
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Enrollment))
            return false;
        Enrollment e = (Enrollment) o;
        if (!ToolBox.equals(getConfig(), e.getConfig()))
            return false;
        if (!ToolBox.equals(getRequest(), e.getRequest()))
            return false;
        if (!ToolBox.equals(getAssignments(), e.getAssignments()))
            return false;
        return true;
    }

    /** Distance conflicts, in which this enrollment is involved. */
    public Set<DistanceConflict.Conflict> distanceConflicts() {
        if (!isCourseRequest())
            return null;
        if (getRequest().getModel() instanceof StudentSectioningModel) {
            DistanceConflict dc = ((StudentSectioningModel) getRequest().getModel()).getDistanceConflict();
            if (dc == null) return null;
            return dc.allConflicts(this);
        } else
            return null;
    }

    /** Time overlapping conflicts, in which this enrollment is involved. */
    public Set<TimeOverlapsCounter.Conflict> timeOverlappingConflicts() {
        if (getRequest().getModel() instanceof StudentSectioningModel) {
            TimeOverlapsCounter toc = ((StudentSectioningModel) getRequest().getModel()).getTimeOverlaps();
            if (toc == null)
                return null;
            return toc.allConflicts(this);
        } else
            return null;
    }

    /** 
     * Return enrollment priority
     * @return zero for the course, one for the first alternative, two for the second alternative
     */
    public int getPriority() {
        return iPriority;
    }
    
    /**
     * Return total number of slots of all sections in the enrollment.
     */
    public int getNrSlots() {
        int ret = 0;
        for (Assignment a: getAssignments()) {
            if (a.getTime() != null) ret += a.getTime().getLength() * a.getTime().getNrMeetings();
        }
        return ret;
    }
    
    /**
     * Return reservation used for this enrollment
     */
    public Reservation getReservation() { return iReservation; }
    
    /**
     * Set reservation for this enrollment
     */
    public void setReservation(Reservation reservation) { iReservation = reservation; }
    
    /**
     * Time stamp of the enrollment
     */
    public Long getTimeStamp() {
        return iTimeStamp;
    }

    /**
     * Time stamp of the enrollment
     */
    public void setTimeStamp(Long timeStamp) {
        iTimeStamp = timeStamp;
    }

    /**
     * Approval of the enrollment (only used by the online student sectioning)
     */
    public String getApproval() {
        return iApproval;
    }

    /**
     * Approval of the enrollment (only used by the online student sectioning)
     */
    public void setApproval(String approval) {
        iApproval = approval;
    }
    
    /**
     * True if this enrollment can overlap with other enrollments of the student.
     */
    public boolean isAllowOverlap() {
        return (getReservation() != null && getReservation().isAllowOverlap());
    }
    
    /**
     * Enrollment limit, i.e., the number of students that would be able to get into the offering using this enrollment (if all the sections are empty)  
     */
    public int getLimit() {
        if (!isCourseRequest()) return -1; // free time requests have no limit
        Integer limit = null;
        for (Section section: getSections())
            if (section.getLimit() >= 0) {
                if (limit == null)
                    limit = section.getLimit();
                else
                    limit = Math.min(limit, section.getLimit());
            }
        return (limit == null ? -1 : limit);
    }
    
}