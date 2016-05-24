//package Heuristics;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.log4j.Logger;
//
//import ExExExExamModel.ExExExExExam;
//import ExExExExamModel.ExPlacement;
//import ExamModel.ExModel;
//
//import org.cpsolver.ExExExExam.neighbours.ExExExExamRandomMove;
//import org.cpsolver.ExExExExam.neighbours.ExExExExamRoomMove;
//import org.cpsolver.ExExExExam.neighbours.ExExExExamSimpleNeighbour;
//import org.cpsolver.ExExExExam.neighbours.ExExExExamTimeMove;
//import org.cpsolver.ifs.assignment.Assignment;
//import org.cpsolver.ifs.assignment.context.AssignmentContext;
//import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
//import org.cpsolver.ifs.heuristics.NeighbourSelection;
//import org.cpsolver.ifs.model.LazyNeighbour;
//import org.cpsolver.ifs.model.Model;
//import org.cpsolver.ifs.model.Neighbour;
//import org.cpsolver.ifs.model.LazyNeighbour.LazyNeighbourAcceptanceCriterion;
//import org.cpsolver.ifs.solution.Solution;
//import org.cpsolver.ifs.solution.SolutionListener;
//import org.cpsolver.ifs.solver.Solver;
//import org.cpsolver.ifs.util.DataProperties;
//import org.cpsolver.ifs.util.Progress;
//import org.cpsolver.ifs.util.ToolBox;
//
//
///**
// * Hill climber. In each iteration, one of the following three neighbourhoods is
// * selected first
// * <ul>
// * <li>random move ({@link ExExExExamRandomMove})
// * <li>period swap ({@link ExExExExamTimeMove})
// * <li>room swap ({@link ExExExExamRoomMove})
// * </ul>
// * , then a neighbour is generated and it is accepted if its value
// * {@link ExExExExamSimpleNeighbour#value(Assignment)} is below or equal to zero. The search is
// * stopped after a given amount of idle iterations ( can be defined by problem
// * property HillClimber.MaxIdle). <br>
// * <br>
// *
// * @version ExExExExamTT 1.3 (ExExExExamination Timetabling)<br>
// *          Copyright (C) 2008 - 2014 Tomas Muller<br>
// *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
// *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
// * <br>
// *          This library is free software; you can redistribute it and/or modify
// *          it under the terms of the GNU Lesser General Public License as
// *          published by the Free Software Foundation; either version 3 of the
// *          License, or (at your option) any later version. <br>
// * <br>
// *          This library is distributed in the hope that it will be useful, but
// *          WITHOUT ANY WARRANTY; without even the implied warranty of
// *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// *          Lesser General Public License for more details. <br>
// * <br>
// *          You should have received a copy of the GNU Lesser General Public
// *          License along with this library; if not see
// *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
// */
//
//public class ExamHillClimbing extends NeighbourSelectionWithContext<ExExam, ExExExExamPlacement, ExExExExamHillClimbing.Context> implements SolutionListener<ExExExExam, ExExExExamPlacement>, LazyNeighbourAcceptanceCriterion<ExExExExam, ExExExExamPlacement> {
//    private List<NeighbourSelection<ExExam, ExExamPlacement>> iNeighbours = null;
//    private int iMaxIdleIters = 5000;
//
//    /**
//     * Constructor
//     *
//     * @param properties
//     *            problem properties (use HillClimber.MaxIdle to set maximum
//     *            number of idle iterations)
//     */
//    public ExExExExamHillClimbing(DataProperties properties) {
//        this(properties, "Hill Climbing");
//    }
//
//    /**
//     * Constructor
//     *
//     * @param properties
//     *            problem properties (use HillClimber.MaxIdle to set maximum
//     *            number of idle iterations)
//     * @param name solver search phase name
//     */
//    @SuppressWarnings("unchecked")
//    public ExExExExamHillClimbing(DataProperties properties, String name) {
//        iMaxIdleIters = properties.getPropertyInt("HillClimber.MaxIdle", iMaxIdleIters);
//        String neighbours = properties.getProperty("HillClimber.Neighbours",
//                ExExExExamRandomMove.class.getName() + ";" +
//                ExExExExamRoomMove.class.getName() + ";" +
//                ExExExExamTimeMove.class.getName());
//        neighbours += ";" + properties.getProperty("HillClimber.AdditionalNeighbours", "");
//        iNeighbours = new ArrayList<NeighbourSelection<ExExExExam,ExExExExamPlacement>>();
//        for (String neighbour: neighbours.split("\\;")) {
//            if (neighbour == null || neighbour.isEmpty()) continue;
//            try {
//                Class<NeighbourSelection<ExExExExam, ExExExExamPlacement>> clazz = (Class<NeighbourSelection<ExExExExam, ExExExExamPlacement>>)Class.forName(neighbour);
//                iNeighbours.add(clazz.getConstructor(DataProperties.class).newInstance(properties));
//            } catch (Exception e) {
//                sLog.error("Unable to use " + neighbour + ": " + e.getMessage());
//            }
//        }
//        iName = name;
//    }
//
//    /**
//     * Initialization
//     */
//    @Override
//    public void init(Solver<ExExExExam, ExExExExamPlacement> solver) {
//        super.init(solver);
//        solver.currentSolution().addSolutionListener(this);
//        for (NeighbourSelection<ExExam, ExExamPlacement> neighbour: iNeighbours)
//            neighbour.init(solver);
//        solver.setUpdateProgress(false);
//        iProgress = Progress.getInstance(solver.currentSolution().getModel());
//        getContext(solver.currentSolution().getAssignment()).reset();
//    }
//
//    /**
//     * Select one of the given neighbourhoods randomly, select neighbour, return
//     * it if its value is below or equal to zero (continue with the next
//     * selection otherwise). Return null when the given number of idle
//     * iterations is reached.
//     */
//    @Override
//    public Neighbour<ExExam, ExPlacement> selectNeighbour(Solution<ExExam, ExPlacement> solution) {
//        Context context = getContext(solution.getAssignment());
//        context.activateIfNeeded();
//        while (true) {
//            if (context.incIter(solution)) break;
//            NeighbourSelection<ExExam, ExPlacement> ns = iNeighbours.get(ToolBox.random(iNeighbours.size()));
//            Neighbour<ExExam, ExPlacement> n = ns.selectNeighbour(solution);
//            if (n != null) {
//                if (n instanceof LazyNeighbour) {
//                    ((LazyNeighbour<ExExam,ExPlacement>)n).setAcceptanceCriterion(this);
//                    return n;
//                } else if (n.value(solution.getAssignment()) <= 0.0) return n;
//            }
//        }
//        context.reset();
//        return null;
//    }
//
//    /**
//     * Memorize the iteration when the last best solution was found.
//     */
//    @Override
//    public void bestSaved(Solution<ExExam, ExPlacement> solution) {
//        getContext(solution.getAssignment()).bestSaved(solution.getModel());
//    }
//
//    @Override
//    public void solutionUpdated(Solution<ExExam, ExPlacement> solution) {
//    }
//
//    @Override
//    public void getInfo(Solution<ExExam, ExPlacement> solution, Map<String, String> info) {
//    }
//
//    @Override
//    public void getInfo(Solution<ExExam, ExPlacement> solution, Map<String, String> info, Collection<ExExam> variables) {
//    }
//
//    @Override
//    public void bestCleared(Solution<ExExam, ExPlacement> solution) {
//    }
//
//    @Override
//    public void bestRestored(Solution<ExExam, ExPlacement> solution) {
//    }
//
//    /** Accept lazy neighbour */
//    @Override
//    public boolean accept(Assignment<ExExam, ExPlacement> assignment, LazyNeighbour<ExExam, ExPlacement> neighbour, double value) {
//        return value <= 0.0;
//    }
//
//    @Override
//    public Context createAssignmentContext(Assignment<ExExExExam, ExExExExamPlacement> assignment) {
//        return new Context();
//    }
//
//    public class Context implements AssignmentContext {
//        private int iLastImprovingIter = 0;
//        private double iBestValue = 0;
//        private int iIter = 0;
//        private boolean iActive;
//
//        protected void reset() {
//            iIter = 0;
//            iLastImprovingIter = 0;
//            iActive = false;
//        }
//
//        protected void activateIfNeeded() {
//            if (!iActive) {
//                iProgress.setPhase(iName + "...");
//                iActive = true;
//            }
//        }
//
//        protected boolean incIter(Solution<ExExam, ExPlacement> solution) {
//            iIter++;
//            iProgress.setProgress(Math.round(100.0 * (iIter - iLastImprovingIter) / iMaxIdleIters));
//            if (iIter - iLastImprovingIter >= iMaxIdleIters) return true;
//            return false;
//        }
//
//        protected void bestSaved(ExModel<ExExam, ExPlacement> model) {
//            if (Math.abs(iBestValue - model.getBestValue()) >= 1.0) {
//                iLastImprovingIter = iIter;
//                iBestValue = model.getBestValue();
//            }
//        }
//    }
//}
