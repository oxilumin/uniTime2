package net.sf.cpsolver.exam.heuristics;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriodPlacement;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.exam.neighbours.ExamSimpleNeighbour;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;

/**
 * Initial solution construction heuristics. <br>
 * <br>
 * While there are exams that are still not assigned:
 * <ul>
 * <li>The worst unassigned exam is selected (using {@link Exam#compareTo(Exam)})
 * <li>The best period that does not break any hard constraint and for which
 * there is a room assignment available is selected together with the set the
 * best available rooms for the exam in the best period (computed using
 * {@link Exam#findBestAvailableRooms(ExamPeriodPlacement)}).
 * </ul>
 * <br>
 * <br>
 * If problem property ExamConstruction.CheckLocalOptimality is true, local
 * (time) optimality is enforced at the end of this phase. During this
 * procedure, for each exam, it tries to change the period of the exam so that
 * the time cost is lower (see {@link ExamPlacement#getTimeCost()}), but no hard
 * constraint is violated. The problem is considered locally optimal if there is
 * no such move. <br>
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
public class ExamConstruction implements NeighbourSelection<Exam, ExamPlacement> {
    private HashSet<String> iAssignments = new HashSet<String>();
    private boolean iCheckLocalOptimality = false;
    private HashSet<Exam> iSkip = new HashSet<Exam>();
    private Progress iProgress = null;
    private boolean iActive;

    /**
     * Constructor
     * 
     * @param properties
     *            problem properties
     */
    public ExamConstruction(DataProperties properties) {
        iCheckLocalOptimality = properties.getPropertyBoolean("ExamConstruction.CheckLocalOptimality",
                iCheckLocalOptimality);
    }

    /**
     * Initialization
     */
    @Override
    public void init(Solver<Exam, ExamPlacement> solver) {
        iSkip.clear();
        solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iActive = false;
    }

    /**
     * Find a new assignment of one of the assigned exams that improves the time
     * cost {@link ExamPlacement#getTimeCost()} and for which there is a set of
     * available rooms {@link Exam#findBestAvailableRooms(ExamPeriodPlacement)}.
     * Return null, if there is no such assignment (the problem is considered
     * locally optimal).
     */
    public Neighbour<Exam, ExamPlacement> checkLocalOptimality(ExamModel model) {
        if (iCheckLocalOptimality) {
            for (Exam exam : model.assignedVariables()) {
                ExamPlacement current = exam.getAssignment();
                if (current.getTimeCost() <= 0)
                    continue;
                ExamPlacement best = null;
                for (ExamPeriodPlacement period : exam.getPeriodPlacements()) {
                    if (exam.countStudentConflicts(period) > 0) {
                        if (iAssignments.contains(exam.getId() + ":" + period.getIndex()))
                            continue;
                    }
                    if (!exam.checkDistributionConstraints(period))
                        continue;
                    ExamPlacement placement = new ExamPlacement(exam, period, null);
                    if (best == null || best.getTimeCost() > placement.getTimeCost()) {
                        Set<ExamRoomPlacement> rooms = exam.findBestAvailableRooms(period);
                        if (rooms != null)
                            best = new ExamPlacement(exam, period, rooms);
                    }
                }
                if (best != null && best.getTimeCost() < current.getTimeCost())
                    return new ExamSimpleNeighbour(best);
            }
        }
        iActive = false;
        return null;
    }

    /**
     * Select a neighbour. While there are exams that are still not assigned:
     * <ul>
     * <li>The worst unassigned exam is selected (using
     * {@link Exam#compareTo(Exam)})
     * <li>The best period that does not break any hard constraint and for which
     * there is a room assignment available is selected together with the set
     * the best available rooms for the exam in the best period (computed using
     * {@link Exam#findBestAvailableRooms(ExamPeriodPlacement)}).
     * </ul>
     * Return null when done (all variables are assigned and the problem is
     * locally optimal).
     */
    @Override
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        ExamModel model = (ExamModel) solution.getModel();
        if (!iActive) {
            iActive = true;
            iProgress.setPhase("Construction ...", model.variables().size());
        }
        iProgress.setProgress(model.nrAssignedVariables());
        if (model.nrUnassignedVariables() <= iSkip.size())
            return checkLocalOptimality(model);
        Exam bestExam = null;
        for (Exam exam : model.unassignedVariables()) {
            if (iSkip.contains(exam))
                continue;
            if (bestExam == null || exam.compareTo(bestExam) < 0)
                bestExam = exam;
        }
        ExamPlacement best = null;
        for (ExamPeriodPlacement period : bestExam.getPeriodPlacements()) {
            if (bestExam.countStudentConflicts(period) > 0) {
                if (iAssignments.contains(bestExam.getId() + ":" + period.getIndex()))
                    continue;
            }
            if (!bestExam.checkDistributionConstraints(period))
                continue;
            ExamPlacement placement = new ExamPlacement(bestExam, period, null);
            if (best == null || best.getTimeCost() > placement.getTimeCost()) {
                Set<ExamRoomPlacement> rooms = bestExam.findBestAvailableRooms(period);
                if (rooms != null)
                    best = new ExamPlacement(bestExam, period, rooms);
            }
        }
        if (best != null) {
            iAssignments.add(bestExam.getId() + ":" + best.getPeriod().getIndex());
            return new ExamSimpleNeighbour(best);
        } /*
           * else { for (Enumeration
           * e=bestExam.getPeriodPlacements().elements();e.hasMoreElements();) {
           * ExamPeriodPlacement period = (ExamPeriodPlacement)e.nextElement();
           * ExamPlacement placement = new ExamPlacement(bestExam, period,
           * null); if (best==null ||
           * best.getTimeCost()>placement.getTimeCost()) { Set rooms =
           * bestExam.findBestAvailableRooms(period); if (rooms!=null) best =
           * new ExamPlacement(bestExam, period, rooms); } } if (best!=null) {
           * bestExam.allowAllStudentConflicts(best.getPeriod());
           * iAssignments.add(bestExam.getId()+":"+best.getPeriod().getIndex());
           * return new ExamSimpleNeighbour(best); } }
           */
        if (!iSkip.contains(bestExam)) {
            iSkip.add(bestExam);
            return selectNeighbour(solution);
        }
        return checkLocalOptimality(model);
    }

}