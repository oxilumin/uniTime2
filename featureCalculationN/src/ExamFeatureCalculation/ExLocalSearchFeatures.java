package ExamFeatureCalculation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by oxilumin on 5/30/2016.
 */
public class ExLocalSearchFeatures {

    private final Double bestLocalSearchScore;
    private ExLocalSearchSolutionDataPoint minScoreElement;
    private ExLocalSearchSolutionDataPoint maxScoreElement;
    private ExLocalSearchSolutionDataPoint medianScoreElement;

    public ExLocalSearchFeatures(Double bestLocalSearchScore, List<ExLocalSearchSolutionDataPoint> bestTimes) {

        this.bestLocalSearchScore = bestLocalSearchScore;

        Comparator comparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                if (o1 instanceof ExLocalSearchSolutionDataPoint && o2 instanceof ExLocalSearchSolutionDataPoint) {
                    ExLocalSearchSolutionDataPoint left = (ExLocalSearchSolutionDataPoint) o1;
                    ExLocalSearchSolutionDataPoint right = (ExLocalSearchSolutionDataPoint) o2;
                    Double leftScore = left.getScore();
                    Double rightScore = right.getScore();

                    return Double.compare(leftScore, rightScore);
                }

                return 0;
            }
        };

        bestTimes.sort(comparator);

        if (bestTimes.size() > 0) {
            minScoreElement = bestTimes.get(0);
            maxScoreElement = bestTimes.get(bestTimes.size() - 1);
            medianScoreElement = bestTimes.get(bestTimes.size() / 2);
        } else {
            minScoreElement = new ExLocalSearchSolutionDataPoint(0, 0, 0, 0);
            maxScoreElement = new ExLocalSearchSolutionDataPoint(0, 0, 0, 0);
            medianScoreElement = new ExLocalSearchSolutionDataPoint(0, 0, 0, 0);
        }
    }

    public Double getBestLocalSearchScore() {
        return bestLocalSearchScore;
    }

    public ExLocalSearchSolutionDataPoint getMinScoreElement() {
        return minScoreElement;
    }

    public ExLocalSearchSolutionDataPoint getMaxScoreElement() {
        return maxScoreElement;
    }

    public ExLocalSearchSolutionDataPoint getMedianScoreElement() {
        return medianScoreElement;
    }
}
