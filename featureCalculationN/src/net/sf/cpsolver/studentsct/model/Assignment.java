package net.sf.cpsolver.studentsct.model;

import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;

/**
 * Time and room assignment. This can be either {@link Section} or
 * {@link FreeTimeRequest}. <br>
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
public interface Assignment {
    /** Time assignment */
    public TimeLocation getTime();

    /**
     * Room assignment
     * 
     * @return list of {@link net.sf.cpsolver.coursett.model.RoomLocation}
     */
    public List<RoomLocation> getRooms();

    /** Number of rooms in which a section meets */
    public int getNrRooms();

    /**
     * True, if this assignment is overlapping in time and space with the given
     * assignment.
     */
    public boolean isOverlapping(Assignment assignment);

    /**
     * True, if this assignment is overlapping in time and space with the given
     * set of assignments.
     */
    public boolean isOverlapping(Set<? extends Assignment> assignments);

    /** Enrollment with this assignmnet was assigned to a {@link Request}. */
    public void assigned(Enrollment enrollment);

    /** Enrollment with this assignmnet was unassigned from a {@link Request}. */
    public void unassigned(Enrollment enrollment);

    /** Return the list of assigned enrollments that contains this assignment. */
    public Set<Enrollment> getEnrollments();
    
    /** Return true if overlaps are allowed, but the number of overlapping slots should be minimized. */
    public boolean isAllowOverlap();
    
    /** Unique id */
    public long getId();

    /** Compare assignments by unique ids. */
    public int compareById(Assignment a);
}
