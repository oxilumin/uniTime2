/**
 * Created by oxilumin on 5/23/2016.
 */
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.itc.ItcModel;
import net.sf.cpsolver.itc.heuristics.ItcUnassignedVariableSelection;
import net.sf.cpsolver.itc.heuristics.search.ItcGreatDeluge;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber;
import net.sf.cpsolver.itc.heuristics.search.ItcTabuSearch;
import org.apache.log4j.Logger;

public class ExItcNeighbourSelection<V extends Variable<V, T>, T extends Value<V, T>> extends StandardNeighbourSelection<V, T> {
    private static Logger sLog = Logger.getLogger(ExItcNeighbourSelection.class);
    private int iPhase = 0;
    private NeighbourSelection<V, T> iConstruct;
    private NeighbourSelection<V, T> iFirst;
    private NeighbourSelection<V, T> iSecond;
    private NeighbourSelection<V, T> iThird;

    public ExItcNeighbourSelection(DataProperties var1) throws Exception {
        super(var1);
        double var2 = var1.getPropertyDouble("Itc.Construction.ValueWeight", 0.0D);
        this.iConstruct = (NeighbourSelection)Class.forName(var1.getProperty("Itc.Construction", StandardNeighbourSelection.class.getName())).getConstructor(new Class[]{DataProperties.class}).newInstance(new Object[]{var1});
        if(this.iConstruct instanceof StandardNeighbourSelection) {
            StandardNeighbourSelection var4 = (StandardNeighbourSelection)this.iConstruct;
            var4.setValueSelection((ValueSelection)Class.forName(var1.getProperty("Itc.ConstructionValue", ItcTabuSearch.class.getName())).getConstructor(new Class[]{DataProperties.class}).newInstance(new Object[]{var1}));
            var4.setVariableSelection((VariableSelection)Class.forName(var1.getProperty("Itc.ConstructionVariable", ItcUnassignedVariableSelection.class.getName())).getConstructor(new Class[]{DataProperties.class}).newInstance(new Object[]{var1}));

            try {
                var4.getValueSelection().getClass().getMethod("setValueWeight", new Class[]{Double.TYPE}).invoke(var4.getValueSelection(), new Object[]{new Double(var2)});
            } catch (NoSuchMethodException var7) {
                ;
            }
        }

        try {
            this.iConstruct.getClass().getMethod("setValueWeight", new Class[]{Double.TYPE}).invoke(this.iConstruct, new Object[]{new Double(var2)});
        } catch (NoSuchMethodException var6) {
            ;
        }

        this.iFirst = (NeighbourSelection)Class.forName(var1.getProperty("Itc.First", ItcHillClimber.class.getName())).getConstructor(new Class[]{DataProperties.class}).newInstance(new Object[]{var1});
        this.iSecond = (NeighbourSelection)Class.forName(var1.getProperty("Itc.Second", ItcGreatDeluge.class.getName())).getConstructor(new Class[]{DataProperties.class}).newInstance(new Object[]{var1});
        if(var1.getProperty("Itc.Third") != null) {
            this.iThird = (NeighbourSelection)Class.forName(var1.getProperty("Itc.Third")).getConstructor(new Class[]{DataProperties.class}).newInstance(new Object[]{var1});
        }

    }

    public void init(Solver<V, T> var1) {
        super.init(var1);
        this.iConstruct.init(var1);
        this.iFirst.init(var1);
        this.iSecond.init(var1);
        if(this.iThird != null) {
            this.iThird.init(var1);
        }

    }

    protected void incPhase(Solution<V, T> var1, String var2) {
        ++this.iPhase;
        if(sLog.isInfoEnabled()) {
            ItcModel var3 = (ItcModel)var1.getModel();
            sLog.info("**CURR[" + var1.getIteration() + "]** P:" + Math.round(var3.getTotalValue()) + " (" + var3.csvLine() + ")");
            sLog.info("Phase " + var2);
        }

    }

    public Neighbour<V, T> selectNeighbour(Solution<V, T> var1) {
        Neighbour var2 = null;
        switch(this.iPhase) {
            case 0:
                var2 = this.iConstruct.selectNeighbour(var1);
                if(var2 != null) {
                    return var2;
                } else {
                    this.incPhase(var1, "first");
                }
            case 1:
                var2 = this.iFirst.selectNeighbour(var1);
               return var2;
            default:
                this.iPhase = 1;
                return this.selectNeighbour(var1);
        }
    }
}

