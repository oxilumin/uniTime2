package net.sf.cpsolver.studentsct.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.constraint.ConfigLimit;
import net.sf.cpsolver.studentsct.constraint.CourseLimit;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.reservation.Reservation;

/**
 * Representation of a request of a student for one or more course. A student
 * requests one of the given courses, preferably the first one. <br>
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
public class CourseRequest extends Request {
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private List<Course> iCourses = null;
    private Set<Choice> iWaitlistedChoices = new HashSet<Choice>();
    private Set<Choice> iSelectedChoices = new HashSet<Choice>();
    private boolean iWaitlist = false;
    private Long iTimeStamp = null;
    private Double iCachedMinPenalty = null, iCachedMaxPenalty = null;
    public static boolean sSameTimePrecise = false;

    /**
     * Constructor
     * 
     * @param id
     *            request unique id
     * @param priority
     *            request priority
     * @param alternative
     *            true if the request is alternative (alternative request can be
     *            assigned instead of a non-alternative course requests, if it
     *            is left unassigned)
     * @param student
     *            appropriate student
     * @param courses
     *            list of requested courses (in the correct order -- first is
     *            the requested course, second is the first alternative, etc.)
     * @param waitlist
     *            time stamp of the request if the student can be put on a wait-list (no alternative
     *            course request will be given instead)
     */
    public CourseRequest(long id, int priority, boolean alternative, Student student, java.util.List<Course> courses,
            boolean waitlist, Long timeStamp) {
        super(id, priority, alternative, student);
        iCourses = new ArrayList<Course>(courses);
        for (Course course: iCourses)
            course.getRequests().add(this);
        iWaitlist = waitlist;
        iTimeStamp = timeStamp;
    }

    /**
     * List of requested courses (in the correct order -- first is the requested
     * course, second is the first alternative, etc.)
     */
    public List<Course> getCourses() {
        return iCourses;
    }

    /**
     * Create enrollment for the given list of sections. The list of sections
     * needs to be correct, i.e., a section for each subpart of a configuration
     * of one of the requested courses.
     */
    public Enrollment createEnrollment(Set<? extends Assignment> sections, Reservation reservation) {
        if (sections == null || sections.isEmpty())
            return null;
        Config config = ((Section) sections.iterator().next()).getSubpart().getConfig();
        Course course = null;
        for (Course c: iCourses) {
            if (c.getOffering().getConfigs().contains(config)) {
                course = c;
                break;
            }
        }
        return new Enrollment(this, iCourses.indexOf(course), course, config, sections, reservation);
    }

    /**
     * Create enrollment for the given list of sections. The list of sections
     * needs to be correct, i.e., a section for each subpart of a configuration
     * of one of the requested courses.
     */
    public Enrollment createEnrollment(Set<? extends Assignment> sections) {
        Enrollment ret = createEnrollment(sections, null);
        ret.guessReservation(true);
        return ret;
        
    }
    
    /**
     * Maximal domain size (i.e., number of enrollments of a course request), -1 if there is no limit.
     */
    protected int getMaxDomainSize() {
        StudentSectioningModel model = (StudentSectioningModel) getModel();
        return model == null ? null : model.getMaxDomainSize();
    }

    /**
     * Return all possible enrollments.
     */
    @Override
    public List<Enrollment> computeEnrollments() {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        int idx = 0;
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(ret, idx, 0, course, config, new HashSet<Section>(), 0, false, false,
                        false, false, getMaxDomainSize() <= 0 ? -1 : ret.size() + getMaxDomainSize());
            }
            idx++;
        }
        return ret;
    }

    /**
     * Return a subset of all enrollments -- randomly select only up to
     * limitEachConfig enrollments of each config.
     */
    public List<Enrollment> computeRandomEnrollments(int limitEachConfig) {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        int idx = 0;
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(ret, idx, 0, course, config, new HashSet<Section>(), 0, false, false,
                        false, true, (limitEachConfig <= 0 ? limitEachConfig : ret.size() + limitEachConfig));
            }
            idx++;
        }
        return ret;
    }

    /**
     * Return true if the both sets of sections contain sections of the same
     * subparts, and each pair of sections of the same subpart is offered at the
     * same time.
     */
    private boolean sameTimes(Set<Section> sections1, Set<Section> sections2) {
        for (Section s1 : sections1) {
            Section s2 = null;
            for (Section s : sections2) {
                if (s.getSubpart().equals(s1.getSubpart())) {
                    s2 = s;
                    break;
                }
            }
            if (s2 == null)
                return false;
            if (!ToolBox.equals(s1.getTime(), s2.getTime()))
                return false;
        }
        return true;
    }

    /**
     * Recursive computation of enrollments
     * 
     * @param enrollments
     *            list of enrollments to be returned
     * @param priority
     *            zero for the course, one for the first alternative, two for the second alternative
     * @param penalty
     *            penalty of the selected sections
     * @param course
     *            selected course
     * @param config
     *            selected configuration
     * @param sections
     *            sections selected so far
     * @param idx
     *            index of the subparts (a section of 0..idx-1 subparts has been
     *            already selected)
     * @param availableOnly
     *            only use available sections
     * @param skipSameTime
     *            for each possible times, pick only one section
     * @param selectedOnly
     *            select only sections that are selected (
     *            {@link CourseRequest#isSelected(Section)} is true)
     * @param random
     *            pick sections in a random order (useful when limit is used)
     * @param limit
     *            when above zero, limit the number of selected enrollments to
     *            this limit
     * @param reservations
     *            list of applicable reservations
     */
    private void computeEnrollments(Collection<Enrollment> enrollments, int priority, double penalty, Course course, Config config,
            HashSet<Section> sections, int idx, boolean availableOnly, boolean skipSameTime, boolean selectedOnly,
            boolean random, int limit) {
        if (limit > 0 && enrollments.size() >= limit)
            return;
        if (idx == 0) { // run only once for each configuration
            boolean canOverLimit = false;
            if (availableOnly && (getModel() == null || ((StudentSectioningModel)getModel()).getReservationCanAssignOverTheLimit())) {
                for (Reservation r: getReservations(course)) {
                    if (!r.canAssignOverLimit()) continue;
                    if (!r.getConfigs().isEmpty() && !r.getConfigs().contains(config)) continue;
                    if (r.getReservedAvailableSpace(this) < getWeight()) continue;
                    canOverLimit = true; break;
                }
            }
            if (!canOverLimit) {
                if (availableOnly && config.getLimit() >= 0 && ConfigLimit.getEnrollmentWeight(config, this) > config.getLimit())
                    return;
                if (availableOnly && course.getLimit() >= 0 && CourseLimit.getEnrollmentWeight(course, this) > course.getLimit())
                    return;
                if (config.getOffering().hasReservations()) {
                    boolean hasReservation = false, hasOtherReservation = false, hasConfigReservation = false, reservationMustBeUsed = false;
                    for (Reservation r: getReservations(course)) {
                        if (r.mustBeUsed()) reservationMustBeUsed = true;
                        if (availableOnly && r.getReservedAvailableSpace(this) < getWeight()) continue;
                        if (r.getConfigs().isEmpty()) {
                            hasReservation = true;
                        } else if (r.getConfigs().contains(config)) {
                            hasReservation = true;
                            hasConfigReservation = true;
                        } else {
                            hasOtherReservation = true;
                        }
                    }
                    if (!hasConfigReservation && config.getTotalUnreservedSpace() < getWeight())
                        return;
                    if (!hasReservation && config.getOffering().getTotalUnreservedSpace() < getWeight())
                        return;
                    if (hasOtherReservation && !hasReservation)
                        return;
                    if (availableOnly && !hasReservation && config.getOffering().getUnreservedSpace(this) < getWeight())
                        return;
                    if (availableOnly && !hasConfigReservation && config.getUnreservedSpace(this) < getWeight())
                        return;
                    if (!hasReservation && reservationMustBeUsed)
                        return;
                }
            }
        }
        if (config.getSubparts().size() == idx) {
            if (skipSameTime && sSameTimePrecise) {
                boolean waitListedOrSelected = false;
                if (!getSelectedChoices().isEmpty() || !getWaitlistedChoices().isEmpty()) {
                    for (Section section : sections) {
                        if (isWaitlisted(section) || isSelected(section)) {
                            waitListedOrSelected = true;
                            break;
                        }
                    }
                }
                if (!waitListedOrSelected) {
                    for (Enrollment enrollment : enrollments) {
                        if (sameTimes(enrollment.getSections(), sections))
                            return;
                    }
                }
            }
            if (!config.getOffering().hasReservations()) {
                enrollments.add(new Enrollment(this, priority, null, config, new HashSet<Assignment>(sections), null));
            } else {
                Enrollment e = new Enrollment(this, priority, null, config, new HashSet<Assignment>(sections), null);
                boolean mustHaveReservation = config.getOffering().getTotalUnreservedSpace() < getWeight();
                boolean mustHaveConfigReservation = config.getTotalUnreservedSpace() < getWeight();
                boolean mustHaveSectionReservation = false;
                for (Section s: sections) {
                    if (s.getTotalUnreservedSpace() < getWeight()) {
                        mustHaveSectionReservation = true;
                        break;
                    }
                }
                boolean canOverLimit = false;
                if (availableOnly &&  (getModel() == null || ((StudentSectioningModel)getModel()).getReservationCanAssignOverTheLimit())) {
                    for (Reservation r: getReservations(course)) {
                        if (!r.canAssignOverLimit() || !r.isIncluded(e)) continue;
                        if (r.getReservedAvailableSpace(this) < getWeight()) continue;
                        enrollments.add(new Enrollment(this, priority, null, config, new HashSet<Assignment>(sections), r));
                        canOverLimit = true;
                    }
                }
                if (!canOverLimit) {
                    boolean reservationMustBeUsed = false;
                    reservations: for (Reservation r: (availableOnly ? new TreeSet<Reservation>(getReservations(course)) : getReservations(course))) {
                        if (r.mustBeUsed()) reservationMustBeUsed = true;
                        if (!r.isIncluded(e)) continue;
                        if (availableOnly && r.getReservedAvailableSpace(this) < getWeight()) continue;
                        if (mustHaveConfigReservation && r.getConfigs().isEmpty()) continue;
                        if (mustHaveSectionReservation)
                            for (Section s: sections)
                                if (r.getSections(s.getSubpart()) == null && s.getTotalUnreservedSpace() < getWeight()) continue reservations;
                        enrollments.add(new Enrollment(this, priority, null, config, new HashSet<Assignment>(sections), r));
                        if (availableOnly) return; // only one available reservation suffice (the best matching one)
                    }
                    // a case w/o reservation
                    if (!(mustHaveReservation || mustHaveConfigReservation || mustHaveSectionReservation) &&
                        !(availableOnly && config.getOffering().getUnreservedSpace(this) < getWeight()) &&
                        !reservationMustBeUsed) {
                        enrollments.add(new Enrollment(this, (getReservations(course).isEmpty() ? 0 : 1) + priority, null, config, new HashSet<Assignment>(sections), null));
                    }
                }
            }
        } else {
            Subpart subpart = config.getSubparts().get(idx);
            HashSet<TimeLocation> times = (skipSameTime ? new HashSet<TimeLocation>() : null);
            List<Section> sectionsThisSubpart = subpart.getSections();
            if (skipSameTime) {
                sectionsThisSubpart = new ArrayList<Section>(subpart.getSections());
                Collections.sort(sectionsThisSubpart);
            }
            List<Section> matchingSectionsThisSubpart = new ArrayList<Section>(subpart.getSections().size());
            boolean hasChildren = !subpart.getChildren().isEmpty();
            for (Section section : sectionsThisSubpart) {
                if (section.getParent() != null && !sections.contains(section.getParent()))
                    continue;
                if (section.isOverlapping(sections))
                    continue;
                if (selectedOnly && !isSelected(section))
                    continue;
                boolean canOverLimit = false;
                if (availableOnly && (getModel() == null || ((StudentSectioningModel)getModel()).getReservationCanAssignOverTheLimit())) {
                    for (Reservation r: getReservations(course)) {
                        if (!r.canAssignOverLimit()) continue;
                        if (r.getSections(subpart) != null && !r.getSections(subpart).contains(section)) continue;
                        if (r.getReservedAvailableSpace(this) < getWeight()) continue;
                        canOverLimit = true; break;
                    }
                }
                if (!canOverLimit) {
                    if (availableOnly && section.getLimit() >= 0
                            && SectionLimit.getEnrollmentWeight(section, this) > section.getLimit())
                        continue;
                    if (config.getOffering().hasReservations()) {
                        boolean hasReservation = false, hasSectionReservation = false, hasOtherReservation = false, reservationMustBeUsed = false;
                        for (Reservation r: getReservations(course)) {
                            if (r.mustBeUsed()) reservationMustBeUsed = true;
                            if (availableOnly && r.getReservedAvailableSpace(this) < getWeight()) continue;
                            if (r.getSections(subpart) == null) {
                                hasReservation = true;
                            } else if (r.getSections(subpart).contains(section)) {
                                hasReservation = true;
                                hasSectionReservation = true;
                            } else {
                                hasOtherReservation = true;
                            }
                        }
                        if (!hasSectionReservation && section.getTotalUnreservedSpace() < getWeight())
                            continue;
                        if (hasOtherReservation && !hasReservation)
                            continue;
                        if (availableOnly && !hasSectionReservation && section.getUnreservedSpace(this) < getWeight())
                            continue;
                        if (!hasReservation && reservationMustBeUsed)
                            continue;
                    }
                }
                if (skipSameTime && section.getTime() != null && !hasChildren && !times.add(section.getTime()) && !isSelected(section) && !isWaitlisted(section))
                    continue;
                matchingSectionsThisSubpart.add(section);
            }
            if (random || limit > 0)
                Collections.shuffle(sectionsThisSubpart);
            int i = 0;
            for (Section section: matchingSectionsThisSubpart) {
                sections.add(section);
                computeEnrollments(enrollments, priority, penalty + section.getPenalty(), course, config, sections, idx + 1,
                        availableOnly, skipSameTime, selectedOnly, random,
                        limit < 0 ? limit : Math.max(1, limit * (1 + i) / matchingSectionsThisSubpart.size())
                        );
                sections.remove(section);
                i++;
            }
        }
    }

    /** Return all enrollments that are available */
    public List<Enrollment> getAvaiableEnrollments() {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        int idx = 0;
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(ret, idx, 0, course, config, new HashSet<Section>(), 0, true, false, false, false,
                        getMaxDomainSize() <= 0 ? -1 : ret.size() + getMaxDomainSize());
            }
            idx++;
        }
        return ret;
    }

    /**
     * Return all enrollments that are selected (
     * {@link CourseRequest#isSelected(Section)} is true)
     * 
     * @param availableOnly
     *            pick only available sections
     */
    public List<Enrollment> getSelectedEnrollments(boolean availableOnly) {
        if (getSelectedChoices().isEmpty())
            return null;
        Choice firstChoice = getSelectedChoices().iterator().next();
        List<Enrollment> enrollments = new ArrayList<Enrollment>();
        for (Course course : iCourses) {
            if (!course.getOffering().equals(firstChoice.getOffering()))
                continue;
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(enrollments, 0, 0, course, config, new HashSet<Section>(), 0, availableOnly, false, true, false, -1);
            }
        }
        return enrollments;
    }

    /**
     * Return all enrollments that are available, pick only the first section of
     * the sections with the same time (of each subpart, {@link Section}
     * comparator is used)
     */
    public List<Enrollment> getAvaiableEnrollmentsSkipSameTime() {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        if (getInitialAssignment() != null)
            ret.add(getInitialAssignment());
        int idx = 0;
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(ret, idx, 0, course, config, new HashSet<Section>(), 0, true, true, false, false,
                        getMaxDomainSize() <= 0 ? -1 : ret.size() + getMaxDomainSize());
            }
            idx++;
        }
        return ret;
    }

    /**
     * Return all possible enrollments.
     */
    public List<Enrollment> getEnrollmentsSkipSameTime() {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        int idx = 0;
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(ret, idx, 0, course, config, new HashSet<Section>(), 0, false, true, false, false,
                        getMaxDomainSize() <= 0 ? -1 : ret.size() + getMaxDomainSize());
            }
            idx++;
        }
        return ret;
    }

    /** Wait-listed choices */
    public Set<Choice> getWaitlistedChoices() {
        return iWaitlistedChoices;
    }

    /**
     * Return true when the given section is wait-listed (i.e., its choice is
     * among wait-listed choices)
     */
    public boolean isWaitlisted(Section section) {
        return iWaitlistedChoices.contains(section.getChoice());
    }

    /** Selected choices */
    public Set<Choice> getSelectedChoices() {
        return iSelectedChoices;
    }

    /**
     * Return true when the given section is selected (i.e., its choice is among
     * selected choices)
     */
    public boolean isSelected(Section section) {
        return iSelectedChoices.contains(section.getChoice());
    }

    /**
     * Request name: A for alternative, 1 + priority, (w) when waitlist, list of
     * course names
     */
    @Override
    public String getName() {
        String ret = (isAlternative() ? "A" : "")
                + (1 + getPriority() + (isAlternative() ? -getStudent().nrRequests() : 0)) + ". "
                + (isWaitlist() ? "(w) " : "");
        int idx = 0;
        for (Course course : iCourses) {
            if (idx == 0)
                ret += course.getName();
            else
                ret += ", " + idx + ". alt " + course.getName();
            idx++;
        }
        return ret;
    }

    /**
     * True if the student can be put on a wait-list (no alternative course
     * request will be given instead)
     */
    public boolean isWaitlist() {
        return iWaitlist;
    }
    
    /**
     * True if the student can be put on a wait-list (no alternative course
     * request will be given instead)
     */
    public void setWaitlist(boolean waitlist) {
        iWaitlist = waitlist;
    }
    
    /**
     * Time stamp of the request
     */
    public Long getTimeStamp() {
        return iTimeStamp;
    }

    @Override
    public String toString() {
        return getName() + (getWeight() != 1.0 ? " (W:" + sDF.format(getWeight()) + ")" : "");
    }

    /** Return course of the requested courses with the given id */
    public Course getCourse(long courseId) {
        for (Course course : iCourses) {
            if (course.getId() == courseId)
                return course;
        }
        return null;
    }

    /** Return configuration of the requested courses with the given id */
    public Config getConfig(long configId) {
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                if (config.getId() == configId)
                    return config;
            }
        }
        return null;
    }

    /** Return subpart of the requested courses with the given id */
    public Subpart getSubpart(long subpartId) {
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                for (Subpart subpart : config.getSubparts()) {
                    if (subpart.getId() == subpartId)
                        return subpart;
                }
            }
        }
        return null;
    }

    /** Return section of the requested courses with the given id */
    public Section getSection(long sectionId) {
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                for (Subpart subpart : config.getSubparts()) {
                    for (Section section : subpart.getSections()) {
                        if (section.getId() == sectionId)
                            return section;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Minimal penalty (minimum of {@link Offering#getMinPenalty()} among
     * requested courses)
     */
    public double getMinPenalty() {
        if (iCachedMinPenalty == null) {
            double min = Double.MAX_VALUE;
            for (Course course : iCourses) {
                min = Math.min(min, course.getOffering().getMinPenalty());
            }
            iCachedMinPenalty = new Double(min);
        }
        return iCachedMinPenalty.doubleValue();
    }

    /**
     * Maximal penalty (maximum of {@link Offering#getMaxPenalty()} among
     * requested courses)
     */
    public double getMaxPenalty() {
        if (iCachedMaxPenalty == null) {
            double max = Double.MIN_VALUE;
            for (Course course : iCourses) {
                max = Math.max(max, course.getOffering().getMaxPenalty());
            }
            iCachedMaxPenalty = new Double(max);
        }
        return iCachedMaxPenalty.doubleValue();
    }

    /** Clear cached min/max penalties and cached bound */
    public void clearCache() {
        iCachedMaxPenalty = null;
        iCachedMinPenalty = null;
    }

    /**
     * Estimated bound for this request -- it estimates the smallest value among
     * all possible enrollments
     */
    @Override
    public double getBound() {
        return - getWeight() * ((StudentSectioningModel)getModel()).getStudentWeights().getBound(this);
        /*
        if (iCachedBound == null) {
            iCachedBound = new Double(-Math.pow(Enrollment.sPriorityWeight, getPriority())
                    * (isAlternative() ? Enrollment.sAlterativeWeight : 1.0)
                    * Math.pow(Enrollment.sInitialWeight, (getInitialAssignment() == null ? 0 : 1))
                    * Math.pow(Enrollment.sSelectedWeight, (iSelectedChoices.isEmpty() ? 0 : 1))
                    * Math.pow(Enrollment.sWaitlistedWeight, (iWaitlistedChoices.isEmpty() ? 0 : 1))
                    *
                    // Math.max(Enrollment.sMinWeight,getWeight()) *
                    (getStudent().isDummy() ? Student.sDummyStudentWeight : 1.0)
                    * Enrollment.normalizePenalty(getMinPenalty()));
        }
        return iCachedBound.doubleValue();
        */
    }

    /** Return true if request is assigned. */
    @Override
    public boolean isAssigned() {
        return getAssignment() != null && !(getAssignment()).getAssignments().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && (o instanceof CourseRequest);
    }
    
    /**
     * Get reservations for this course requests
     */
    public List<Reservation> getReservations(Course course) {
        if (iReservations == null)
            iReservations = new HashMap<Course, List<Reservation>>();
        List<Reservation> reservations = iReservations.get(course);
        if (reservations == null) {
            reservations = new ArrayList<Reservation>();
            boolean mustBeUsed = false;
            for (Reservation r: course.getOffering().getReservations()) {
                if (!r.isApplicable(getStudent())) continue;
                if (!mustBeUsed && r.mustBeUsed()) { reservations.clear(); mustBeUsed = true; }
                if (mustBeUsed && !r.mustBeUsed()) continue;
                reservations.add(r);
            }
            iReservations.put(course, reservations);
        }
        return reservations;
    }
    private Map<Course, List<Reservation>> iReservations = null;
    
    /**
     * Return true if there is a reservation for a course of this request
     */
    public boolean hasReservations() {
        for (Course course: getCourses())
            if (!getReservations(course).isEmpty())
                return true;
        return false;
    }
    
    /**
     * Clear reservation information that was cached on this section
     */
    public void clearReservationCache() {
        if (iReservations != null) iReservations.clear();
    }

}
