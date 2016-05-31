package ExamFeatureCalculation;

/**
 * Created by oxilumin on 5/23/2016.
 */
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.ItcModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class ExItcTest {
    private static String sProblem = null;
    private static File sInputFile = null;
    private static File sOutputFile = null;
    private static File sCSVFile = null;
    private static File sLogFile = null;
    private static long sSeed = generateSeed();
    private static long sTimeOut = -1L;
    private static DataProperties sConfig = new DataProperties();
    private static Logger sLog = Logger.getLogger(net.sf.cpsolver.itc.ItcTest.class);

    public ExItcTest() {
    }

    public static long generateSeed() {
        return Math.round(9.223372036854776E18D * Math.random());
    }

    public static void setupLogging(File var0, boolean var1, boolean var2) {
        Logger var3 = Logger.getRootLogger();
        ConsoleAppender var4 = new ConsoleAppender(new PatternLayout("%m%n"));
        var4.setThreshold(var1 ? Level.INFO : Level.WARN);
        var3.addAppender(var4);
        Logger.getLogger(JProf.class).setLevel(Level.ERROR);
        if (var1 || var2) {
            try {
                FileAppender var5 = new FileAppender(new PatternLayout("%d{dd-MMM-yy HH:mm:ss.SSS} [%t] %-5p %c{2}> %m%n"), var0.getPath(), false);
                var5.setThreshold(Level.DEBUG);
                var3.addAppender(var5);
            } catch (IOException var6) {
                sLog.fatal("Unable to configure logging, reason: " + var6.getMessage(), var6);
            }
        }

        if (!var2) {
            var3.setLevel(var1 ? Level.INFO : Level.WARN);
        }

        sLog = var3;
    }

    public static void printUsage() {
        System.out.println("Usage: java [options] -jar itc2007.jar problem input [output] [timeout] [seed]");
        System.out.println();
        System.out.println("  problem ... itc, tim, exam");
        System.out.println("  input ... input file");
        System.out.println("  output ... output file or folder");
        System.out.println();
        System.out.println("Additional options:");
        System.out.println("  -Dtimeout=X ... time to run in seconds");
        System.out.println("  -Dseed=X ... random generator seed");
        System.out.println("  -Dverbose=info ... enable info messages");
        System.out.println("  -Dverbose=debug ... enable debug messages");
        System.out.println();
        System.out.println("Examples:");
        System.out.println();
        System.out.println("Track1: (Examination Timetabling)");
        System.out.println("  java -jar itc2007.jar exam exam_comp_set1.exam exam_comp_set1.out");
        System.out.println();
        System.out.println("Track2: (Post Enrolment based Course Timetabling, time limit set to 288 seconds)");
        System.out.println("  java -Dtimeout=288 -jar itc2007.jar tim comp-2007-2-1.tim comp-2007-2-1.sol");
        System.out.println();
        System.out.println("Track3: (Curriculum based Course Timetabling, time limit set to 288 seconds, seed set to 123)");
        System.out.println("  java -Dseed=123 -Dtimeout=288 -jar itc2007.jar ctt comp01.ctt comp01.out");
    }

    public static void initLogger(String logFile) {
        sLogFile = new File(logFile);
        boolean var5 = "debug".equals(System.getProperty("verbose"));
        setupLogging(sLogFile, true, var5);
    }

    public static boolean init(String problemType, String inputFile, String outputFile, Long timeout, Long maxIters) {

        sProblem = problemType;
        sInputFile = new File(inputFile);
        if (!sInputFile.exists()) {
            System.err.println("Input file \'" + sInputFile + "\' does not exist.");
            return false;
        } else {

            sTimeOut = timeout;

            sSeed = Long.parseLong(System.getProperty("seed", String.valueOf(sSeed)));
            sTimeOut = Long.parseLong(System.getProperty("timeout", String.valueOf(sTimeOut)));
            if (ExItcTest.class.getResource("/ExamFeatureCalculation/" + sProblem + ".properties") != null) {
                try {
                    sConfig.load(ExItcTest.class.getResourceAsStream("/ExamFeatureCalculation/" + sProblem + ".properties"));
                } catch (IOException var4) {
                    System.err.println("Unable to read property file, reason: " + var4.getMessage());
                    var4.printStackTrace(System.err);
                }
            }

            sConfig.putAll(System.getProperties());
            String var1 = sConfig.getProperty("Model.Extension", "out");
            String var2;

            sOutputFile = new File(outputFile);
            if (sOutputFile.exists() && sOutputFile.isDirectory()) {
                var2 = sInputFile.getName();
                if (var2.indexOf(46) >= 0) {
                    var2 = var2.substring(0, var2.lastIndexOf(46));
                }

                sOutputFile = new File(sOutputFile, var2 + "_" + sSeed + "." + var1);
            }


            if (sOutputFile.getParentFile() != null) {
                sOutputFile.getParentFile().mkdirs();
            }

            sCSVFile = new File(sOutputFile.getParentFile(), sInputFile.getName().substring(0, sInputFile.getName().lastIndexOf(46)) + ".csv");
            ToolBox.setSeed(sSeed);
            sConfig.setProperty("General.Seed", String.valueOf(sSeed));
            sConfig.setProperty("General.Input", sInputFile.getPath());
            sConfig.setProperty("General.Output", sOutputFile.getPath());
            if (sTimeOut >= 0L) {
                sConfig.setProperty("Termination.TimeOut", String.valueOf(sTimeOut));
            } else {
                sTimeOut = sConfig.getPropertyLong("Termination.TimeOut", -1L);
            }

            if (maxIters > 0L) {
                sConfig.setProperty("Termination.MaxIters", String.valueOf(maxIters));
            }
            sLog.info("Problem: " + sProblem);
            sLog.info("Input:   " + sInputFile);
            sLog.info("Output:  " + sOutputFile);
            sLog.info("CSV:     " + sCSVFile);
            sLog.info("Log:     " + sLogFile);
            sLog.info("Seed:    " + sSeed);
            sLog.info("Timeout: " + sTimeOut);
            return true;
        }

    }

    private static Solver create(List<ExLocalSearchSolutionDataPoint> bestTimes) throws Exception {
        ItcModel var0 = (ItcModel) Class.forName(sConfig.getProperty("Model.Class")).newInstance();
        var0.setProperties(sConfig);
        if (!var0.load(sInputFile)) {
            sLog.error("Unable to load input file.");
            return null;
        } else {
            Solver var1 = new Solver(sConfig);
            Solution var2 = new Solution(var0);
            var1.setInitalSolution(var2);
            var1.currentSolution().addSolutionListener(new SolutionListener() {
                public void solutionUpdated(Solution var1) {
                }

                public void getInfo(Solution var1, Map var2) {
                }

                public void getInfo(Solution var1, Map var2, Collection var3) {
                }

                public void bestCleared(Solution var1) {
                }

                public void bestSaved(Solution var1) {
                    ItcModel var2 = (ItcModel) var1.getModel();
                    sLog.info("**BEST[" + var1.getIteration() + "]** V:" + var2.nrAssignedVariables() + "/" + var2.variables().size() + ", P:" + Math.round(var2.getTotalValue()) + " (" + var2.csvLine() + ")");

                    ExLocalSearchSolutionDataPoint dataPoint = new ExLocalSearchSolutionDataPoint(var1.getIteration(), var2.nrAssignedVariables(), var2.variables().size(), var2.getTotalValue());

                    bestTimes.add(dataPoint);
                }

                public void bestRestored(Solution var1) {
                }
            });
            return var1;
        }
    }

    public static Solution solve() {
        try {
            List<ExLocalSearchSolutionDataPoint> bestTimes = new ArrayList<>();
            Solver var0 = create(bestTimes);
            var0.start();

            try {
                var0.getSolverThread().join();
            } catch (InterruptedException var2) {
                ;
            }

            Solution var1 = var0.currentSolution();
            var1.restoreBest();
            ((ItcModel) var1.getModel()).makeFeasible();
            var1.saveBest();
            return output(var0);
        } catch (Exception var3) {
            sLog.error("Unable to solve problem, reason: " + var3.getMessage(), var3);
            return null;
        }
    }

    private static Solution output(Solver var0) throws Exception {
        Solution var1 = var0.lastSolution();
        ItcModel var2 = (ItcModel) var1.getModel();
        Progress.removeInstance(var2);
        if (var1.getBestInfo() == null) {
            sLog.error("No best solution found.");
            return null;
        } else {
            var1.restoreBest();
            sLog.info("Best solution:" + ToolBox.dict2string(var1.getExtendedInfo(), 1));
            sLog.info("Best solution found after " + var1.getBestTime() + " seconds (" + var1.getBestIteration() + " iterations).");
            sLog.info("Number of assigned variables is " + var1.getModel().assignedVariables().size());
            sLog.info("Total value of the solution is " + var1.getModel().getTotalValue());
            if (sOutputFile != null && !var2.save(sOutputFile)) {
                sLog.error("Unable to save solution.");
            }

            if (sCSVFile != null && var2.cvsPrint()) {
                boolean var3 = sCSVFile.exists();
                PrintWriter var4 = new PrintWriter(new FileWriter(sCSVFile, true));
                if (!var3) {
                    var4.println("seed,timeout,time,iter,total," + var2.csvHeader());
                }

                ItcModel var5 = (ItcModel) var1.getModel();
                DecimalFormat var6 = new DecimalFormat("0.00");
                var4.println(sSeed + "," + sTimeOut + "," + var6.format(var1.getBestTime()) + "," + var1.getBestIteration() + "," + Math.round(var5.getTotalValue() + (double) (5000 * var5.unassignedVariables().size())) + "," + var2.csvLine());
                var4.flush();
                var4.close();
            }

            return var1;
        }
    }

    public static Solution test(String var0, DataProperties var1, long var2, long var4) {
        ToolBox.setSeed(var2);
        var1.setProperty("General.Seed", String.valueOf(var2));
        var1.setProperty("General.Input", var0);
        var1.remove("General.Output");
        var1.setProperty("Termination.TimeOut", String.valueOf(var4));
        sSeed = var2;
        sTimeOut = var4;
        sConfig = var1;
        sCSVFile = null;
        sInputFile = new File(var0);
        sOutputFile = null;
        sLogFile = null;
        Solution var6 = solve();
        return var6;
    }

    public static ExLocalSearchFeatures goSolve(String problemType, String inputFile, String outputFile, Long timeout, Long maxIters) {
        try {
            if (init(problemType, inputFile, outputFile, timeout, maxIters)) {
                List<ExLocalSearchSolutionDataPoint> bestTimes = new ArrayList<>();
                Solver var1 = create(bestTimes);
                Runtime.getRuntime().addShutdownHook(new ShutdownHook(var1));
                var1.start();
                var1.getSolverThread().join();
                Double bestLocalSearchScore = var1.currentSolution().getBestValue();

                ExLocalSearchFeatures localSearchFeatures = new ExLocalSearchFeatures(bestLocalSearchScore, bestTimes);

                return localSearchFeatures;
            }

            return null;

        } catch (Exception var2) {
            sLog.error("Unable to solve problem, reason: " + var2.getMessage(), var2);

            return null;
        }
    }

    private static class ShutdownHook extends Thread {
        Solver iSolver = null;

        public ShutdownHook(Solver var1) {
            this.setName("ShutdownHook");
            this.iSolver = var1;
        }

        public void run() {
            try {
                if (this.iSolver.isRunning()) {
                    this.iSolver.stopSolver();
                }

                Solution var1 = this.iSolver.currentSolution();
                var1.restoreBest();
                ((ItcModel) var1.getModel()).makeFeasible();
                var1.saveBest();
                output(this.iSolver);
            } catch (Exception var2) {
                sLog.error("Unable to solve problem, reason: " + var2.getMessage(), var2);
            }

        }
    }
}
