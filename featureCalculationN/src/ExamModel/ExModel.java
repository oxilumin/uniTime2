package ExamModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.model.BinaryConstraint;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.itc.ItcModel;

import org.apache.log4j.Logger;



/**
 * Representation of Examination Timetabling (exam) problem model.
 * 
 * @version
 * ITC2007 1.0<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not see
 * <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class ExModel extends ItcModel<ExExam, ExPlacement> {
	
    private static Logger sLog = Logger.getLogger(ExModel.class);
    
    private int iNumberOfAfterConstraints = 0, iNumberOfExclusionConstraints = 0, iNumberOfExamCoincidenceConstraints = 0, iNumberOfRoomExclusiveConstraints = 0; 
    
    private List<ExPeriod> iPeriods = new ArrayList<ExPeriod>();
    private List<ExRoom> iRooms = new ArrayList<ExRoom>();
    private List<ExStudent> iStudents = new ArrayList<ExStudent>();
    private int iTwoInARow = 0, iTwoInADay = 0, iPeriodSpread = 0, iNonMixedDurations = 0;
    private int iFrontLoad[] = new int [] {0, 0, 0};
    private List<ExExam> iLargestExams = new ArrayList<ExExam>();
    private List<BinaryConstraint<ExExam, ExPlacement>> iBinaryConstraints = new ArrayList<BinaryConstraint<ExExam,ExPlacement>>();
    private int iFronLoadThreshold = 0;
    
    private int iNumberOfExams = 0;
    
    private int iFronLoadPenalty = 0, iPeriodPenalty = 0, iRoomPenalty = 0;
    private int iTwoInARowPenalty = 0, iTwoInADayPenalty = 0, iWiderSpreadPenalty = 0;
    private int iMixedDurationsPenalty = 0;
    
    private int iBinaryViolationWeight = 5000;
    private int iDirectConflictWeight = 1000;
    
    private int iBinaryViolations = 0, iNrDirectConflicts = 0;
    
    /** Constructor */
    public ExModel() {
        super();
        iAssignedVariables = null;
        iUnassignedVariables = null;
        iPerturbVariables = null;
    }
    
    public int getNumberOfExams() {
    	return iNumberOfExams;
    }
    /** List of periods
     * @return list of {@link ExPeriod}
     */
    public List<ExPeriod> getPeriods() {
        return iPeriods;
    }
    /** First period */
    public ExPeriod firstPeriod() {
        return iPeriods.get(0);
    }
    /** Last period */
    public ExPeriod lastPeriod() {
        return iPeriods.get(iPeriods.size() - 1);
    }
    /** Number of different days */
    public int getNrDays() {
        return lastPeriod().getDay() + 1;
    }
    private int nrTimes = -1;
    /** Number of different times */
    public int getNrTimes() {
        if (nrTimes<0) {
            for (ExPeriod p: getPeriods())
                nrTimes = Math.max(nrTimes,p.getTime()+1);
        }
        return nrTimes;
    }
    /** Number of periods */
    public int getNrPeriods() {
        return iPeriods.size();
    }
    /** Period of given index (0 .. getNrPeriods()-1)*/
    public ExPeriod getPeriod(int period) {
        return iPeriods.get(period);
    }    
    /** Add a period */
    protected void addPeriod(String day, String time, int length, int weight) {
        ExPeriod lastPeriod = (iPeriods.isEmpty() ? null : iPeriods.get(iPeriods.size() - 1));
        ExPeriod p = new ExPeriod(day, time, length, weight);
        if (lastPeriod==null)
            p.setIndex(iPeriods.size(),0,0);
        else if (lastPeriod.getDayStr().equals(day)) {
            p.setIndex(iPeriods.size(), lastPeriod.getDay(), lastPeriod.getTime()+1);
        } else
            p.setIndex(iPeriods.size(), lastPeriod.getDay()+1, 0);
        if (lastPeriod!=null) {
            lastPeriod.setNext(p);
            p.setPrev(lastPeriod);
        }
        iPeriods.add(p);
    }
    
    private Boolean iDirectConflictsAllowed = null;
    
    /** True if direct conflicts are allowed (default is false, to be changed by Exam.AllowDirectConflict parameter) */
    public boolean areDirectConflictsAllowed() {
        if (iDirectConflictsAllowed==null)
            iDirectConflictsAllowed = false; //getProperties().getPropertyBoolean("Exam.AllowDirectConflict", Boolean.FALSE);
        return iDirectConflictsAllowed.booleanValue();
    }
    
    private Boolean iBinaryViolationsAllowed = null;
    /** True if binary constraints can be violated (default is false, to be changed by Exam.AllowBinaryViolations parameter) */
    public boolean areBinaryViolationsAllowed() {
       // if (iBinaryViolationsAllowed==null)
       //     iBinaryViolationsAllowed = getProperties().getPropertyBoolean("Exam.AllowBinaryViolations", Boolean.FALSE);
        return  false; //iBinaryViolationsAllowed.booleanValue();
    }

    /** 
     * Load input problem (see <a href='http://www.cs.qub.ac.uk/itc2007/examtrack/exam_track_index_files/Inputformat.htm'>input format</a> for more details)
     */
    public boolean load(File file) throws Exception {
    	
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line = null;
        String state = null; int count = 0;
        Hashtable<Integer, ExStudent> students = new Hashtable<Integer, ExStudent>();
        Hashtable<Integer, ExExam> exams = new Hashtable<Integer, ExExam>();
        
        while ((line=in.readLine())!=null) {
        	
            if (line.startsWith("[") && line.endsWith("]")) {
                state = line.substring(1,line.length()-1);
                
                if (state.indexOf(':')>=0) {
                    count = Integer.parseInt(state.substring(state.indexOf(':')+1));
                    state = state.substring(0,state.indexOf(':'));
                    
                } else {
                    count = 0;
                }
                //sLog.debug("state:"+state+", count:"+count);
                if (count==0) continue;
            }
            
            if (line.trim().length()==0) continue;
            if ("Exams".equals(state)) {
            	iNumberOfExams = count;
                for (int i=0;i<count;i++) {
                	
                    line = in.readLine();
                    StringTokenizer stk = new StringTokenizer(line, ", ");
                    int length = Integer.parseInt(stk.nextToken());
                    ExExam exam = new ExExam(variables().size(),length);
                    
                    while (stk.hasMoreTokens()) {
                        int studentId = Integer.parseInt(stk.nextToken());
                        ExStudent student = students.get(new Integer(studentId));
                        if (student==null) {
                            	/*if (areDirectConflictsAllowed())
                            		student = new ExStudentSoft(studentId);
                            	else */
                            student = new ExStudentHard(studentId);
                            students.put(new Integer(studentId),student);
                            addConstraint(student);
                            iStudents.add(student);
                        }
                        student.addVariable(exam);
                        exam.getStudents().add(student);
                    }
                    
                    addVariable(exam);
                    exams.put(new Integer(i),exam);
                }
            }
            if ("Periods".equals(state)) {
                for (int i=0;i<count;i++) {
                    line = in.readLine();
                    StringTokenizer stk = new StringTokenizer(line, ", ");
                    addPeriod(
                            stk.nextToken(),
                            stk.nextToken(),
                            Integer.parseInt(stk.nextToken()),
                            Integer.parseInt(stk.nextToken()));
                }
            }
            if ("Rooms".equals(state)) {
                for (int i=0;i<count;i++) {
                    line = in.readLine();
                    StringTokenizer stk = new StringTokenizer(line, ", ");
                    ExRoom room = new ExRoom(i,
                            Integer.parseInt(stk.nextToken()),
                            Integer.parseInt(stk.nextToken()));
                    iRooms.add(room);
                    addConstraint(room);
                }
            }
            if ("PeriodHardConstraints".equals(state)) {
                StringTokenizer stk = new StringTokenizer(line, ", ");
                ExExam ex1 = (ExExam)exams.get(Integer.valueOf(stk.nextToken()));
                String constraint = stk.nextToken();
                ExExam ex2 = (ExExam)exams.get(Integer.valueOf(stk.nextToken()));
                if (ex1.equals(ex2)) {
                    sLog.info("Constraint "+constraint+" posted between "+ex1+" and "+ex2+".");
                } else {
                    BinaryConstraint<ExExam, ExPlacement> c = null;
                    if ("AFTER".equals(constraint)) {
                    	c = new ExPrecedence(ex2,ex1,true);
                    	iNumberOfAfterConstraints ++;
                    }
                        //c = new ExPrecedence(ex2,ex1,!areBinaryViolationsAllowed());
                    else if ("EXAM_COINCIDENCE".equals(constraint)) {
                    	c = new ExSamePeriod(ex1,ex2,true);
                    	iNumberOfExamCoincidenceConstraints ++;
                        //c = new ExSamePeriod(ex1,ex2,!areBinaryViolationsAllowed());
                    }
                    else if ("EXCLUSION".equals(constraint)) {
                    	c = new ExDifferentPeriod(ex1,ex2,true);
                    	iNumberOfExclusionConstraints ++;
                        //c = new ExDifferentPeriod(ex1,ex2,!areBinaryViolationsAllowed());
                    }
                    else
                        sLog.error("Unknown ["+state+"] constraint "+constraint);
                    if (c!=null) {
                        if (constraints().contains(c)) {
                            sLog.info("Constraint "+constraint+" posted between "+ex1+" and "+ex2+" already exists.");
                            ex1.binaryConstraints().remove(c);
                            ex2.binaryConstraints().remove(c);
                        } else {
                            addConstraint(c);
                            iBinaryConstraints.add(c);
                        }
                    }
                }
            }
            if ("RoomHardConstraints".equals(state)) {
                StringTokenizer stk = new StringTokenizer(line, ", ");
                ExExam ex1 = (ExExam)exams.get(Integer.valueOf(stk.nextToken()));
                String constraint = stk.nextToken();
                if ("ROOM_EXCLUSIVE".equals(constraint)) {
                    ex1.setRoomExclusive(true);
                    iNumberOfRoomExclusiveConstraints ++;
                }
                else
                    sLog.error("Unknown ["+state+"] constraint "+constraint);
            }
            if ("InstitutionalWeightings".equals(state)) {
                StringTokenizer stk = new StringTokenizer(line, ", ");
                String constraint = stk.nextToken();
                if ("TWOINAROW".equals(constraint)) {
                    iTwoInARow = Integer.parseInt(stk.nextToken());
                } else if ("TWOINADAY".equals(constraint)) {
                    iTwoInADay = Integer.parseInt(stk.nextToken());
                } else if ("PERIODSPREAD".equals(constraint)) {
                    iPeriodSpread = Integer.parseInt(stk.nextToken());
                } else if ("NONMIXEDDURATIONS".equals(constraint)) {
                    iNonMixedDurations = Integer.parseInt(stk.nextToken());
                } else if ("FRONTLOAD".equals(constraint)) {
                    iFrontLoad[0] = Integer.parseInt(stk.nextToken());
                    iFrontLoad[1] = Integer.parseInt(stk.nextToken());
                    iFrontLoad[2] = Integer.parseInt(stk.nextToken());
                } else {
                    sLog.error("Unknown ["+state+"] constraint "+constraint);
                }
            }
        }
        
        for (Constraint<ExExam, ExPlacement> c: constraints()) {
            if (c instanceof ExRoom)
                ((ExRoom)c).init();
            if (c instanceof ExStudent)
                ((ExStudent)c).init();
        }
        
        for (ExExam exam: variables()) {
            exam.init();
            for (Constraint<ExExam, ExPlacement> c: exam.constraints()) {
                if (c instanceof ExSamePeriod) {
                    ExSamePeriod sp = (ExSamePeriod)c;
                    exam.getSamePeriodExams().add(sp.another(exam));
                    for (ExStudent student: exam.getStudents()) {
                        if (sp.another(exam).getStudents().contains(student)) {
                            sLog.warn("Student "+student+" assigned to same-period exams "+exam+" and "+sp.another(exam));
                            student.removeVariable(exam);
                        }
                    }
                }
            }
        }
        TreeSet<ExExam> orderedExams = new TreeSet<ExExam>(new Comparator<ExExam>() {
            public int compare(ExExam e1, ExExam e2) {
                int cmp = -Double.compare(e1.getStudents().size(), e2.getStudents().size());
                if (cmp!=0) return cmp;
                return Double.compare(e1.getId(), e2.getId());
            }
        });
        orderedExams.addAll(variables());
        for (Iterator<ExExam> i=orderedExams.iterator();i.hasNext() && iLargestExams.size()<iFrontLoad[0];) {
            ExExam exam = i.next();
            exam.setIsLargest(true);
            iLargestExams.add(exam);
        }

        iFronLoadThreshold = Math.max(0, getNrPeriods() - iFrontLoad[1]);
        sLog.info("Front load threshold is "+iFronLoadThreshold+" ("+getPeriod(iFronLoadThreshold)+")");
        
        return true;
    }
    
    /** List of all rooms */
    public List<ExRoom> getRooms() {
        return iRooms;
    }
    
    /** List of all students **/
    
    public List<ExStudent> getStudents() {
    	return iStudents;
    }
    
    public List<ExExam> getExams() {
    	return iLargestExams;
    }
    
    /** Two exams in a row penalty:
     * Count the number of occurrences where two examinations are taken by students straight after one another i.e. back to back. Once this has been established, the number of students involved in each occurance should be added and multiplied by the number provided in the two in a row' weighting within the Institutional Model Index'.  Note that two exams in a row are not counted overnight e.g. if a student has an exam the last period of one day and another the first period the next day, this does not count as two in a row.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getTwoInARow(boolean precise) {
        if (!precise) return iTwoInARow*iTwoInARowPenalty;
        if (iTwoInARow==0) return 0;
        int penalty = 0;
        for (ExStudent student: iStudents)
            penalty += student.getTwoInARow();
        return iTwoInARow*penalty;
    }

    /**
     * Two exams in a day penalty:
     * In the case where there are three periods or more in a day, count the number of occurrences of students having two exams in a day which are not directly adjacent, i.e. not back to back, and multiply this by the ' two in a day' weighting provided within the 'Institutional Model Index'. Therefore, two exams in a day are considered as those which are not adjacent i.e. they have a free period between them. This is done to ensure a particular exam placing within a solution does not contribute twice to the overall penalty. For example if Exam A and Exam B were in adjacent periods in the same day the penalty would be counted as part of the 'Two exams in a row penalty'.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getTwoInADay(boolean precise) {
        if (!precise) return iTwoInADay*iTwoInADayPenalty;
        if (iTwoInADay==0) return 0;
        int penalty = 0;
        for (ExStudent student: iStudents)
            penalty += student.getTwoInADay();
        return iTwoInADay*penalty;
    }
    
    /**
     * Period spread penalty:
     * This constraint allows an organisation to 'spread' an individual's examinations over a specified number of periods. This can be thought of an extension of the two constraints previously described.  Within the �Institutional Model Index', a figure is provided relating to how many periods the solution should be �optimised' over. The higher this figure, potentially the better the spread of examinations for individual students. In many institutions constructing solutions while changing this setting has led to timetables which the Institution is much more satisfied with.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getWiderSpread(boolean precise) {
        if (!precise) return iWiderSpreadPenalty;
        int penalty = 0;
        for (ExStudent student: iStudents)
            penalty += student.getWiderSpread(iPeriodSpread);
        return penalty;
    }
    
    /**
     * Mixed durations penalty:
     *  This applies a penalty to a ROOM and PERIOD (not Exam) where there are mixed durations, such that:
     *  For Each Period, For Each Room, Penalty = (number of different durations -1 ) * 'NONMIXEDDURATIONS' weighting.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getMixedDurations(boolean precise) {
        if (!precise) return iNonMixedDurations*iMixedDurationsPenalty;
        int penalty = 0;
        for (ExRoom room: iRooms)
            penalty += room.getMixedDurations();
        return iNonMixedDurations*penalty;
    }
    
    /**
     * Room penalty:
     * It is often the case that organisations want to keep certain room usage to a minimum. As with the 'Mixed Durations' component of the overall penalty, this part of the overall penalty should be calculated on a period by period basis. For each period, if a room used within the solution has an associated penalty, the penalty for that room for that period is calculated by multiplying the associated penalty by the number of times the room is used.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getRoomPenalty(boolean precise) {
        if (!precise) return iRoomPenalty;
        int penalty = 0;
        for (ExRoom room: iRooms)
            penalty += room.getRoomPenalty();
        return penalty;
    }
    
    /** Period spread length (parameter PERIODSPREAD) */
    public int getPeriodSpreadLength() {
        return iPeriodSpread;
    }
    
    /** Front load threshold (index of the first period that has a penalty if used by a large exam) */
    public int getFronLoadThreshold() {
        return iFronLoadThreshold;
    }
    
    /** Mixed duration weight (parameter NONMIXEDDURATIONS) */
    public int getMixedDurationWeight() {
        return iNonMixedDurations;
    }
    
    /** Two exams in a day weight (parameter TWOINADAY) */
    public int getTwoInADayWeight() {
        return iTwoInADay;
    }

    /** Two exams in a row weight (parameter TWOINAROW) */
    public int getTwoInARowWeight() {
        return iTwoInARow;
    }

    /** Front load weight (third argument of parameter FRONTLOAD) */
    public int getFrontLoadWeight() {
        return iFrontLoad[2];
    }
    
    public int getFrontLoadPenaltyValue() {
    	return iFrontLoad[1];
    }
    
    public int getNumberOfAfterConstraints() {
    	return iNumberOfAfterConstraints;
    }
    
    public int getNumberOfExclusionConstraints() {
    	return iNumberOfExclusionConstraints;
    }
    
    public int getNumberOfCoincidenceConstraints() {
    	return iNumberOfExamCoincidenceConstraints;
    }
    
    public int getNumberOfRoomExclusive() {
    	return iNumberOfRoomExclusiveConstraints;
    }
    /** Binary constraint violation weight */
    public int getBinaryViolationWeight() {
        return iBinaryViolationWeight;
    }

    /** Direct student conflict weight */
    public int getDirectConflictWeight() {
        return iDirectConflictWeight;
    }
    
    /** Set binary constraint violation weight */
    public void setBinaryViolationWeight(int binaryViolationWeight) {
        iBinaryViolationWeight = binaryViolationWeight;
    }

    /** Set direct student conflict weight */
    public void setDirectConflictWeight(int directConflictWeight) {
        iDirectConflictWeight = directConflictWeight;
    }

    /**
     * Period penalty:
     * It is often the case that organisations want to keep certain period usage to a minimum. As with the 'Mixed Durations' and the 'Room Penalty' components of the overall penalty, this part of the overall penalty should be calculated on a period by period basis. For each period the penalty is calculated by multiplying the associated penalty by the number of times the exams timetabled within that period.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getPeriodPenalty(boolean precise) {
        if (!precise) return iPeriodPenalty;
        int penalty = 0;
        for (ExExam x: assignedVariables())
            penalty += x.getAssignment().getPeriodPenalty();
        return penalty;
    }

    /**
     * Front load penalty:
     * It is desirable that examinations with the largest numbers of students are timetabled at the beginning of the examination session. In order to take account of this the FRONTLOAD expression is introduced. Within the �Intuitional Model Index' the FRONTLOAD expression has three parameters e.g., 100, 30, 5
     * <br>First parameter = number of largest exams. Largest exams are specified by class size
     * <br>Second parameter= number of last periods to take into account
     * <br>Third parameter= the penalty or weighting 
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getFrontLoadPenalty(boolean precise) {
        if (!precise) return iFrontLoad[2]*iFronLoadPenalty;
        int penalty = 0;
        for (ExExam exam: iLargestExams) {
            ExPlacement p = exam.getAssignment();
            if (p==null) continue;
            if (p.getPeriodIndex()>=getFronLoadThreshold()) penalty++;
        }
        return iFrontLoad[2]*penalty;
    }

    /**
     * Save solution into a given file (see <a href='http://www.cs.qub.ac.uk/itc2007/examtrack/exam_track_index_files/outputformat.htm'>Output Format</a> for more details).
     */
    public boolean save(File file) throws Exception {
        PrintWriter out = new PrintWriter(new FileWriter(file));
        
        for (ExExam exam: variables()) {
            ExPlacement placement = (ExPlacement)exam.getAssignment();
            out.print(placement==null?"-1, -1":placement.getPeriod().getIndex()+", "+placement.getRoom().getId()+"\r\n");
        }
        
        out.flush(); out.close();
        

        if ("true".equals(System.getProperty("report"))) {
            out = new PrintWriter(new FileWriter(new File(file.getParentFile(),file.getName().substring(0,file.getName().lastIndexOf('.'))+".txt")));
            report(out);
            out.flush(); out.close();
        }

        return true;
    }
    
    /**
     * Weighted number of binary constraint violations
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getBinaryViolations(boolean precise) {
        if (!precise) return iBinaryViolations;
        int violations = 0;
        for (BinaryConstraint<ExExam, ExPlacement> bc: iBinaryConstraints) {
            if (bc.isHard()) continue;
            if (bc.first().getAssignment()!=null && bc.second().getAssignment()!=null && 
                    !bc.isConsistent(bc.first().getAssignment(), bc.second().getAssignment())) violations++;
        }
        return violations;
    }
    
    /**
     * Weighted number of direct student conflicts 
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getNrDirectConflicts(boolean precise) {
        if (!precise) return iNrDirectConflicts;
        int conflicts = 0;
        for (ExStudent student: iStudents)
            conflicts += student.getNrDirectConflicts();
        return conflicts;
    }

    /**
     * Total solution value (weighted sum of all criteria)
     */
    public double getTotalValue() {
        return getTotalValue(false);
    }
    
    /**
     * Total solution value (weighted sum of all criteria)
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public double getTotalValue(boolean precise) {
        return
            getTwoInARow(precise)+
            getTwoInADay(precise)+
            getWiderSpread(precise)+
            getMixedDurations(precise)+
            getFrontLoadPenalty(precise)+
            getRoomPenalty(precise)+
            getPeriodPenalty(precise)+
            getBinaryViolationWeight()*getBinaryViolations(precise)+
            getDirectConflictWeight()*getNrDirectConflicts(precise);
    }
    

    /**
     * CSV header (names of problem specific parameters):
     * bv for binary violations,
     * dc for direct conflicts,
     * 2r for two exams in a row penalty,
     * 2d for two exams in a day penalty,
     * ps for period spread penalty,
     * mx for mixed durations penalty,
     * fl for front load penalty,
     * rp for room penalty, and
     * pp for period penalty
     */
    public String csvHeader() { 
        return "bv,dc,2r,2d,ps,mx,fl,rp,pp"; 
    }
    
    /**
     * CSV line (values of problem specific parameters):
     * binary violations,
     * direct conflicts,
     * two exams in a row penalty,
     * two exams in a day penalty,
     * period spread penalty,
     * mixed durations penalty,
     * front load penalty,
     * room penalty, and
     * period penalty
     */
    public String csvLine() { 
        return 
            getBinaryViolations(false)+(getBinaryViolations(false)>0 && getBinaryViolationWeight()!=5000?"/"+getBinaryViolationWeight():"")+","+
            getNrDirectConflicts(false)+(getNrDirectConflicts(false)>0 && getDirectConflictWeight()!=5000?"/"+getDirectConflictWeight():"")+","+
            getTwoInARow(false)+","+
            getTwoInADay(false)+","+
            getWiderSpread(false)+","+
            getMixedDurations(false)+","+
            getFrontLoadPenalty(false)+","+
            getRoomPenalty(false)+","+
            getPeriodPenalty(false);
    }
    
    /**
     * Solution info -- values of problem specific parameters
     */
    public Map<String, String> getInfo() {
    	Map<String, String> info = super.getInfo();
        info.put("Two Exams in a Row",String.valueOf(getTwoInARow(false)));
        info.put("Two Exams in a Day",String.valueOf(getTwoInADay(false)));
        info.put("Wider/Period Spread",String.valueOf(getWiderSpread(false)));
        info.put("Mixed Durations",String.valueOf(getMixedDurations(false)));
        info.put("Larger Exams Constraints",String.valueOf(getFrontLoadPenalty(false)));
        info.put("Room Penalty",String.valueOf(getRoomPenalty(false)));
        info.put("Period Penalty",String.valueOf(getPeriodPenalty(false)));
        info.put("Binary Violations",String.valueOf(getBinaryViolations(false)));
        info.put("Direct Conflicts",String.valueOf(getNrDirectConflicts(false)));
        return info;
    }
    
    /**
     * Extended solution info -- values of problem specific parameters (precisely computed)
     */
    public Map<String, String> getExtendedInfo() {
    	Map<String, String> info = super.getExtendedInfo();
        info.put("Two Exams in a Row [p]",String.valueOf(getTwoInARow(true)));
        info.put("Two Exams in a Day [p]",String.valueOf(getTwoInADay(true)));
        info.put("Wider/Period Spread [p]",String.valueOf(getWiderSpread(true)));
        info.put("Mixed Durations [p]",String.valueOf(getMixedDurations(true)));
        info.put("Larger Exams Constraints [p]",String.valueOf(getFrontLoadPenalty(true)));
        info.put("Room Penalty [p]",String.valueOf(getRoomPenalty(true)));
        info.put("Period Penalty [p]",String.valueOf(getPeriodPenalty(true)));
        info.put("Binary Violations [p]",String.valueOf(getBinaryViolations(true)));
        info.put("Direct Conflicts [p]",String.valueOf(getNrDirectConflicts(true)));
        return info;
    }

    /**
     * Update counters on unassignment of an exam
     */
    public void beforeUnassigned(long iteration, ExPlacement placement) {
        super.beforeUnassigned(iteration, placement);
        ExExam exam = placement.variable();
        iFronLoadPenalty-=placement.frontLoadPenalty();
        iPeriodPenalty-=placement.getPeriodPenalty();
        iRoomPenalty-=placement.getRoom().getPenalty();
        iTwoInARowPenalty-=placement.twoInARowPenalty();
        iTwoInADayPenalty-=placement.twoInADayPenalty();
        iWiderSpreadPenalty-=placement.widerSpreadPenalty();
        iMixedDurationsPenalty-=placement.mixedDurationsPenalty();
        iBinaryViolations-=placement.nrBinaryViolations();
        iNrDirectConflicts-=placement.nrDirectConflicts();
        for (ExStudent s: exam.getStudents())
            s.afterUnassigned(iteration, placement);
        placement.getRoom().afterUnassigned(iteration, placement);
    }
    
    /**
     * Update counters on assignment of an exam
     */
    public void afterAssigned(long iteration, ExPlacement placement) {
        super.afterAssigned(iteration, placement);
        ExExam exam = placement.variable();
        iFronLoadPenalty+=placement.frontLoadPenalty();
        iPeriodPenalty+=placement.getPeriodPenalty();
        iRoomPenalty+=placement.getRoom().getPenalty();
        iTwoInARowPenalty+=placement.twoInARowPenalty();
        iTwoInADayPenalty+=placement.twoInADayPenalty();
        iWiderSpreadPenalty+=placement.widerSpreadPenalty();
        iMixedDurationsPenalty+=placement.mixedDurationsPenalty();
        iBinaryViolations+=placement.nrBinaryViolations();
        iNrDirectConflicts+=placement.nrDirectConflicts();
        for (ExStudent s: exam.getStudents())
        	s.afterAssigned(iteration, placement);
        placement.getRoom().afterAssigned(iteration, placement);
    }
    
    /**
     * Report violations of all soft constraints (optimization criteria)
     * @param out output writer to print violations
     */
    public void report(PrintWriter out) {
        List<ExExam> orderedExams = new ArrayList<ExExam>(variables());
        Collections.sort(orderedExams, new Comparator<ExExam>() {
            public int compare(ExExam e1, ExExam e2) {
                int cmp = -Double.compare(e1.getStudents().size(), e2.getStudents().size());
                if (cmp!=0) return cmp;
                return Double.compare(e1.getId(), e2.getId());
            }
        });
        orderedExams.addAll(variables());
        out.println("Front load threshold is "+iFronLoadThreshold+" ("+getPeriod(iFronLoadThreshold)+")");
        int notScheduled = 0, roomPenaltyCnt = 0, roomPenalty = 0, periodPenaltyCnt = 0, periodPenalty = 0, twoInRow = 0, 
            twoInDay = 0, twoInRowCnt = 0, twoInDayCnt = 0, widerSpreadCnt = 0, widerSpread = 0, frontLoad = 0; 
        for (ExExam exam: variables()) {
            ExPlacement placement = (ExPlacement)exam.getAssignment();
            if (placement==null) {
                out.println("Exam "+exam.getId()+": not scheduled");
                notScheduled++;
            } else {
                out.println("Exam "+exam.getId()+": "+placement.getPeriod()+" room "+placement.getRoom().getId()+" (length "+exam.getLength()+", students "+exam.getStudents().size()+(exam.isLargest()?", "+(1+iLargestExams.indexOf(exam))+". largest":"")+")");
                if (placement.getRoom().getPenalty()>0)  {
                    out.println("Exam "+exam.getId()+": Room penalty Violation - Penalty "+placement.getRoom().getPenalty());
                    roomPenaltyCnt++;
                    roomPenalty+=placement.getRoom().getPenalty();
                }
                if (placement.getPeriod().getWeight()>0) {
                    out.println("Exam "+exam.getId()+": Period penalty Violation - Penalty "+placement.getPeriod().getWeight());
                    periodPenaltyCnt++;
                    periodPenalty+=placement.getPeriod().getWeight();
                }
                if (exam.isLargest() && placement.getPeriodIndex()>=getFronLoadThreshold()) {
                    out.println("Exam "+exam.getId()+": Front load violation - Penalty "+iFrontLoad[2]);
                    frontLoad++;
                }
                int twoInRowThisExam = 0, twoInDayThisExam = 0, widerSpreadThisExam = 0;
                for (ExStudent s: exam.getStudents()) {
                    for (ExPeriod p=firstPeriod(); p!=null; p=p.next()) {
                        if (!s.hasExam(p)) continue;
                        if (p.getIndex()<=placement.getPeriod().getIndex()) continue;
                        int dist = Math.abs(p.getIndex()-placement.getPeriodIndex());
                        if (p.getDay()==placement.getPeriod().getDay()) {
                            boolean adj = (p.getIndex()+1==placement.getPeriod().getIndex() || p.getIndex()-1==placement.getPeriod().getIndex());
                            if (adj) twoInRowThisExam++; else twoInDayThisExam++;
                            out.println("Exam "+exam.getId()+":   -- student "+s.getId()+" has another exam "+p+" (Exam "+s.getExamStr(p)+", "+(adj?"in a row":"in a day")+", distance "+dist+")");
                            /*
                            for (Enumeration g=getPeriods().getPeriods().elements();g.hasMoreElements();) {
                                ExPeriods.Period px = (ExPeriods.Period)g.nextElement();
                                ExPlacement pl = s.getPlacement(px.getDay(), px.getTime());
                                if (pl!=null)
                                    sLog.info("Exam "+exam.getId()+":    -- student "+s.getId()+" has exam "+pl.variable().getId()+" at "+px);
                            }
                            */
                        }
                        if (dist>0 && dist<=iPeriodSpread) {
                            out.println("Exam "+exam.getId()+":   -- student "+s.getId()+" has another exam "+p+" (Exam "+s.getExamStr(p)+", distance "+dist+")");
                            /*
                            for (Enumeration g=getPeriods().getPeriods().elements();g.hasMoreElements();) {
                                ExPeriods.Period px = (ExPeriods.Period)g.nextElement();
                                ExPlacement pl = s.getPlacement(px.getDay(), px.getTime());
                                if (pl!=null)
                                    sLog.info("Exam "+exam.getId()+":    -- student "+s.getId()+" has exam "+pl.variable().getId()+" at "+px);
                            }
                            */
                            widerSpreadThisExam++;
                        }
                    }
                }
                if (twoInDayThisExam>0) {
                    out.println("Exam "+exam.getId()+": TwoInADay Violation with "+twoInDayThisExam+" students - Penalty "+iTwoInADay*twoInDayThisExam);
                    twoInDay += twoInDayThisExam;
                    twoInDayCnt++;
                }
                if (twoInRowThisExam>0) {
                    out.println("Exam "+exam.getId()+": TwoInARow Violation with "+twoInRowThisExam+" students - Penalty "+iTwoInARow*twoInRowThisExam);
                    twoInRow += twoInRowThisExam;
                    twoInRowCnt++;
                }
                if (widerSpreadThisExam>0) {
                    out.println("Exam "+exam.getId()+": Wider Spread Violation with "+widerSpreadThisExam+" students - Penalty "+widerSpreadThisExam);
                    widerSpread += widerSpreadThisExam;
                    widerSpreadCnt ++;
                }
            }
        }
        int mixDurCnt = 0;
        for (ExRoom room: getRooms()) {
            for (ExPeriod p=firstPeriod(); p!=null; p=p.next()) {
                Set<Integer> durations = new HashSet<Integer>();
                StringBuffer sb = new StringBuffer();
                for (Iterator<ExPlacement> i=room.getExams(p).iterator(); i.hasNext(); ) {
                    ExPlacement placement = i.next();
                    ExExam exam = placement.variable();
                    durations.add(new Integer(exam.getLength()));
                    sb.append(exam.getId()+"/"+exam.getLength());
                    if (i.hasNext()) sb.append(", ");
                }
                if (durations.size()>1) {
                    out.println("Room "+room.getId()+" at Period "+p.getIndex()+": Mix Durations penalty Violation - Penalty "+iNonMixedDurations*(durations.size()-1)+" ("+sb+")");
                    mixDurCnt+=durations.size()-1;
                }
            }
        }
        out.println("");
        out.println("TwoInARow: "+twoInRowCnt+", pen="+iTwoInARow*twoInRow);
        out.println("TwoInADay: "+twoInDayCnt+", pen="+iTwoInADay*twoInDay);
        out.println("WiderSpreads: "+widerSpreadCnt+", pen="+widerSpread);
        out.println("PeriodPenalties: "+periodPenaltyCnt+", pen="+periodPenalty);
        out.println("RoomPenalties: "+roomPenaltyCnt+", pen="+roomPenalty);
        out.println("MixDurationPenalties: "+mixDurCnt+", pen="+iNonMixedDurations*mixDurCnt);
        out.println("FrontLoadPenalties: "+frontLoad+", pen="+iFrontLoad[2]*frontLoad);
        out.println("");
        out.println("Overall penalty = "+
                (iTwoInARow*twoInRow +
                 iTwoInADay*twoInDay +
                 widerSpread +
                 periodPenalty +
                 roomPenalty +
                 iNonMixedDurations*mixDurCnt +
                 iFrontLoad[2]*frontLoad));
    }
    
    /**
     * Set binary constraint violations weight and direct conflict weight to 5000
     */
    public void makeFeasible() {
        setBinaryViolationWeight(5000);
        setDirectConflictWeight(5000);
    }
}