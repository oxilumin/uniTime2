package ExamFeatureCalculation;

/**
 * Created by oxilumin on 5/30/2016.
 */
public class ExLocalSearchSolutionDataPoint {
    private final long iteration;
    private final int assignedVariables;
    private final int totalVariables;
    private final double score;

    public ExLocalSearchSolutionDataPoint(long iteration, int assignedVariables, int totalVariables, double score) {
        this.iteration = iteration;
        this.assignedVariables = assignedVariables;
        this.totalVariables = totalVariables;
        this.score = score;
    }

    public long getIteration() {
        return iteration;
    }

    public int getAssignedVariables() {
        return assignedVariables;
    }

    public int getTotalVariables() {
        return totalVariables;
    }

    public double getScore() {
        return score;
    }
}
