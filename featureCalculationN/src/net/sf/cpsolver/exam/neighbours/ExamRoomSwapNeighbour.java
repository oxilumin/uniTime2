package net.sf.cpsolver.exam.neighbours;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.exam.model.ExamRoomSharing;
import net.sf.cpsolver.ifs.model.Neighbour;

/**
 * Swap a room between two assigned exams. <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
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
public class ExamRoomSwapNeighbour extends Neighbour<Exam, ExamPlacement> {
    private double iValue = 0;
    private ExamPlacement iX1, iX2 = null;
    private ExamRoomPlacement iR1, iR2 = null;

    public ExamRoomSwapNeighbour(ExamPlacement placement, ExamRoomPlacement current, ExamRoomPlacement swap) {
        if (placement.getRoomPlacements().contains(swap))
            return; // not an actual swap
        Exam exam = placement.variable();
        if (!swap.isAvailable(placement.getPeriod()))
            return; // room not available
        if (!exam.checkDistributionConstraints(swap))
            return; // a distribution constraint is violated
        int size = 0;
        for (ExamRoomPlacement r : placement.getRoomPlacements())
            size += r.getSize(exam.hasAltSeating());
        size -= current.getSize(exam.hasAltSeating());
        size += swap.getSize(exam.hasAltSeating());
        if (size < exam.getSize())
            return; // new room is too small
        ExamPlacement conflict = null;
        ExamRoomSharing rs = ((ExamModel)exam.getModel()).getRoomSharing();
        if (rs != null && placement.getRoomPlacements().size() == 1) {
            Set<ExamPlacement> x = new HashSet<ExamPlacement>();
            rs.computeConflicts(exam, swap.getRoom().getPlacements(placement.getPeriod()), swap.getRoom(), x);
            if (x.size() > 1) return;
            else if (x.size() == 1) conflict = x.iterator().next();
        } else {
            List<ExamPlacement> conf = swap.getRoom().getPlacements(placement.getPeriod());
            if (conf.size() > 1) return;
            else if (conf.size() == 1) conflict = conf.get(0);
        }
        if (conflict == null) {
            Set<ExamRoomPlacement> newRooms = new HashSet<ExamRoomPlacement>(placement.getRoomPlacements());
            newRooms.remove(current);
            newRooms.add(swap);
            for (Iterator<ExamRoomPlacement> i = newRooms.iterator(); i.hasNext();) {
                ExamRoomPlacement r = i.next();
                if (r.equals(swap))
                    continue;
                if (size - r.getSize(exam.hasAltSeating()) >= exam.getSize()) {
                    i.remove();
                    size -= r.getSize(exam.hasAltSeating());
                }
            }
            iX1 = new ExamPlacement(exam, placement.getPeriodPlacement(), newRooms);
            iValue = iX1.toDouble() - (exam.getAssignment() == null ? 0.0 : exam.getAssignment().toDouble());
        } else {
            Exam x = conflict.variable();
            ExamRoomPlacement xNew = x.getRoomPlacement(current.getRoom());
            if (xNew == null)
                return; // conflicting exam cannot be assigned in the current
                        // room
            if (!x.checkDistributionConstraints(xNew))
                return; // conflicting exam has a distribution constraint
                        // violated
            int xSize = 0;
            for (ExamRoomPlacement r : conflict.getRoomPlacements()) {
                xSize += r.getSize(x.hasAltSeating());
            }
            xSize -= swap.getSize(x.hasAltSeating());
            xSize += current.getSize(x.hasAltSeating());
            if (xSize < x.getSize())
                return; // current room is too small for the conflicting exam
            if (rs != null) {
                List<ExamPlacement> other = new ArrayList<ExamPlacement>(current.getRoom().getPlacements(conflict.getPeriod()));
                other.remove(placement);
                if (!other.isEmpty() && conflict.getRoomPlacements().size() > 1) return;
                if (rs.inConflict(x, other, current.getRoom())) return;
            }
            Set<ExamRoomPlacement> newRooms = new HashSet<ExamRoomPlacement>(placement.getRoomPlacements());
            newRooms.remove(current);
            newRooms.add(swap);
            for (Iterator<ExamRoomPlacement> i = newRooms.iterator(); i.hasNext();) {
                ExamRoomPlacement r = i.next();
                if (r.equals(swap))
                    continue;
                if (size - r.getSize(exam.hasAltSeating()) >= exam.getSize()) {
                    i.remove();
                    size -= r.getSize(exam.hasAltSeating());
                }
            }
            iX1 = new ExamPlacement(exam, placement.getPeriodPlacement(), newRooms);
            Set<ExamRoomPlacement> xRooms = new HashSet<ExamRoomPlacement>(conflict.getRoomPlacements());
            xRooms.remove(x.getRoomPlacement(swap.getRoom()));
            xRooms.add(xNew);
            for (Iterator<ExamRoomPlacement> i = xRooms.iterator(); i.hasNext();) {
                ExamRoomPlacement r = i.next();
                if (r.equals(swap))
                    continue;
                if (xSize - r.getSize(x.hasAltSeating()) >= x.getSize()) {
                    i.remove();
                    xSize -= r.getSize(x.hasAltSeating());
                }
            }
            iX2 = new ExamPlacement(x, conflict.getPeriodPlacement(), xRooms);
            iValue = iX1.toDouble() - (exam.getAssignment() == null ? 0.0 : exam.getAssignment().toDouble())
                    + iX2.toDouble() - conflict.toDouble();
        }
        iR1 = current;
        iR2 = swap;
    }

    public boolean canDo() {
        return iX1 != null;
    }

    @Override
    public void assign(long iteration) {
        if (iX2 == null) {
            iX1.variable().assign(iteration, iX1);
        } else {
            if (iX1.variable().getAssignment() != null)
                iX1.variable().unassign(iteration);
            iX2.variable().unassign(iteration);
            iX1.variable().assign(iteration, iX1);
            iX2.variable().assign(iteration, iX2);
        }
    }

    @Override
    public String toString() {
        if (iX2 == null) {
            return iX1.variable().getAssignment() + " -> " + iX1.toString() + " / " + " (value:" + value() + ")";
        } else {
            return iX1.variable().getName() + ": " + iR1.getRoom() + " <-> " + iR2.getRoom() + " (w "
                    + iX2.variable().getName() + ", value:" + value() + ")";
        }
    }

    protected static String toString(double[] x, double[] y) {
        DecimalFormat df = new DecimalFormat("0.00");
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < x.length; i++) {
            if (i > 0)
                s.append(",");
            s.append(df.format(x[i] - y[i]));
        }
        return "[" + s.toString() + "]";
    }

    @Override
    public double value() {
        return iValue;
    }
}
