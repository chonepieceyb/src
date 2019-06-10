/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package QLAi;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Attack;
import ai.abstraction.Harvest;
import ai.abstraction.LightRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import static java.lang.Integer.min;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import rts.units.Unit;
import rts.units.UnitType;
import QLAi.Qlearning;
/**
 *
 * @author msi-
 */
public class QLAi extends AbstractionLayerAI{
    //直接把Q矩阵写在代码里，因为不知道怎么弄路径
     double QMatrixV2[][] = {{.00,.00,-6.14,.00,.00,.00,.00,.00,.00,.00,.00,-1156.25,-511.56,.00,-500.00,.00,.00,-500.00,.00,.00,.00,.00,-44.81,.00,.00,8.48,.00,.00,.00,.00,.00,-795.81,-67.80,.00,-500.00,-97.04,.00,-760.72,.00,.00,.00,-24.93,-31.47,.00,49.59,-103.99,.00,.00,17.18,.00,.00,-614.36,-72.42,.00,-205.07,-527.66,.00,-473.58,.00,.00,.00,226.77,.00,.00,-101.70,.00,.00,.00,.00,.00,.00,42.54,.00,.00,.00,.00,.00,175.66,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-8.14,.00,.00,.00,.00,.00,},
{.00,.00,-43.76,.00,.00,.00,.00,.00,.00,.00,.00,-1156.25,-124.78,.00,.00,.00,.00,-500.00,.00,.00,.00,.00,-49.12,.00,.00,-56.40,.00,.00,.00,.00,.00,42.68,-118.20,.00,-80.37,-119.90,.00,-698.33,.00,.00,.00,-15.59,-77.61,.00,.00,-21.00,.00,.00,.00,.00,.00,493.52,8.03,.00,-23.84,-528.31,.00,-497.00,.00,.00,.00,-15.70,.00,.00,-108.22,.00,.00,.00,.00,.00,.00,-82.90,.00,.00,.00,.00,.00,-14.16,.00,.00,.00,.00,.00,.00,587.46,.00,.00,.00,.00,.00,.00,.00,.00,.00,-19.14,.00,.00,.00,.00,.00,},
{.00,.00,-45.26,.00,.00,.00,.00,.00,.00,.00,.00,-1156.25,-490.99,.00,.00,.00,.00,-718.75,.00,.00,.00,.00,-44.38,.00,.00,-49.49,.00,.00,.00,.00,.00,-279.13,-67.61,.00,-518.71,-111.34,.00,-932.15,.00,.00,.00,185.57,901.90,.00,.00,-125.35,.00,.00,.00,.00,.00,-418.32,-82.56,403.10,-132.87,-571.49,.00,-478.68,.00,.00,.00,-8.85,1131.72,.00,-113.41,.00,.00,.00,.00,.00,.00,-127.96,610.86,.00,.00,.00,.00,-4.80,.00,.00,.00,.00,1.75,.00,.00,.00,.00,.00,.00,.00,.00,-72.98,.00,.00,-98.45,.00,.00,.00,.00,.00,},
{.00,.00,-72.00,.00,.00,73.43,.00,.00,.00,.00,.00,-1156.25,-94.25,.00,.00,147.38,.00,-500.00,.00,.00,39.13,.00,-213.06,.00,.00,-83.72,.00,.00,.00,.00,.00,-58.86,230.40,.00,87.84,-129.33,.00,-659.00,.00,.00,.00,-36.26,-46.43,.00,.00,-81.50,.00,166.60,.00,.00,.00,-510.29,-73.23,.00,-544.52,84.66,.00,-252.72,65.55,.00,.00,-9.64,.00,.00,1119.08,.00,.00,.00,.00,.00,.00,-72.03,.00,.00,.00,.00,.00,-13.51,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-22.33,.00,.00,.00,.00,.00,.00,.00,.00,},
{.00,.00,-58.75,.00,.00,.00,.00,.00,.00,.00,.00,-1156.25,-97.50,.00,.00,.00,.00,-718.75,.00,.00,.00,.00,-51.04,.00,.00,-22.45,.00,.00,.00,.00,.00,-465.44,-41.88,.00,-134.03,-25.77,.00,-701.90,.00,.00,.00,-55.69,-50.88,.00,-3.95,-11.10,.00,.00,.00,.00,.00,-639.09,-73.85,.00,-537.99,-623.38,.00,-494.57,.00,.00,.00,-14.43,-3.62,.00,-133.87,.00,.00,.00,.00,.00,.00,-70.92,.00,.00,84.66,.00,.00,-40.39,.00,.00,.00,14.31,.00,.00,.00,.00,.00,.00,.00,.00,.00,-83.94,.00,.00,-49.51,.00,.00,.00,.00,.00,},
{.00,.00,-108.40,.00,.00,.00,.00,.00,.00,.00,.00,-1078.12,3.88,.00,-500.00,.00,.00,-500.00,.00,.00,.00,.00,-202.00,.00,.00,-70.41,.00,.00,.00,.00,.00,-17.34,-43.96,.00,-154.55,-489.75,.00,-691.46,.00,.00,.00,-89.99,-26.63,.00,.00,-17.21,.00,-43.34,.00,.00,.00,-623.50,-70.79,.00,-500.00,-745.23,.00,37.04,.00,.00,.00,-9.56,.00,.00,-98.76,.00,.00,.00,.00,.00,.00,-256.53,-29.82,.00,.00,.00,.00,-16.71,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-15.26,.00,.00,.00,.00,.00,},
{.00,.00,-126.50,.00,.00,.00,.00,.00,.00,.00,.00,-1156.25,-115.88,.00,.00,.00,.00,-500.00,.00,.00,.00,.00,-58.56,.00,.00,-73.00,.00,.00,.00,.00,.00,-20.69,-183.21,.00,-132.68,-138.25,.00,-699.27,.00,.00,.00,-18.75,-46.06,.00,-5.07,-16.50,.00,.00,.00,.00,.00,-597.43,-101.11,.00,-491.78,-528.75,.00,-502.38,.00,.00,.00,-25.95,.00,666.79,-118.72,.00,.00,.00,.00,.00,.00,-73.15,.00,.00,.00,.00,.00,-31.00,.00,.00,.00,-11.14,-13.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,3.36,.00,.00,.00,.00,.00,},
{.00,.00,-60.05,.00,.00,.00,.00,.00,.00,.00,.00,-1156.25,-93.38,.00,-500.00,.00,.00,.00,.00,.00,.00,.00,616.92,.00,.00,-61.50,.00,.00,.00,.00,.00,-660.47,-56.43,.00,-130.07,-485.96,.00,-564.27,.00,.00,.00,-52.37,-34.43,.00,.00,-62.68,.00,.00,-13.00,.00,.00,-614.04,-88.25,.00,-123.00,-540.00,.00,-808.13,.00,.00,.00,-27.38,-73.00,.00,-92.79,.00,.00,-95.36,.00,.00,.00,-103.66,-48.50,.00,.00,.00,.00,-49.60,.00,.00,.00,-10.64,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-45.08,.00,.00,.00,.00,.00,}
    };
    double QMatrixV3[][] ={{.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,35.92,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-446.17,.00,.00,-95.23,.00,.00,-23.36,.00,.00,.00,382.93,.00,.00,.00,.00,.00,127.98,.00,.00,.00,-75.14,314.16,.00,.00,.00,.00,.00,.00,.00,.00,47.49,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,},
{.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-51.69,.00,.00,.00,.00,.00,.00,.00,.00,.00,-8.53,.00,.00,.00,.00,.00,.00,.00,.00,.00,147.49,.00,.00,-49.65,-5.83,.00,150.21,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-42.67,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,},
{.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-33.19,.00,.00,-79.00,-15.17,.00,-67.79,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-71.61,.00,.00,127.81,.00,.00,.00,.00,.00,.00,.00,.00,.00,738.56,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,},
{.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-496.16,.00,.00,.00,-31.40,.00,.00,.00,.00,.00,77.93,.00,.00,.00,-43.75,.00,.00,-81.87,.00,.00,-19.75,.00,.00,.00,-22.12,.00,.00,.00,.00,.00,.00,.00,.00,.00,-85.64,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,},
{.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-27.12,.00,.00,.00,.00,.00,56.44,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-33.81,.00,.00,-43.18,.00,.00,-27.00,.00,.00,.00,.00,334.34,.00,248.84,.00,.00,.00,.00,.00,.00,-46.91,.00,166.76,.00,.00,.00,75.19,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,},
{.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,70.82,.00,.00,.00,.00,.00,-500.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-2.28,.00,.00,.00,-98.92,.00,.00,39.01,91.63,.00,-24.18,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-45.01,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,147.10,.00,.00,.00,.00,.00,.00,.00,.00,},
{.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,31.83,.00,.00,.00,.00,.00,.00,.00,.00,284.77,.00,.00,.00,.00,.00,.00,-73.75,.00,.00,-59.38,.00,.00,-21.38,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-7.14,.00,.00,-9.05,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,},
{.00,23.50,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,-31.00,.00,.00,142.88,.00,.00,.00,.00,.00,.00,41.27,.00,.00,.00,.00,.00,.00,.00,.00,.00,-33.89,23.06,.00,-37.50,.00,.00,-59.29,.00,.00,.00,.00,.00,.00,-17.50,.00,.00,-37.50,.00,.00,.00,-40.66,.00,.00,.00,71.81,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,.00,}};
    //内置类
    private class UnitsState{
       //属性,为了方便全部弄成public
        public Unit base = null;    //基地
        public Unit bArrackes = null;   //兵营
        //兵力数
        public int workerNum =0;
        public int lightNum=0;
        public int rangedNum =0;
        public int heavyNum = 0;
        public int unitNum =0; //包含兵营和基地的所有单位的数目
        public int[] averagePos = new int[2];   //单位的平均位置，除了基地外的所有单位横纵坐标的平均值
        //构造函数
        public UnitsState(){
               resetToZero();
        }
        //更新 参数是所有单位(我方单位还是地方单位要手动区分（包括基地和未分配的单位）
        void update( List<Unit> Units){
            resetToZero();
            int allNums =0;
            int allX =0;
            int allY=0;
            for(Unit u: Units){
                if(u!=null){
                    allNums++;
                    allX+=u.getX();
                    allY+=u.getY();
                    if(u.getType()==baseType){
                        base= u;
                    }else if(u.getType()==barracksType){
                        bArrackes=u;
                    }else if(u.getType() == workerType){
                        workerNum++;
                    }else if(u.getType() == lightType){
                        lightNum++;
                    }else if(u.getType()==rangedType){
                        rangedNum++;
                    }
                    else if(u.getType()== heavyType){
                        heavyNum++;
                    }
                 }
            }
            averagePos[0] = (int)allX/allNums;
            averagePos[1]= (int)allY/allNums;
            unitNum = allNums;
        }
        public void resetToZero(){
            base = null; 
            bArrackes = null;
            workerNum =0;
            lightNum=0;
            rangedNum =0;
            heavyNum = 0;
            unitNum =0;
            averagePos[0]=-1;
            averagePos[1]=-1;
        }
    }
    //内置类 训练版本,以及和Qlearning有关的参数
    private class QTrainInformation{
        public int version=2 ; //训练版本,目前有 0 8*8左上 1 8*8 右下 2 16*16左上  3 16*16右下,目前有 0 8*8左上 1 8*8 右下 2 16*16左上  3 16*16右下
        public double alpha= 0.5;
        public double Gamma = 0.5;
        public String QMatrixFileName= null;
        public String rewardMatrixFileName = null;
        public int actionNum=8;
        public int stateNum=100;
        public int timeStep=100;
        public QTrainInformation(int version,double alpha, double Gamma, int actions, int states, int timeSteps){   
            setTrainParameter(alpha, Gamma);
            setMatrixSize( actions,states);
            setTimeStep(timeStep);
            QMatrixFileName = "QMatrix_V" + Integer.toString(version)+".txt";
            rewardMatrixFileName = "rewardMatrix_V" + Integer.toString(version)+".txt";
        }
        public QTrainInformation(int version){
            //默认值
            setTrainParameter(0.5, 0.5);
            setMatrixSize( 8,100);
            setTimeStep(100);
            QMatrixFileName = "QMatrix_V" + Integer.toString(version)+".txt";
            rewardMatrixFileName = "rewardMatrix_V" + Integer.toString(version)+".txt";
        }
        //方法,设定能够运行时改变的训练参数
        public void setTrainParameter(double a, double r){
            alpha =a;
            Gamma =r;
        }
        public void setMatrixSize( int actions, int states){
             actionNum = actions;
             stateNum = states;
        }
        public void setTimeStep(int step){
            timeStep = step;
        }
    }
    
    //内置类，上一次的学习状态
    private class LastLearningState{
          public int lastAction;        //上一次的action值
          public int lastState;         //上一次的state值
          public int lastSituation;      //上一次的战场局势
          public int lastEnermyA_D;      //上一次敌军的攻守状态
          public int lastEnermyMajor;    //上一次敌军的主力兵种
          public int lastCombatValue=0; //上一个决策状态的战斗力，初始化为0
          public int lastTime=0;      //上一次决策的时间
          //构造函数
          public LastLearningState(){
              lastTime=0;
              lastCombatValue=0;
          }
          //更新函数
          public void update(int args[])throws ArrayIndexOutOfBoundsException{
              try{
                  lastAction = args[0];
                  lastState = args[1];
                  lastSituation = args[2];
                  lastEnermyA_D = args[3];
                  lastEnermyMajor = args[4];
                  lastCombatValue = args[5];
                  lastTime=args[6];
              }catch(Exception e){
                  throw new ArrayIndexOutOfBoundsException();
              }
          }
    }
     
    //AI版本，根据不同的地图大小以及不同的出生点位置选择不同的版本,目前有 0 8*8左上 1 8*8 右下 2 16*16左上  3 16*16右下,目前有 0 8*8左上 1 8*8 右下 2 16*16左上  3 16*16右下
     int version=2;  
     UnitTypeTable m_utt = null;
     UnitType workerType;
     UnitType baseType;
     UnitType barracksType;
     UnitType lightType;
     UnitType heavyType;
     UnitType rangedType;
     int maxBuildWorkersNum=1;
     int maxHarvestWorkersNum=2; //最多有两个农民用于采矿
     int leastWorkersNum =3;    //最少worker的数目
     int maxOffendWorkersNum=6; //rush农民数目 -1表示无限制
     int maxHeavyNum=-1; // 重甲兵的最大数目
     int maxLightNum=-1;  //轻甲兵的最大数目
     int maxRangedNum=-1; //远程兵的最大数目
     List<Unit> harvestWorkers = new ArrayList<>();
     List<Unit> buildWorkers = new ArrayList<>();
    // Unit my_Base = null;
    // Unit my_Barracks = null;
     //保存的上一次的单位状态
     UnitsState ourUnitsState = new UnitsState();   //我方的
     UnitsState enermyUnitsState = new UnitsState();   //地方的
     
     //一些矩阵
     int m_battleWeight[][] ={    //战斗权系数矩阵,(单兵战斗力对比一致性矩阵)
        {1 ,-3,-5,-4},{3 ,1, 2, -2},{ 5 ,-2, 1, 2},{4 ,2, -2 ,1}};
     int m_AD_weight_V1[][]={      //我方在左上角的时候..
         { 4,2},{2,1}
     };
     int m_AD_weight_V2[][]={      //我方在右下角的时候..
         { 1,2},{2,4}
     };
     
    //和getState有关的数组
    // 0-50： 大劣势-大优势
   // int offsetSituation[] = {0,10,20,30,40};
    int offsetSituation[] = {0,20,40,60,80};
    // 0 防守 5 进攻
    int offsetD_A[]={0,10};
    // 0-4 worker light ranged hearv 混合
    //int offsetMajor[]={0,1,2,3,4};
    int offsetMajor[]={0,1,2,3,4,5,6,7,8,9};
    //和action 有关的数组
    int actionA_D[]={4,4,4,4,0,0,0,0};
    String actionMajor[]={"Light","Light","Ranged","Ranged","Worker","Light","Ranged","Light"};
    
    //和Qlearning 有关的参数 ,Qlearning 在构造函数中构造，而Information 在getAction中生成
    Qlearning myQlearning = null;
    QTrainInformation trainInformation = null;
    
    //过去的状态
    LastLearningState lastState = new LastLearningState();

    
    public QLAi(UnitTypeTable a_utt) {
       this(a_utt, new AStarPathFinding());
    }
    public QLAi(UnitTypeTable a_utt,PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }
    
    public AI clone() {
        return new QLAi(m_utt,pf);
    }
    public void reset() {
          super.reset();
     }
    public void reset(UnitTypeTable a_utt) {
     
        m_utt=a_utt;
        workerType = m_utt.getUnitType("Worker");
        baseType = m_utt.getUnitType("Base");
        barracksType = m_utt.getUnitType("Barracks");
        lightType = m_utt.getUnitType("Light");
        heavyType =m_utt.getUnitType("Heavy");
        rangedType=m_utt.getUnitType("Ranged");
    }

    public PlayerAction getAction(int player, GameState gs) {
        //变量的声明
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        List<Unit> warriorUnits = new ArrayList<>();
        List<Unit> enermyUnits = new ArrayList<>();
        List<Unit> enermyNormalUnits = new ArrayList<>();
        List<Unit> ourNormalUnits = new ArrayList<>();  //用于评估的我方单位
        List<Unit> ourUnits = new ArrayList<>();
        Unit my_Base=ourUnitsState.base=null;
        Unit my_Barracks = ourUnitsState.bArrackes;
        Unit enermy_Base = enermyUnitsState.base=null;
        Unit enermy_Barracks=enermyUnitsState.bArrackes=null;
        System.out.println("time"+gs.getTime());
        int lastTime=lastState.lastTime;
        int action=lastState.lastAction;
        int state = lastState.lastState;
        int situation = lastState.lastSituation;
        int enermyA_D = lastState.lastEnermyA_D;
        int enermyMajor = lastState.lastEnermyMajor;
        int combatValue = lastState.lastCombatValue;
        int workersNum =0;
        int bX;   //兵营坐标
        int bY;
        if(player==0){
            bX=4;
            bY=3;
        }else{
            bX=13;
            bY=16;
        }
        //创建Qlearning 
        if(gs.getTime()==0){
        //createQlearning(pgs,player);
            this.createQlearningV2(player);
        }
        //获取战场的信息
        if(my_Base!=null &&my_Base.getHitPoints()<=0){
            my_Base = null;
        }
        if( my_Barracks  !=null && my_Barracks .getHitPoints()<=0){
            my_Barracks  = null;
        }
        
        int harvestNum=0;//
        //统计当前harvestWorkers数组里实际能工作的农民数
        for(Unit u : harvestWorkers){
            if(u!=null && u.getHitPoints()>0){
                harvestNum++;
            }
        }
     
       int buildWorkersNum = 0 ;
       boolean isBuild ;
       if(gs.getTime()>=50){
           isBuild = true;}
       else{
           isBuild=false;
       }
         if(my_Barracks!=null){
            isBuild = false;
            buildWorkers.clear();
        }
      //统计农民的数目，农民数目至少为3个
        //统计buildWorkers数据
        for(Unit u :buildWorkers){
            if(u!=null && u.getHitPoints()>0){
                buildWorkersNum++;
            }
        }
     
        //对战场所有单位进行分类
        for(Unit u: pgs.getUnits()){
            if(u!=null ){
                  if(u.getPlayer()==player){
                      if(u.getType() == baseType){   // 基地
                          my_Base = u;
                      }else if(u.getType()== barracksType){   //兵营
                          my_Barracks = u; 
                      }else{    //非基地和非兵营单位
                          ourNormalUnits.add(u);
                          if(gs.getActionAssignment(u)==null ) { //非分配工作的单位
                              if(u.getType() == workerType){
                                   workersNum++;
                                   if(!harvestWorkers.contains(u) && !buildWorkers.contains(u)){
                                   //优先build
                                        if( isBuild && my_Barracks==null && buildWorkersNum<maxBuildWorkersNum){
                                              buildWorkers.add(u);
                                              buildWorkersNum++;
                                        }else if(harvestNum< maxHarvestWorkersNum){
                                              harvestWorkers.add(u);
                                              harvestNum++;
                                        }else{
                                                warriorUnits.add(u);
                                        }
                                   }
                              }else if(u.getType().canAttack){     //可作战单位
                                      warriorUnits.add(u);
                                      System.out.println(warriorUnits.size());
                               }
                          }
                      }
                  }else if(u.getPlayer()!= player){   //敌方的
                      if(u.getType() == baseType){
                           enermy_Base =u ;
                       }else if(u.getType()==barracksType){
                            enermy_Barracks= u;
                       }else if(u.getType().canAttack){
                           enermyNormalUnits.add(u);
                       }
                  }
            }
                      
         }

        enermyUnits.add(enermy_Barracks);
        enermyUnits.add(enermy_Base);
        enermyUnits.addAll(enermyNormalUnits);
        ourUnits.add(my_Barracks);
        ourUnits.add(my_Base);
        ourUnits.addAll(ourNormalUnits);
        
        System.out.println("我方的大小"+ourNormalUnits.size());
        System.out.println("敌方的大小"+enermyNormalUnits.size());
        int battleState[]=this.evaluateState(my_Base,ourNormalUnits, enermy_Base, enermyNormalUnits, 0.4f ,4);
        System.out.println("评估局势");
        int majorUnitType[] = this.evaluateEnermyMajorUnit(pgs, p, 6, 3);
        enermyA_D=this.evaluate_AD_Tactics(pgs, p,1.2f );
        situation = battleState[0];
        //开始Q-learning 
        //获取majorUnitType
        enermyMajor = getMajorType(majorUnitType);
        //获取state
        state = getState(situation ,enermyA_D, enermyMajor);
        //开局设一个单独的状态
        System.out.println("当前的状态"+state);
        if(gs.getTime()==551||gs.getTime()-lastTime>=trainInformation.timeStep){
            lastTime= gs.getTime();
            System.out.println("训练 lastTime"+lastTime);
            if(gs.getTime()>=550){
                if(battleState[1]!=-1000){
                   //  action=myQlearning.learning(state,battleState[1]-combatValue, true);
                   action = myQlearning.makeDecision(state);
                }else{
                     //action=myQlearning.learning(state,battleState[1], true);
                      action = myQlearning.makeDecision(state);
                }
            }
            //将当前战斗力存储下来
            System.out.println("本次的rewardv "+(battleState[1]-combatValue));
            combatValue=battleState[1];
            myQlearning.printLastS_A();
        }else if( gs.getTime()<=550){
            this.workersBehavior(harvestWorkers,buildWorkers, p, pgs,isBuild,1,bX,bY);
            if(enermy_Barracks!=null){
             action=5;
            }else {
             System.out.println("敌方没有兵营，建造worker");
             action=4;
            }
        }
        
        String ourMajor=actionMajor[action];
        int ourA_D=this.actionA_D[action];
        
        if(gs.getTime()>=150 ){
             this.baseBehavior(my_Base, p, gs, 3,0);
        }else{
             this.baseBehavior(my_Base, p, gs, 4,2);   
        }
        // 使用战略
       if(gs.getTime()>=150){
            if(ourMajor!="Worker" ){
                 this.workersBehavior(harvestWorkers,buildWorkers, p, pgs, isBuild,1,bX,bY);
                this.rushTactics(gs, player, ourMajor, my_Barracks, warriorUnits, enermyUnits, 2,ourA_D);
            }else{
                 this.workersBehavior(harvestWorkers,buildWorkers, p, pgs, isBuild,1,bX,bY);
                 this.rushTactics(gs, player, ourMajor, my_Base, warriorUnits, enermyUnits, 2,ourA_D);
            }
       }
       //更新信息
        int args[]={        
        action,
        state ,
        situation,
        enermyA_D ,
        enermyMajor ,
        combatValue,
        lastTime,
        };
        try{
            lastState.update(args);
        }catch(Exception e){
            System.out.println("更新参数时数组越界");
        }
        ourUnitsState.update(ourUnits);
        enermyUnitsState.update(enermyUnits);
        //辅助信息
        switch(majorUnitType[0]){
            case 0:  System.out.println("以Worker为主");break;
            case 1:    System.out.println("以Light为主");break;
            case 2:   System.out.println("以Ranged为主");break;
            case 3:    System.out.println("以Heavy为主");break;
            case 4:    System.out.println("混合兵种");break;
            default:  System.out.println("判断主要兵种出错");
        }
        switch(enermyA_D){
            case 1:  System.out.println("敌方进攻");break;
            case 0:  System.out.println("敌方防守");break;
            default:  System.out.println("判断攻守失败");
        }
        //System.out.println("###");
        //System.out.println(enermyUnits.size());
        //农民建筑和收获
     
        
        switch(battleState[0]){
            case 0: System.out.println("我方大劣势");break;
            case 1: System.out.println("我方劣势");break;
            case 2: System.out.println("局势平衡");break;
            case 3: System.out.println("我方优势");break;
            case 4: System.out.println("我方大优势");break;
            default: System.out.println("评估出错");
        }
           System.out.println("getActionOver\n");
        return translateActions(player, gs);
     
    }
    
 
    //一些游戏过程的函数
    private void createQlearning(PhysicalGameState pgs, int player){
        int sizeOffset[] ={0,2,4,6};
        int playerOffset[]={0,1};
        int sizeIndex= (pgs.getWidth() /8)-1;
        int playerIndex = player;
        version = sizeOffset[sizeIndex]+ playerOffset[playerIndex];
        trainInformation = new QTrainInformation(version);
        int actionNum = trainInformation.actionNum;
        int stateNum = trainInformation.stateNum;
        String QMatrixFileName = trainInformation.QMatrixFileName;
        String rewardMatrixFileName = trainInformation.rewardMatrixFileName;
        double alpha = trainInformation.alpha;
        double r =trainInformation.Gamma;
        myQlearning=new Qlearning(actionNum,stateNum,alpha,r,".\\"+QMatrixFileName,".\\"+rewardMatrixFileName);
    }
    private void createQlearningV2( int player){
        trainInformation = new QTrainInformation(version);
        int actionNum = trainInformation.actionNum;
        int stateNum = trainInformation.stateNum;
        if(player==0){
            this.myQlearning = new Qlearning(this.QMatrixV2,actionNum,stateNum);
        }else if(player==1){
             this.myQlearning = new Qlearning(this.QMatrixV3,actionNum,stateNum);
        }
    }

    private int getMajorType(int[] majorUnitType){
        int majorUnit=-1;
        if(majorUnitType[0]==4){
            majorUnit=3+majorUnitType[1];
        }else{
            majorUnit=majorUnitType[0];
        }
        return majorUnit;
    }
    private int  getState(int situation, int A_D, int major){
        int state=0;
        if( major==-1 || situation==-1||  A_D==-1){
            state=20;
        }else{
           state =  this.offsetSituation[situation] + this.offsetD_A[A_D] +this.offsetMajor[major];
        }
        return state;
    }
    /*
    判断对面是攻击还是防守的函数
    返回值 0：敌方防守 1：敌方进攻 : -1不正常
    input: gs 同之前，
           p: 我方的player！
    NumThrehold,决定是攻还是守的阈值 如果 结果<NumThrehold 结果是防守， 结果》=NumThrehold 结果是攻击
    */
    public int evaluate_AD_Tactics(PhysicalGameState pgs, Player p, float NumThrehold ){
      int width = pgs.getWidth();
      int height = pgs.getHeight();
      int center[] = {width/2,height/2};
      int AD_weight[][] = new int[2][2];
      switch(p.getID()){              // ID是 0 基地在左上方 1基地在右下方
          case 0: AD_weight = m_AD_weight_V1; break;//左上方的时候选择V1矩阵 
          case 1: AD_weight=m_AD_weight_V2;break;
          default: return -1;
      }
      int i=0;
      int j=0;
      //将整个战场分成4块，统计敌方的兵力在4块所占的比例，然后* AD_weight 根据最后的结果与阈值比较得出攻守结论
      float percentOfUnits[][] = new float[2][2];  //初始化
      int UnitsNum=0;
      for(i=0;i<2;i++){
          for(j=0;j<2;j++){
              percentOfUnits[i][j] =0 ;
          }
      }
      for(Unit u :pgs.getUnits()){
            if(u!=null && u.getPlayer()>=0 && u.getPlayer()!=p.getID() && u.getType().canAttack){
               
                int tempX=u.getX(); int tempY=u.getY();   //获取坐标
                if(tempX>=0 && tempX<center[0] && tempY>=0 && tempY<center[1]){   //左上
                    percentOfUnits[0][0]++; 
                    UnitsNum++;  //总数目+1
                 }else if (tempX>=center[0] && tempX<=width && tempY>=0 && tempY<center[1]) {   //右上
                    percentOfUnits[0][1]++;
                     UnitsNum++;  //总数目+1
                 }else if (tempX>=0 && tempX<center[0] && tempY>=center[1] && tempY<=height){   //左下
                    percentOfUnits[1][0]++;
                     UnitsNum++;  //总数目+1
                 }else if (tempX>=center[0] && tempX<=width && tempY>=center[1] && tempY<=height){  //右下
                     percentOfUnits[1][1]++;
                     UnitsNum++;
                 }
            }
      }
      //计算概率
      if(UnitsNum==0){
          return -1;
      }
       for(i=0;i<2;i++){
          for(j=0;j<2;j++){
              percentOfUnits[i][j] /= UnitsNum ;
          }
      }
       //和权矩阵相乘
       float AD_Value =0;
        for(i=0;i<2;i++){
          for(j=0;j<2;j++){
             AD_Value+= percentOfUnits[i][j] * AD_weight[i][j] ;
          }
      }
       if(AD_Value>=0 && AD_Value< NumThrehold){
           return 0;   //防守
       }else if(AD_Value >=NumThrehold){
           return 1;   //进攻
       }else{
           return -1;  //出错
       }
    }
    
    /*
    判断对面的主力兵种的函数，（主要是Light,Ranged,Heavy,Worker,混合兵种（没有一个兵种的数目占绝对优势的时候）) 其中 light heavy ranged的数量比例都是 1：1 ,而worker不然
    参数说明： Player p, 我方
               PhysicalGameState  pgs 战场的信息
               workerValue:将农民折换成 light heavy ranged  折换后的Num= 原本Num/workerValue
               NumThreshold: 确定是否有主力兵种的阈值，如果 maxNum-second<NumThreshoud(不包括NumThreshoud) 认为这时候对面是混合兵种。
    output: 一个int数组，int[0] 0:主力是worker 1主力是light 2主力是ranged 3主力是 heavy 4 混合兵种  int[1]:如果不是混合兵种的话，该值为0，否则该值为混合的类型 1： WL 2: WR 3： WH 4：LR 5 LH 6 RH ,如果有任意一个值为-1的话说明返回值有问题
    */
    public int[] evaluateEnermyMajorUnit(PhysicalGameState pgs, Player p,int workerValue, int NumThreshold){
        //统计战场上敌方兵种的数目
        //分量 0 农民数目，1 light 2 ranged 3hearvy
       int MajorUnitType[]= new int[2];
       MajorUnitType[0]=-1;MajorUnitType[1]=-1;
       int UnitNum[]=new int[4];
       //初始化
       int i=0;
       for(i=0;i<4;i++){
           UnitNum[i]=0;
       }
       //数目最多的兵种和第二多的兵种 ，-1表示尚未统计，而 0表示农民 1:light 2:ranged 3:hearvt
       int largestUnitID=-1;
       int secondUnitID=-1;
       //统计数目
       for(Unit u : pgs.getUnits()){
          if(u!=null && u.getPlayer()>=0 && u.getPlayer()!=p.getID() && u.getType().canAttack){
              if(u.getType()==workerType){
                  UnitNum[0]++;
              }else if(u.getType()==lightType){
                  UnitNum[1]++;
              }else if(u.getType()==rangedType){
                  UnitNum[2]++;
              }else if(u.getType()==heavyType){
                  UnitNum[3]++;
              }
          }
       }
       //折换worker的数目
       UnitNum[0]/=workerValue;
       int maxNum=0;
       int secondNum=0;
       //数量小直接暴力统计了..
       for(i=0;i<4;i++){
           if(largestUnitID!=-1 && UnitNum[i]>maxNum){
               largestUnitID=i;
               maxNum=UnitNum[i];
           }else if(largestUnitID==-1){
               largestUnitID=i;
               maxNum=UnitNum[i];
           }
       }
       for(i=0;i<4;i++){
           if(i!=largestUnitID){
               if(secondUnitID!=-1 && UnitNum[i]>secondNum){
               secondUnitID=i;
               secondNum=UnitNum[i];
                }else if(secondUnitID==-1){
                    secondUnitID=i;
                    secondNum=UnitNum[i];
                }  
           }
       }
          
       //判断主力兵种
       //output: 一个int数组，int[0] 0:主力是worker 1主力是light 2主力是ranged 3主力是 heavy 4混合兵种  int[1]:如果不是混合兵种的话，该值为0，否则该值为混合的类型 1： WL 2: WR 3:WH  4：LR 5 LH 6 RH ,如果有任意一个值为-1的话说明返回值有问题
       if(largestUnitID==-1 || secondUnitID==-1){
            MajorUnitType[0]=-1;
           MajorUnitType[1]=-1;
           System.out.println("判断主力兵种出错1");
           return MajorUnitType;
       }
       if(secondNum==0 && maxNum!=0){
           MajorUnitType[0]=largestUnitID;
           MajorUnitType[1]=0;
       }
       else if(maxNum-secondNum >NumThreshold ){
           MajorUnitType[0]=largestUnitID;
           MajorUnitType[1]=0;
       }else{
          MajorUnitType[0]=4;
          int tempFlag = (largestUnitID+1)*(secondUnitID+1); //加以相乘，主要为了方便判断, 由上面largestUnitID!=secondUnitID
                switch(tempFlag){
                    case 2 :   MajorUnitType[1]=1;break;  //WL
                    case 3 :   MajorUnitType[1]=2;break;  //WR
                    case 4 :   MajorUnitType[1]=3;break;  //WH
                    case 6 :   MajorUnitType[1]=4;break;  //LR
                    case 8 :   MajorUnitType[1]=5;break;  //LH
                    case 12 :  MajorUnitType[1]=6;break;  //RH
                    default : MajorUnitType[1]=-1;
                }
          }
        return MajorUnitType;
    }
    
    /* 
    分析战场形势的函数：从两个方面进行评估（1 敌我双方各个单位数目的差值（一定考虑） 2 敌我双方单位和对方基地的距离的插值（可选择考虑的程度）暂时不考虑兵营
    output: 一个 int[0] 范围从 0 - 4  0:我方大劣势 1：我方劣势 2：双方均等 3：我方优势 4 我方大优势 , int[1]真实的战斗力
    input: gs同之前 , 
           ourUnits(我方单位）和enermyUnits(敌方单位）是评估的对象， 
           myBase 我方基地   enermyBase 敌方基地
           distanceLevel ： 敌我双方距离的影响因子，即是否将敌我双方距离对方基地的距离也纳入评估的范围  范围从 0 到 1 0是不考虑距离，1 考虑距离且程度最大
           scoreStep : 评价阈值的步长
    */
    public int[] evaluateState(Unit my_Base,List<Unit> ourUnits,Unit enermy_Base, List<Unit> enermyUnits,float distanceLevel, int scoreStep){
        /* 
        
        battleMatrix战斗矩阵 行、列（0:代表 worker, 1:light 2:range 3:heavy)
        battleMatrix[i][j] 表示 我方的i兵种战斗力之和 - 敌方的 j兵种战斗力之和
        单个兵种的战斗力公式 1+(maxd/d)*distanceLevel 注：这里没有考虑兵种的差异，而实考虑数目和距离，兵种的差异体现在之后还有一个 矩阵 maxd表示地图最大距离，而d 表示本兵种距离对方基地的距离
        */
        //输出
         System.out.println("进入评估函数");
        int output[]={0,0};
        int battleWeight[][] = m_battleWeight;//战斗权系数矩阵,(单兵战斗力对比一致性矩阵)
        if(my_Base==null && enermy_Base!=null){    //我方没基地了 大劣势
            System.out.println("我方没基地了");
            output[0]=0;
            output[1]=-1000;
            return output;
        }
        if(enermy_Base == null && my_Base!=null){   //敌方没基地了 大优势
            System.out.println("敌方没基地了");
            output[0]=4;
            output[1]=1000;
            return output;
        }
        if(my_Base == null && enermy_Base == null){    //敌我双方都没有基地，不考虑距离因素
            System.out.println("都没基地了");
            distanceLevel=0;
        }
        float ourCombat[] = new float[4];  //我方战力
        float enermyCombat[]= new float[4]; //敌方战力
        //战力初始化
        int i,j;
        for(i=0;i<4;i++){
            ourCombat[i]=0;
            enermyCombat[i]=0;
        }
        int maxD;
        //获取地图最大距离
        if(distanceLevel==0){
            maxD=0;
        }else{
            maxD= Math.abs( my_Base.getX()- enermy_Base.getX())+ Math.abs( my_Base.getY()- enermy_Base.getY());
        }
        //开始统计我方战斗数组
        for(Unit u1: ourUnits){
             
           if(u1!=null && u1.getType().canAttack){  //如果不为空且能够进攻
                float combatValue=0;
                if(enermy_Base!=null){   //敌方基地存在才能计算距离
                       int d = Math.abs( u1.getX()- enermy_Base.getX())+ Math.abs(u1.getY()- enermy_Base.getY());
                       System.out.println(enermy_Base.getX()+" " +enermy_Base.getY());
                       combatValue = maxD+(maxD/d)*distanceLevel;
                }else{
                    combatValue =maxD;
                }
                UnitType uType =u1.getType();
                if(uType == workerType){
                    ourCombat[0]+=combatValue;
                }else if(uType == lightType){
                    ourCombat[1]+=combatValue;
                }else if(uType== rangedType){
                    ourCombat[2]+=combatValue;
                }else if(uType == heavyType){
                    ourCombat[3]+=combatValue;
                }
           }
        }
       
        //统计敌方战斗数组
        for(Unit u2: enermyUnits){
               if(u2!=null && u2.getType().canAttack){  //如果不为空且能够进攻
                    float combatValue=0;
                    if(my_Base!=null){   //我方基地存在才能计算距离R
                           int d = Math.abs( u2.getX()- my_Base.getX())+ Math.abs(u2.getY()- my_Base.getY());
                           combatValue = maxD+(maxD/d)*distanceLevel;
                    }else{
                        combatValue =maxD ;
                    }
                    UnitType uType =u2.getType();
                    if(uType == workerType){
                        enermyCombat[0]+=combatValue;
                    }else if(uType == lightType){
                        enermyCombat[1]+=combatValue;
                    }else if(uType== rangedType){
                        enermyCombat[2]+=combatValue;
                    }else if(uType == heavyType){
                        enermyCombat[3]+=combatValue;
                    }
               }
            }
          //计算 myCombatAll
          float myCombatAll=0;
          int enermyNoZero=0;
          System.out.println("循环2");
          for(i=0;i<4;i++){
              if(enermyCombat[i]!=0){
                enermyNoZero++;
              }
          }   
          if(enermyNoZero==0){
              output[0]=4;
              output[1]=1000;
              return output;
          }
          for(i=0;i<4;i++){
              float myCombatI=0;
              if(ourCombat[i]!=0){
                for(j=0;j<4;j++){
                    if(enermyCombat[j]!=0){
                        if(battleWeight[i][j]>0){
                           myCombatI+=(ourCombat[i]*battleWeight[i][j]-enermyCombat[j]);
                        }else{
                             myCombatI+=(ourCombat[i]+enermyCombat[j]*battleWeight[i][j]);
                        }
                    }
                }
                myCombatAll+= myCombatI/ enermyNoZero;
              }
          }
         //更新output
         output[1]=(int)myCombatAll;
         System.out.println("战力之和"+myCombatAll);
         scoreStep*=maxD;
         //评估威胁

         if(myCombatAll> -scoreStep && myCombatAll< scoreStep  ){
             output[0]=2;
            // return 2;
         }else if(myCombatAll>=scoreStep&& myCombatAll<3*scoreStep){
             output[0]=3;
           //  return 3;
         }else if(myCombatAll>=3*scoreStep){
             output[0]=4;
           //  return 4;
         }else if(myCombatAll<=-scoreStep&& myCombatAll> -3*scoreStep){
             output[0]=1;
           //  return 1;
         }else if(myCombatAll<=-3*scoreStep){
             output[0]=0;
           //  return 0;
         }else{
             System.out.println("评估出现问题,认为战力相同");
           //  return 2;
         }
          
         return output;
    }
    
    /*
      获取reward的函数，根据对战的结果我们发现对于攻击和防守reward应该不同
      根据成员变量 上一步的action 以及当前战场的情况来得到上一步action 的reward
    */
    private double getReward(){
        return 0;
    }
    /*
    gs 和 Player同之前
    input :
    rushType:进攻策略 ，有 worker rush, heavy rush , light rush 和 range rush
    Unit trainBuilding 用来训练的单位,如果是worker rush就选择 base ,如果是 非worker rush 就选择 兵营，如果不选择训练 就 null
    ourUnits:我方的目前可用的单位(不包括农民，目前的设想是，这些单位是用于进攻的单位）
    enermyUnits: 敌方的单位
    rushTarget: 进攻目标 1是进攻基地，2进攻非采矿单位
    rushLevel : 进攻程度 0：防守   1：少许进攻  2：半数进攻（选 0.5倍兵力进攻） 3：强势进攻（选0.75倍兵力进攻） 4：全力进攻（所有的兵力都进攻）
    */
    public void rushTactics(GameState gs, int player, String rushType,Unit trainBuilding,List<Unit>ourUnits, List<Unit> enermyUnits, int rushTarget, int rushLevel)
    {   
        PhysicalGameState pgs=gs.getPhysicalGameState();
        Player p=gs.getPlayer(player);
        List<Unit> warriorUnits = new ArrayList<>(); //主进攻单位
        List<Unit> defendUnits = new ArrayList<>(); //防御单位
        List<Unit> otherAttackUnits = new ArrayList<>(); //非主进攻单位
        int warriorNum=0;    //当前进攻数目
        int maxWarriorNum;  //最大进攻数目，由rushlevel决定
        if(rushLevel==0)
            maxWarriorNum=0;
        else
            maxWarriorNum=ourUnits.size();
        System.out.println("ourUnits:"+ourUnits.size());
        System.out.println("maxWarriorNum:"+ maxWarriorNum);
        //根据rushType训练相应的单位,以及挑选相应的主进攻队伍,次进攻队伍,以及防御队伍
        for(Unit u:ourUnits){
           if(u.getType() == m_utt.getUnitType(rushType)){
                //有rushlevel决定，如果数目达到进攻上限，剩下的队伍转为防守
                if(warriorNum <maxWarriorNum){    
                    warriorUnits.add(u);
                    warriorNum++;
                 }
                else{
                     defendUnits.add(u);
                }
                
            }
            else{
                if(warriorNum<maxWarriorNum){
                    otherAttackUnits.add(u);
                    warriorNum++;
                }
                else{
                    defendUnits.add(u);
                }
            }
        }
         //训练相应的rush单位
         if(trainBuilding!=null){
             if(trainBuilding.getType()==baseType){
                 this.baseBehavior(trainBuilding, p, gs, 2,0);
             }else if(trainBuilding.getType()==barracksType){
                 this.barracksBehavior(trainBuilding, p, gs, rushType);
             }
         }
         //对敌方单位进行分类
         Unit enermyBase=null;
         List<Unit> e_NoBaseUnits=new ArrayList<>();
         for(Unit u: enermyUnits){
             if(u!=null){
                 if(u.getType()==m_utt.getUnitType("Base")){
                     enermyBase=u;
                 }
                 else {
                     e_NoBaseUnits.add(u);
                 }
             }
         }
         List<Unit> targetUnits= new ArrayList<>();
         if(rushTarget==1 && enermyBase!=null){
             targetUnits.add(enermyBase);
         }else if(rushTarget==2 && e_NoBaseUnits.size()>=3){
              // System.out.println("##");
              //System.out.println(e_NoBaseUnits.size());
              for(Unit target:e_NoBaseUnits){
                  targetUnits.add(target);
              }
         }else{
             targetUnits=enermyUnits;
         }
         
         //主进攻部队进攻
         for(Unit warrior:warriorUnits){
             attackBehavior(warrior,p,gs); 
         }
         //次进攻部队进攻
         for(Unit warrior:otherAttackUnits){
             attackBehavior(warrior,p,gs);
         }
         for(Unit warrior:defendUnits ){
             if(gs.getTime()<=2500){
                  defenceBehavior(warrior,p,gs);
             }else{
                  attackBehavior(warrior,p,gs);
             }
         }
    }

    /*
    intput : trainType : 1 根据maxharvestWorkersNum 训练用来收获的农民
                         2 根据maxOffendWorkersNum 训练用来防御的农民
                        3 根据leastWorkersNum 维持农民数量
                        4 任意数目，数目由另外一个参数给定，只有在trainType为4的时候才有效
    */
    public void baseBehavior(Unit u, Player p, GameState gs,int trainType,int trainNum) {
         //先统计目前战场上的农民的数目
        PhysicalGameState pgs=gs.getPhysicalGameState();
        int allWorkers=0;
        int harvestWorkers = 0; 
        int offendWorkers =0;
        for (Unit u2:selectUnitsAround(gs, p.getID(), "Worker", 0, 0, pgs.getWidth(), pgs.getHeight(), -1, false)) {
            //说明，这里我不知道怎么确定自己的农民现在正在干啥。
            if(u2!=null){
                AbstractAction aa = getAbstractAction(u2);
                if (aa instanceof Harvest) {
                      allWorkers++;
                    harvestWorkers++;
                }else if(aa instanceof Attack){
                      allWorkers++;
                    offendWorkers++;
                }else{
                    allWorkers++;
                }
            }
        }
        switch(trainType){
            case 1:        //训练用来收获的农民
                if(maxHarvestWorkersNum == -1 && p.getResources()>=workerType.cost){
                    train(u,workerType);
                }else if(harvestWorkers<maxHarvestWorkersNum && p.getResources()>=workerType.cost){
                     train(u,workerType);
                }
                break;
            case 2:     //训练用来rush的农民
                if(maxOffendWorkersNum==-1 && p.getResources()>=workerType.cost){
                    train(u,workerType);
                }else if(offendWorkers< maxOffendWorkersNum && p.getResources()>=workerType.cost){
                    train(u,workerType);
                }
                break;
            case 3:
                /*
                if(maxHarvestWorkersNum == -1 && p.getResources()>=workerType.cost){
                    train(u,workerType);
                }else if(harvestWorkers<maxHarvestWorkersNum && p.getResources()>=workerType.cost){
                     train(u,workerType);
                }
                if(maxOffendWorkersNum==-1 && p.getResources()>=workerType.cost){
                    train(u,workerType);
                }else if(offendWorkers< maxOffendWorkersNum && p.getResources()>=workerType.cost){
                    train(u,workerType);
                }*/
                if(allWorkers<leastWorkersNum){
                    train(u,workerType);
                }
                break;
            case 4:
                if(allWorkers<trainNum){
                    train(u,workerType);
                }
                break;
            default:
                break;
                
        }
    }
     
    /*
     兵营的行为，参数说明：最后一个参数 String type (训练的类型）
     */
    public void barracksBehavior(Unit u, Player p, GameState gs, String type) {
        PhysicalGameState pgs=gs.getPhysicalGameState();
        if(type=="Light"){
            int Lights=0;
            for(Unit u2: selectUnitsAround(gs, p.getID(), "Light", 0, 0, pgs.getWidth(), pgs.getHeight(), -1, false) ){
                if(u2.getType().canAttack){
                    Lights++;
                }
            }
            if(maxLightNum == -1 && p.getResources()>=lightType.cost){
                    train(u,lightType);
             }else if( Lights<maxLightNum  && p.getResources()>=lightType.cost){
                     train(u,lightType);
             }
        }else if(type=="Heavy"){
            int Heavys=0;
            for(Unit u2: selectUnitsAround(gs, p.getID(), "Heavy", 0, 0, pgs.getWidth(), pgs.getHeight(), -1, false) ){
                if(u2.getType().canAttack){
                    Heavys++;
                }
            }
            if(maxLightNum == -1 && p.getResources()>=heavyType.cost){
                    train(u,heavyType);
             }else if( Heavys<maxLightNum  && p.getResources()>=heavyType.cost){
                     train(u,heavyType);
             }
         }else if(type=="Ranged"){
            int rangeds=0;
            for(Unit u2: selectUnitsAround(gs, p.getID(), "Ranged", 0, 0, pgs.getWidth(), pgs.getHeight(), -1, false) ){
                if(u2.getType().canAttack){
                    rangeds++;
                }
            }
            if(maxRangedNum == -1 && p.getResources()>=rangedType.cost){
                    train(u,rangedType);
             }else if( rangeds<maxRangedNum  && p.getResources()>=rangedType.cost){
                     train(u,rangedType);
             }
         }
    }
    
    /* 
    农民的行为,建筑+收获， 也就是说参数 workers应该是掉实现分开的用来收获的农民（加建筑）
    附加参数 ： isBuild 是否增加建筑命令，true建建筑 ,false 不建
    baseNum :我方允许的基地数目
    barracksNum :我方允许的兵营的数目
    barrack_x,barrack_y:兵营坐标
    input: 
    */
    public void workersBehavior(List<Unit>harvestWorkers,List<Unit> builderWorkers, Player p, PhysicalGameState pgs,boolean isBuild,int barracksNum,int barrack_x,int barrack_y) {
        int nbarracks = 0;

        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        List<Unit> workers = new LinkedList<Unit>();
        freeWorkers.addAll(harvestWorkers);
        workers.addAll(builderWorkers);

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }

        List<Integer> reservedPositions = new LinkedList<Integer>();
         System.out.println("workersize"+workers.size());
     //    if(!isBuild){
       //      System.out.println("不允许建造兵营");
       //  }
        if(isBuild &&!workers.isEmpty()){
              if (nbarracks < barracksNum ) {
                // build a barracks:
                if (p.getResources() >= barracksType.cost + resourcesUsed ) {
                    System.out.println("资源足够建造兵营");
                    Unit u = workers.remove(0);
                  //  move(u,barrack_x,barrack_y);
                    buildIfNotAlreadyBuilding(u,barracksType,barrack_x,barrack_y,reservedPositions,p,pgs);
                    resourcesUsed += barracksType.cost;
                }else{  //否则让建筑工人去采矿
                   // if(p.getResources() < barracksType.cost + resourcesUsed ){
                         System.out.println("资源不够建造兵营");
                    //}
                    
                    for(Unit u: workers){
                        if(u!=null){
                            freeWorkers.add(u);
                        }
                    }
                }
            }
        }

        // harvest with all the free workers:
        for (Unit u : freeWorkers) {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isResource) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestResource == null || d < closestDistance) {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource != null && closestBase != null) {
                AbstractAction aa = getAbstractAction(u);
                if (aa instanceof Harvest) {
                    Harvest h_aa = (Harvest)aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
                } else {
                    harvest(u, closestResource, closestBase);
                }
            }
        }
    }
    //进攻
    public void attackBehavior(Unit u,Player p,GameState gs)
    {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;  
        if(gs.getActionAssignment(u)==null && u.getType().canAttack)
        {
            for(Unit u2:pgs.getUnits()) 
            {
                if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) 
                { 
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestEnemy==null || closestDistance==0|| d<closestDistance) 
                    {
                        closestEnemy = u2;
                        closestDistance = d;
                    }
                }
            }
            attack(u,closestEnemy);
        }
    }    
    public void defenceBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        Unit closestMeleeEnemy = null;
        int closestDistance = 0;
        int enemyDistance = 0;
        int mybase = 0;
        for(Unit u2:pgs.getUnits()) {
            if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) { 
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy==null || d<closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
            else if(u2.getPlayer()==p.getID() && u2.getType() == m_utt.getUnitType("Base"))
            {
                mybase = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
            }
        }
        if (closestEnemy!=null && (closestDistance < pgs.getHeight()/4 || mybase < pgs.getHeight()/4||(u.getX()>10))) {
            attack(u,closestEnemy);
        }
        else
        {
            attack(u, null);
        }
        
    }
    //框选一定范围内的单位，参数：gs, 单位名称，左上角坐标，长，宽, 单位数目（如果为-1则框选全部单位），是否只框选没有行动的单位(没有行动，而不是没有赋与行动（默认所有都框选，如果为true就只框选没有行动的单位）
    public List<Unit> selectUnitsAround(GameState gs,int player,String unitName ,int x,int y, int width,int height,int num, boolean  noAction){
        PhysicalGameState pgs= gs.getPhysicalGameState();
        int tempNum=0;
        List<Unit> selectUnits=new ArrayList<>();
        for(int i=x;i<x+width;i++){
            for(int j=y;j<y+height;j++){
                Unit tempUnit;
                tempUnit=pgs.getUnitAt(i, j);
                if( !( tempUnit == null || tempUnit.getType()!=m_utt.getUnitType(unitName)|| tempUnit.getPlayer()!=player)){
                    //如果是所有的单位都选中的话
                    if(!noAction){
                        selectUnits.add(tempUnit);
                        tempNum++;
                    }
                    else{
                        if(gs.getActionAssignment(tempUnit)==null){
                            selectUnits.add(tempUnit);
                            tempNum++;
                        }
                    }
                }
            }
            if(num!=-1){
                if(tempNum>=num){
                    return selectUnits;
                }
            }
        }
        return selectUnits;
    }
    
    /*获取八个方向的偏移量偏移大小为d
        输入  i:偏移方向 0到7 0表示正上方，然后顺时针旋转
             d:偏移大小
        输出：偏移数组 （x,y)
    
    */
    public int[] getOffset(int i,int d){
        int offset[] =new int[2];
        switch(i){
            case 0: offset[0]=-d; offset[1]=0; return offset;
            case 1: offset[0]=-d; offset[1]=d; return offset;
            case 2: offset[0]=0; offset[1]=d; return offset;
            case 3: offset[0]=d; offset[1]=d; return offset;
            case 4: offset[0]=d; offset[1]=0; return offset;
            case 5: offset[0]=d; offset[1]=-d; return offset;
            case 6: offset[0]=0; offset[1]=-d; return offset;
            case 7: offset[0]=-d; offset[1]=-d; return offset;
            default:  offset[0]=0; offset[1]=0; return offset;
        }
    }
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
 
    
}