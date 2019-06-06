/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mymicrorts;

import ai.RandomBiasedAI;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRushPlusPlus;
import ai.abstraction.cRush.CRush_V2;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.portfolio.portfoliogreedysearch.UnitScript;
import ai.portfolio.portfoliogreedysearch.UnitScriptAttack;
import ai.portfolio.portfoliogreedysearch.UnitScriptBuild;
import ai.portfolio.portfoliogreedysearch.UnitScriptHarvest;
import ai.portfolio.portfoliogreedysearch.UnitScriptIdle;
import ai.portfolio.portfoliogreedysearch.UnitScriptTrain;
import ai.portfolio.portfoliogreedysearch.UnitScriptsAI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.UnitActionAssignment;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

/**
 *
 * @author santi
 *
 * This class implements "Portfolio Greedy Search", as presented by Churchill and Buro in the paper:
 * "Portfolio Greedy Search and Simulation for Large-Scale Combat in StarCraft"
 *
 * Moreover, their original paper focused purely on combat, and thus their portfolio was very samll.
 * Here:
 * - getSeedPlayer does not make sense in general, since each unit type might have a different set of scripts, so it's ignored
 * - the portfolios might be very large, since we have to include scripts for training, building, harvesting, etc.
 * - new units might be created, so a script is selected as the "default" for those new units before hand
 *
 */
public class Simulate extends AIWithComputationBudget {

    public static int DEBUG = 0;

    int LOOKAHEAD = 200;
    int I = 1;  // number of iterations for improving a given player
    int R = 1;  // number of times to improve with respect to the response fo the other player
    EvaluationFunction evaluation = null;
    HashMap<UnitType, List<UnitScript>> scripts = null;
    UnitTypeTable utt;
    PathFinding pf;

    UnitScript defaultScript = null;

    long start_time = 0;
    int nplayouts = 0;
    int currentTime=0;          //每隔一段时间更换策略

    
    public Simulate(UnitTypeTable utt) {
        this(100, -1, 100, 1, 1, 
             new SimpleSqrtEvaluationFunction3(),
             utt,
             new AStarPathFinding());
    }
    
    
    public Simulate(int time, int max_playouts, int la, int a_I, int a_R, EvaluationFunction e, UnitTypeTable a_utt, PathFinding a_pf) {
        super(time, max_playouts);
        LOOKAHEAD = la;
        I = a_I;
        R = a_R;
        evaluation = e;
        utt = a_utt;
        pf = a_pf;

        UnitScript harvest = new UnitScriptHarvest(pf,utt);
        UnitScript buildBarracks = new UnitScriptBuild(pf,utt.getUnitType("Barracks"));
        UnitScript buildBase = new UnitScriptBuild(pf,utt.getUnitType("Base"));
        UnitScript attack = new UnitScriptAttack(pf);
        UnitScript idle = new UnitScriptIdle();
        UnitScript trainWorker = new UnitScriptTrain(utt.getUnitType("Worker"));
        UnitScript trainLight = new UnitScriptTrain(utt.getUnitType("Light"));
        UnitScript trainHeavy = new UnitScriptTrain(utt.getUnitType("Heavy"));
        UnitScript trainRanged = new UnitScriptTrain(utt.getUnitType("Ranged"));

        defaultScript = idle;
        scripts = new HashMap<>();
        {
            List<UnitScript> l = new ArrayList<>();
            l.add(harvest);
            l.add(buildBarracks);
            l.add(buildBase);
            l.add(attack);
            l.add(idle);
            scripts.put(utt.getUnitType("Worker"),l);
        }
        {
            List<UnitScript> l = new ArrayList<>();
            scripts.put(utt.getUnitType("Base"),l);
            l.add(trainWorker);
            l.add(idle);
        }
        {
            List<UnitScript> l = new ArrayList<>();
            scripts.put(utt.getUnitType("Barracks"),l);
            l.add(trainLight);
            l.add(trainHeavy);
            l.add(trainRanged);
            l.add(idle);
        }
        {
            List<UnitScript> l = new ArrayList<>();
            scripts.put(utt.getUnitType("Light"),l);
            l.add(attack);
            l.add(idle);
        }
        {
            List<UnitScript> l = new ArrayList<>();
            scripts.put(utt.getUnitType("Heavy"),l);
            l.add(attack);
            l.add(idle);
        }
        {
            List<UnitScript> l = new ArrayList<>();
            scripts.put(utt.getUnitType("Ranged"),l);
            l.add(attack);
            l.add(idle);
        }
    }


    public void reset() {
    }


    public PlayerAction getAction(int player, GameState gs) throws Exception {
        if (gs.winner()!=-1) return new PlayerAction();
        if (!gs.canExecuteAnyAction(player)) return new PlayerAction();

        if (DEBUG>=1) System.out.println("PGSAI " + player + "(MAX_TIME = " + TIME_BUDGET +", I: " + I + ", R: " + R + ")");
        
        List<Unit> playerUnits = new ArrayList<>();
        List<Unit> enemyUnits = new ArrayList<>();
        PlayerAction pa = new PlayerAction();
                
        //控制每隔15个T模拟一次 至少这样能完整的跑下来......
        currentTime++;
        AI temp =null;
        /*
        if(currentTime%10!=0)
        {
            temp = new test(utt);
            return pa=temp.getAction(player, gs);
        }
        */
        for(Unit u:gs.getUnits()) {
            if (u.getPlayer()==player) playerUnits.add(u);
            else if (u.getPlayer()>=0) enemyUnits.add(u);
        }
        int n1 = playerUnits.size();
        int n2 = enemyUnits.size();

        UnitScript playerScripts[] = new UnitScript[n1];
        UnitScript enemyScripts[] = new UnitScript[n2];

        // Init the players:
        // 改动应该是改这里！！！替换成自己的备选策略就行
        for(int i = 0;i<n1;i++) playerScripts[i] = defaultScript(playerUnits.get(i), gs);
        for(int i = 0;i<n2;i++) enemyScripts[i] = defaultScript(enemyUnits.get(i), gs);

        // Note: here, the original algorithm does "getSeedPlayer", which only makes sense if the same scripts can be used for all the units

        start_time = System.currentTimeMillis();
        nplayouts = 0;
        AI best=null;
        int strategy = improve3(player, playerScripts, playerUnits, enemyScripts, enemyUnits, gs);
        switch(strategy)
        {
            case 0:best=new test(utt);break;
            //case 0:best=new WorkerRush(utt);break;
            //case 0:best=new FirstRush(utt);break;
            //case 1:best=new MyNewAI(utt,new BFSPathFinding());break;
            //case 1:best=new RangedRush(utt);break;
            case 1:best=new FirstRush(utt);break;
            case 2:best=new AllDefence(utt);;break;
            default:best=new test(utt);break;
        }
        //System.out.println(gs.getTime());
        if(best==null)
        {
            System.out.println("错误11111111111！！！！！");
            best = new test(utt);
        }
        /*
        //类似于提前看几步，反正这里R设置的是1
        for(int r = 0;r<R;r++) {
            improve3(1-player, enemyScripts, enemyUnits, playerScripts, playerUnits, gs);
            improve3(player, playerScripts, playerUnits, enemyScripts, enemyUnits, gs);
        }*/
        // generate the final Player Action:
        pa=best.getAction(player, gs);
        /*
        for(int i = 0;i<n1;i++) {
            Unit u = playerUnits.get(i);
            if (gs.getUnitAction(u)==null) {
                UnitScript s = playerScripts[i].instantiate(u, gs);
                if (s!=null) {
                    UnitAction ua = s.getAction(u, gs);
                    if (ua!=null) {
                        pa.addUnitAction(u, ua);
                    } else {
                        pa.addUnitAction(u, new UnitAction(UnitAction.TYPE_NONE));
                    }
                } else {
                    pa.addUnitAction(u, new UnitAction(UnitAction.TYPE_NONE));                
                }
            }
        }
        */
        return pa;
    }


    public UnitScript defaultScript(Unit u, GameState gs) {
        // the first script added per type is considered the default:
        List<UnitScript> l = scripts.get(u.getType());
        return l.get(0).instantiate(u, gs);
    }


    public void improve(int player,
                        UnitScript scriptsToImprove[], List<Unit> units,
                        UnitScript otherScripts[], List<Unit> otherUnits, GameState gs) throws Exception {
        // scriptsToImprove对应playerScripts,units对应playerUnits

        List<AI> cand = null;
        AI temp1 = new test(utt);
        cand.add(temp1);
        AI temp2 = new WorkerRushPlusPlus(utt);
        cand.add(temp2);
        AI temp3 = new CRush_V2(utt);
        cand.add(temp3);
        
        for(int i = 0;i<I;i++) {
            if (DEBUG>=1) System.out.println("Improve player " + player + "(" + i + "/" + I + ")");
            for(int u = 0;u<scriptsToImprove.length;u++) {
                if (ITERATIONS_BUDGET>0 && nplayouts>=ITERATIONS_BUDGET) {
                    if (DEBUG>=1) System.out.println("nplayouts>=MAX_PLAYOUTS");
                    return;
                }
                if (TIME_BUDGET>0 && System.currentTimeMillis()>=start_time+TIME_BUDGET) {
                    if (DEBUG>=1) System.out.println("Time out!");
                    return;
                }

                Unit unit = units.get(u);
                double bestEvaluation = 0;
                UnitScript bestScript = null;
                List<UnitScript> candidates = scripts.get(unit.getType());
                for(UnitScript us:candidates) {
                    UnitScript s = us.instantiate(unit, gs);
                    if (s!=null) {
                        if (DEBUG>=2) System.out.println("  " + unit + " -> " + s.getClass().toString());
                        scriptsToImprove[u] = s;
                        double e = playout(player, scriptsToImprove, units, otherScripts, otherUnits, gs);
                        
                        if (bestScript==null || e>bestEvaluation) {
                            bestScript = us;
                            bestEvaluation = e;
                            if (DEBUG>=2) System.out.println("    new best: " + e);
                        }
                    }
                }
                scriptsToImprove[u] = bestScript;
            }
        }
    }
        
    public int improve3(int player,
                        UnitScript scriptsToImprove[], List<Unit> units,
                        UnitScript otherScripts[], List<Unit> otherUnits, GameState gs) throws Exception {
        // scriptsToImprove对应playerScripts,units对应playerUnits

        List<AI> cand = new ArrayList<>();
        AI temp1 = new test(utt);
        //AI temp1 = new WorkerRush(utt);
        cand.add(temp1);
        AI temp2 = new FirstRush(utt);
        //AI temp2 = new MyNewAI(utt,new BFSPathFinding());
        cand.add(temp2);
        AI temp3 = new AllDefence(utt);
        cand.add(temp3);
        AI bestAI=new test(utt);
        int strategy=0;
        for(int i = 0;i<I;i++) {
            if (DEBUG>=1) System.out.println("Improve player " + player + "(" + i + "/" + I + ")");
            for(int u = 0;u<scriptsToImprove.length;u++) {
                if (ITERATIONS_BUDGET>0 && nplayouts>=ITERATIONS_BUDGET) {
                    if (DEBUG>=1) System.out.println("nplayouts>=MAX_PLAYOUTS");
                    return 0;
                }
                if (TIME_BUDGET>0 && System.currentTimeMillis()>=start_time+TIME_BUDGET) {
                    if (DEBUG>=1) System.out.println("Time out!");
                    return 0;
                }
                
                //Unit unit = units.get(u);
                double bestEvaluation = 0;
                UnitScript bestScript = null;
                //List<UnitScript> candidates = scripts.get(unit.getType());
                int num=0;
                for(AI p:cand) {
                    if (p!=null) {
                        double e = playout3(player, p, units, otherScripts, otherUnits, gs);                      
                        if (bestAI==null || e>bestEvaluation) {
                            bestAI = p;
                            bestEvaluation = e;
                            {
                                if(num==0)
                                {
                                    strategy=0;
                                    System.out.println("    new best: " + "Test:" + e);
                                }
                                if(num==1)
                                {
                                    strategy=1;
                                    System.out.println("    new best: " + "FirstRush:" + e);
                                }
                                if(num==2)
                                {
                                    strategy=2;
                                    System.out.println("    new best: " + "AllDefence:" + e);
                                }
                            }   
                        }
                        num++;
                    }
                }               
                scriptsToImprove[u] = bestScript;
            }
        }
        return strategy;
    }
    
    public AI improve2(int player,
                        AI p1, List<Unit> units,
                        AI p2, List<Unit> otherUnits, GameState gs) throws Exception {
        List<AI> cand = null;
        AI temp1 = new CRush_V2(utt);
        cand.add(temp1);
        AI temp2 = new WorkerRushPlusPlus(utt);
        cand.add(temp2);
        AI temp3 = new test(utt);
        cand.add(temp3);
        AI bestAI=null;
        double bestEvaluation = 0;
        int i=0;
        for(AI us:cand) {
            double e = playout2(player, us, units, p2, otherUnits, gs);
            if (bestAI==null || e>bestEvaluation) {
                bestAI = us;
                bestEvaluation = e;
                if(i==0)
                    System.out.println("    new best: " + "CRush_V2:" + e);
                if(i==1)
                    System.out.println("    new best: " + "WorkerRushRush:" + e);
                if(i==2)
                    System.out.println("    new best: " + "Test:" + e);
            }
            i++;
        }
        return bestAI;
    }
        
    public double playout3(int player,
                          AI p1, List<Unit> units1,
                          UnitScript scripts2[], List<Unit> units2, GameState gs) throws Exception {
//        if (DEBUG>=1) System.out.println("  playout... " + LOOKAHEAD);
        nplayouts++;



        GameState gs2 = gs.clone();
        GameState gs3 = gs.clone();

        
        int timeLimit = gs2.getTime() + LOOKAHEAD;
        boolean gameover = false;
        int temp=gs2.getTime();
        while(!gameover && gs2.getTime()<timeLimit) {
            //System.out.println("内层"+gs2.getTime());
            //System.out.println("外层"+gs.getTime());
            //temp++;System.out.println("时差"+temp);
            if (gs2.isComplete()) 
            {
                gameover = gs2.cycle();
            } else 
            {
                for(int i=0;i<1000;i++)
                {
                    gs2.cycle();
                }
                AI ai1 = p1;
                AI ai2 = new UnitScriptsAI(scripts2, units2, scripts, defaultScript);
                ai1.reset();
                ai2.reset();
                //gs2.setTime(gs);
                gs2.issue(ai1.getAction(player, gs2));
                gs2.issue(ai2.getAction(1-player, gs2));
            }
        }        
        double e = evaluation.evaluate(player, 1-player, gs2);
//        if (DEBUG>=1) System.out.println("  done: " + e);
        return e;
    }
    
    public double playout2(int player,
                          AI p1, List<Unit> units1,
                          AI p2, List<Unit> units2, GameState gs) throws Exception {
//        if (DEBUG>=1) System.out.println("  playout... " + LOOKAHEAD);
        nplayouts++;

        AI ai1 = p1;
        AI ai2 = p2;

        GameState gs2 = gs.clone();
        ai1.reset();
        ai2.reset();
        int timeLimit = gs2.getTime() + LOOKAHEAD;
        boolean gameover = false;
        while(!gameover && gs2.getTime()<timeLimit) {
            if (gs2.isComplete()) {
                gameover = gs2.cycle();
            } else {
                gs2.issue(ai1.getAction(player, gs2));
                gs2.issue(ai2.getAction(1-player, gs2));
            }
        }        
        double e = evaluation.evaluate(player, 1-player, gs2);
//        if (DEBUG>=1) System.out.println("  done: " + e);
        return e;
    }
    
    public double playout(int player,
                          UnitScript scripts1[], List<Unit> units1,
                          UnitScript scripts2[], List<Unit> units2, GameState gs) throws Exception {
//        if (DEBUG>=1) System.out.println("  playout... " + LOOKAHEAD);
        nplayouts++;

        AI ai1 = new UnitScriptsAI(scripts1, units1, scripts, defaultScript);
        AI ai2 = new UnitScriptsAI(scripts2, units2, scripts, defaultScript);

        GameState gs2 = gs.clone();
        ai1.reset();
        ai2.reset();
        int timeLimit = gs2.getTime() + LOOKAHEAD;
        boolean gameover = false;
        while(!gameover && gs2.getTime()<timeLimit) {
            if (gs2.isComplete()) {
                gameover = gs2.cycle();
            } else {
                gs2.issue(ai1.getAction(player, gs2));
                gs2.issue(ai2.getAction(1-player, gs2));
            }
        }        
        double e = evaluation.evaluate(player, 1-player, gs2);
//        if (DEBUG>=1) System.out.println("  done: " + e);
        return e;
    }

    @Override
    public AI clone() {
        return new Simulate(TIME_BUDGET, ITERATIONS_BUDGET, LOOKAHEAD, I, R, evaluation, utt, pf);
    }
    
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + TIME_BUDGET + ", " + ITERATIONS_BUDGET + ", " + LOOKAHEAD + ", " + I + ", " + R + ", " + evaluation + ", " + pf + ")";
    }
    
    
    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("TimeBudget",int.class,100));
        parameters.add(new ParameterSpecification("IterationsBudget",int.class,-1));
        parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        parameters.add(new ParameterSpecification("I", int.class, 1));
        parameters.add(new ParameterSpecification("R", int.class, 1));
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction3()));
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
        
        return parameters;
    }    
    
    
    public int getPlayoutLookahead() {
        return LOOKAHEAD;
    }
    
    
    public void setPlayoutLookahead(int a_pola) {
        LOOKAHEAD = a_pola;
    }

    
    public int getI() {
        return I;
    }
    
    
    public void setI(int a) {
        I = a;
    }
    
    
    public int getR() {
        return R;
    }
    
    
    public void setR(int a) {
        R = a;
    }
       
    
    public EvaluationFunction getEvaluationFunction() {
        return evaluation;
    }
    
    
    public void setEvaluationFunction(EvaluationFunction a_ef) {
        evaluation = a_ef;
    }        
        
    
    public PathFinding getPathFinding() {
        return pf;
    }
    
    
    public void setPathFinding(PathFinding a_pf) {
        pf = a_pf;
    }    
}
