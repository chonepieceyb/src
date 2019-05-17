/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package myrtsai;

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
import myrtsai.Qlearning;
/**
 *
 * @author msi-
 */
public class MyRtsAi extends AbstractionLayerAI{
     UnitTypeTable m_utt = null;
     UnitType workerType;
     UnitType baseType;
     UnitType barracksType;
     UnitType lightType;
     UnitType heavyType;
     UnitType rangedType;
     /*
     List<Unit> HarvestWorkers = new ArrayList<>();       //用于收获的工人
     List<Unit> offendWorkers = new ArrayList<>();       //用于进攻的工人
     List<Unit> noHarvestUnits=new ArrayList<>(); //非采集单位
     List<Unit> resources = new ArrayList<>();           //资源
     List<Unit> warriorUnits= new ArrayList<>();  //进攻的士兵
     List<Unit> defendUnits= new ArrayList<>();   //防守的士兵
     */
     /*
     Unit my_base = null;
     Unit my_Barracks=null;
     Unit enermy_base= null;
     */
     //数据说明 如果为 -1表示对数量没有限制
    // int BuildWorkersNum=0;
     //int HarvestWorkersNum=0;
     int maxBuildWorkersNum=1;
     int maxHarvestWorkersNum=2; //最多有两个农民用于采矿
     int leastWorkersNum =3;    //最少worker的数目
     int maxOffendWorkersNum=6; //rush农民数目 -1表示无限制
     int maxHeavyNum=-1; // 重甲兵的最大数目
     int maxLightNum=-1;  //轻甲兵的最大数目
     int maxRangedNum=-1; //远程兵的最大数目
      List<Unit> harvestWorkers = new ArrayList<>();
      List<Unit> buildWorkers = new ArrayList<>();
      Unit my_Base = null;
      Unit my_Barracks = null;
     //一些矩阵
     int m_battleWeight[][] ={    //战斗权系数矩阵,(单兵战斗力对比一致性矩阵)
        {1 ,-3,-5,-4},{3 ,1, 2, -2},{ 5 ,-2, 1, 2},{4 ,2, -2 ,1}};
     int m_AD_weight_V1[][]={      //我方在左上角的时候..
         { 4,2},{2,1}
     };
     int m_AD_weight_V2[][]={      //我方在右下角的时候..
         { 1,2},{2,4}
     };
    
    //和Qlearning 有关的参数
    Qlearning myQlearning = null;
    double alpha= 0.5;
    double r = 0.6;
    String QMatrixFileName = "QMatrix.txt";
    String rewardMatrixFileName = "rewardMatrix.txt";
    int timeStep=100;
    int lastTime=0;
    int action;
    int actionNum=8;
    int stateNum=100;
    int lastCombatValue=0; //上一个决策状态的战斗力，初始化为0
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
    String actionMajor[]={"Worker","Light","Ranged","Heavy","Worker","Light","Ranged","Heavy"};
    
    public MyRtsAi(UnitTypeTable a_utt) {
       this(a_utt, new AStarPathFinding());
    }
    public MyRtsAi(UnitTypeTable a_utt,PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }
    
    public AI clone() {
        return new MyRtsAi(m_utt,pf);
    }
    public void reset() {
          super.reset();
     }
    public void reset(UnitTypeTable a_utt) {
        myQlearning =new Qlearning(actionNum,stateNum,alpha,r,".\\"+QMatrixFileName,".\\"+rewardMatrixFileName);
        m_utt=a_utt;
        workerType = m_utt.getUnitType("Worker");
        baseType = m_utt.getUnitType("Base");
        barracksType = m_utt.getUnitType("Barracks");
        lightType = m_utt.getUnitType("Light");
        heavyType =m_utt.getUnitType("Heavy");
        rangedType=m_utt.getUnitType("Ranged");
    }

    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        System.out.println("time"+gs.getTime());
        //获取战场的信息
        if(my_Base!=null && my_Base.getHitPoints()<=0){
            my_Base = null;
        }
        if(my_Barracks !=null &&my_Barracks .getHitPoints()<=0){
            my_Barracks  = null;
        }
        int harvestNum=0;//
        
        //统计当前harvestWorkers数组里实际能工作的农名数
        for(Unit u : harvestWorkers){
            if(u!=null && u.getHitPoints()>0){
                harvestNum++;
            }
        }
       // List<Unit> harvestWorkers = new ArrayList<>();
       int buildWorkersNum = 0 ;
       boolean isBuild ;
       if(gs.getTime()>=50){
           isBuild = true;}
       else{
           isBuild=false;
       }
        int workersNum =0;
        //统计农民的数目，农民数目至少为3个
        //统计buildWorkers数据
        for(Unit u :buildWorkers){
            if(u!=null && u.getHitPoints()>0){
                buildWorkersNum++;
            }
        }
        if( my_Barracks !=null){
            System.out.println("我方有兵营了，开始削减建筑工人的数目"+buildWorkersNum);
            buildWorkers.add(null);
            for(Unit u :buildWorkers){
                if(u!=null && u.getHitPoints()>0){
                 buildWorkers.remove(u);
                 isBuild= false;
                 buildWorkersNum--;
                }
            }
        }
        List<Unit> warriorUnits = new ArrayList<>();
        List<Unit> enermyUnits = new ArrayList<>();
        List<Unit> evlourUnits = new ArrayList<>();  //用于评估的我方单位

        Unit enermy_Base = null;
        Unit evl_myBase=null; //用于评估的my_base
        Unit enermy_Barracks=null;
        //对战场所有单位进行分类
          for(Unit u: pgs.getUnits()){
              if(u!=null ){
                  if(u.getPlayer() == player ){
                      if(u.getType()!=baseType && u.getType().canAttack){
                          evlourUnits .add(u);
                      }else if(u.getType()==baseType){
                          evl_myBase=u;
                      }else if(u.getType()== barracksType){
                           enermy_Barracks=u;
                      }
                  }
              }
          }
        for(Unit u: pgs.getUnits()){
            if(u!=null ){
                  if(u.getPlayer() == player && u.getType()==baseType){
                      evl_myBase=u;
                  }
                  if(u.getPlayer() == player && gs.getActionAssignment(u)==null ){   //我方的单位
                      if(u.getType() == baseType){   // 基地
                          my_Base = u;
                      }else if(u.getType()== barracksType){   //兵营
                          my_Barracks = u; 
                      }else if(u.getType() == workerType && u.getType().canHarvest ){
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
                        //  if(!harvestWorkers.contains(u)){
                               warriorUnits.add(u);
                         // }
                          System.out.println(warriorUnits.size());
                      }
             }else if(u.getPlayer() != player){      //敌方单位
                 if(u.getType() == baseType){
                      enermy_Base =u ;
                 }else if(u.getPlayer()>=0 && u.getPlayer()!=p.getID()){
                      enermyUnits.add(u);
                 }
             }
             }
        }
   
        System.out.println("我方的大小"+evlourUnits.size());
        System.out.println("敌方的大小"+enermyUnits.size());
        int battleState[]=this.evaluateState(evl_myBase,evlourUnits, enermy_Base, enermyUnits, 0.4f ,4);
        int majorUnitType[] = this.evaluateEnermyMajorUnit(pgs, p, 6, 3);
        int enermy_AD_Type=this.evaluate_AD_Tactics(pgs, p,1.2f );
        //开始Q-learning 
        //获取majorUnitType
        int majorUnit=-1;
        if(majorUnitType[0]==4){
            majorUnit=3+majorUnitType[1];
        }else{
            majorUnit=majorUnitType[0];
        }
        //获取state
        int state=0;
        if(majorUnitType[0]==-1 || battleState[0]==-1|| enermy_AD_Type==-1){
            state=20;
        }else{
           state =  this.offsetSituation[battleState[0]] + this.offsetD_A[enermy_AD_Type] +this.offsetMajor[majorUnit];
        }
        //开局设一个单独的状态
        System.out.println("当前的状态"+state);
        if(gs.getTime()==551||gs.getTime()-lastTime>=timeStep){
            lastTime= gs.getTime();
            System.out.println("训练 lastTime"+lastTime);
            if(gs.getTime()>=550){
                if(battleState[1]!=-1000){
                     action=myQlearning.learning(state,battleState[1]-lastCombatValue, true);
                }else{
                     action=myQlearning.learning(state,battleState[1], true);
                }
            }
            //将当前战斗力存储下来
            System.out.println("本次的rewardv "+(battleState[1]-lastCombatValue));
            lastCombatValue=battleState[1];
            myQlearning.printLastS_A();
        }else if( gs.getTime()<=550){
            this.workersBehavior(harvestWorkers,buildWorkers, p, pgs,isBuild,1,4,3);
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
                 this.workersBehavior(harvestWorkers,buildWorkers, p, pgs, isBuild,1,4,3);
                this.rushTactics(gs, player, ourMajor, my_Barracks, warriorUnits, enermyUnits, 2,ourA_D);
            }else{
                 this.workersBehavior(harvestWorkers,buildWorkers, p, pgs, isBuild,1,4,3);
                 this.rushTactics(gs, player, ourMajor, my_Base, warriorUnits, enermyUnits, 2,ourA_D);
            }
       }
        //辅助信息
        switch(majorUnitType[0]){
            case 0:  System.out.println("以Worker为主");break;
            case 1:    System.out.println("以Light为主");break;
            case 2:   System.out.println("以Ranged为主");break;
            case 3:    System.out.println("以Heavy为主");break;
            case 4:    System.out.println("混合兵种");break;
            default:  System.out.println("判断主要兵种出错");
        }
        switch(enermy_AD_Type){
            case 1:  System.out.println("敌方进攻");break;
            case 0:  System.out.println("敌方防守");break;
            default:  System.out.println("判断攻守失败");
        }
        enermyUnits.add(enermy_Base);
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
    
    /*
    框架说明，为了更好的管理整个AI的行动，仿照lightrush将各个建筑，的行为进行封装，我的想法是决策的关键在于比例...（农民的比例和各个兵种的比例)
    */
    /*
    基地的行为，说明：基地主要是为了训练农民，训练农民主要有两个目的 1 收集资源 2 rush
    参数说明 input:前面三个参数 必要的，最后一个参数 1 表示训练收获的农民，2表示训练rush的农民 3表示不区分训练农民（先训练收获的农民，再训练rush的农民）
    */
    
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
        if (closestEnemy!=null && (closestDistance < pgs.getHeight()/4 || mybase < pgs.getHeight()/4)) {
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