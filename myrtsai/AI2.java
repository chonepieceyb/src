/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package myrtsai;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
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
/**
 *
 * @author msi-
 */
public class AI2 extends AbstractionLayerAI{
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
     int maxHarvestWorkersNum=2; //最多有两个农民用于采矿
     int maxBuildWorkersNum=1;
     int maxOffendWorkersNum=-1; //rush农民数目 -1表示无限制
     int maxHeavyNum=-1; // 重甲兵的最大数目
     int maxLightNum=-1;  //轻甲兵的最大数目
     int maxRangedNum=-1; //远程兵的最大数目
     int HarvestWorkersNum=0;
     int BuildWorkersNum=0;
 
    public AI2(UnitTypeTable a_utt) {
       this(a_utt, new AStarPathFinding());
    }
    public AI2(UnitTypeTable a_utt,PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }
    
     public AI clone() {
        return new AI2(m_utt,pf);
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
        
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        //获取战场的信息
        int harvestNum=0;//
        List<Unit> harvestWorkers = new ArrayList<>();
        List<Unit> buildWorker = new ArrayList<>();
        List<Unit> warriorUnits = new ArrayList<>();
        List<Unit> enermyUnits = new ArrayList<>();
        Unit my_Base = null;
        Unit my_Barracks = null;
        Unit enermy_Base = null;
        //对战场所有单位进行分类
        for(Unit u: pgs.getUnits()){
            if(u!=null ){
                if(u.getPlayer() == player && gs.getActionAssignment(u)==null){   //我方的单位
                    if(u.getType() == baseType){   // 基地
                       // System.out.println(u.getType());
                        my_Base = u;
                    }else if(u.getType()== barracksType){   //兵营
                        my_Barracks = u; 
                    }else if(u.getType() == workerType && u.getType().canHarvest && buildWorker.size()<maxBuildWorkersNum&&harvestNum>0&&my_Barracks==null){
                        buildWorker.add(u);
                    }
                    else if( u.getType() == workerType && u.getType().canHarvest && harvestNum< maxHarvestWorkersNum){   //收获单位
                        harvestWorkers.add(u);
                        harvestNum++;
                        //System.out.println(harvestNum);
                    }else if (u.getType().canAttack){     //可作战单位
                        warriorUnits.add(u);
                        //System.out.println("###");System.out.println(warriorUnits.size());System.out.println("###");
                    }
                }else{      //地方单位
                    if(u.getType() == baseType){
                        enermy_Base =u ;
                    }else if(u.getPlayer()>=0 && u.getPlayer()!=p.getID()){
                        enermyUnits.add(u);
                    }
                }
            }
        }
        enermyUnits.add(enermy_Base);
        //System.out.println("###");
        //System.out.println(enermyUnits.size());
        //农民建筑和收获
        workersBehavior(harvestWorkers,buildWorker, p, pgs, true, 1);
        baseBehavior(my_Base, p, gs, 1);
        // 使用战略
        rushTactics(gs, player, "Light", my_Barracks, warriorUnits, enermyUnits, 2, 4);
        return translateActions(player, gs);
    }
    /*
    gs 和 Player同之前
    input :
    rushType:进攻策略 ，有 worker rush, heavy rush , light rush 和 range rush
    Unit trainBuilding 用来训练的单位,如果是worker rush就选择 base ,如果是 非worker rush 就选择 兵营，如果不选择训练 就 null
    ourUnits:我方的目前可用的单位(不包括农民，目前的设想是，这些单位是用于进攻的单位）
    enermyUnits: 敌方的单位
    rushTarget: 进攻目标 1是进攻基地，2进攻非采矿单位
    rushLevel : 进攻程度  0 1 2：轻微进攻（选 0.5倍兵力进攻） 3：强势进攻（选0.75倍兵力进攻） 4：全力进攻（所有的兵力都进攻）
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
        maxWarriorNum= (int)(((float)ourUnits.size())/4)*rushLevel;
        //System.out.println("####");System.out.println(maxWarriorNum);System.out.println("####");
        //根据rushType训练相应的单位,以及挑选相应的主进攻队伍,次进攻队伍,以及防御队伍
        for(Unit u:ourUnits){
           if(u.getType() == m_utt.getUnitType(rushType)){
                //有rushlevel决定，如果数目达到进攻上限，剩下的队伍转为防守
                if(warriorNum < maxWarriorNum){    
                    warriorUnits.add(u);
                    warriorNum++;
                    //System.out.println("##");System.out.println(warriorNum);System.out.println("##");
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
                  this.baseBehavior(trainBuilding, p, gs, 2);
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
             // System.out.println("敌方规模过小");
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
         for(Unit warrior:defendUnits){
             defenceBehavior(warrior,p,gs);
         }
    }    
    /*
    框架说明，为了更好的管理整个AI的行动，仿照lightrush将各个建筑，的行为进行封装，我的想法是决策的关键在于比例...（农民的比例和各个兵种的比例)
    */
    /*
    基地的行为，说明：基地主要是为了训练农民，训练农民主要有两个目的 1 收集资源 2 rush
    参数说明 input:前面三个参数 必要的，最后一个参数 1 表示训练收获的农民，2表示训练rush的农民 3表示不区分训练农民（先训练收获的农民，再训练rush的农民）
    */
     public void baseBehavior(Unit u, Player p, GameState gs,int trainType) {
         //先统计目前战场上的农民的数目
        PhysicalGameState pgs=gs.getPhysicalGameState();
        int allWorkers=0;
        int harvestWorkers = 0; 
        int offendWorkers =0;
        for (Unit u2:selectUnitsAround(gs, p.getID(), "Worker", 0, 0, pgs.getWidth(), pgs.getHeight(), -1, false)) {
            //说明，这里我不知道怎么确定自己的农民现在正在干啥。
            if(u2!=null){
                if (u2.getType().canHarvest&&(maxHarvestWorkersNum == -1||harvestWorkers<maxHarvestWorkersNum)) {
                    harvestWorkers++;
                }else if(u2.getType().canAttack){
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
                if(maxHarvestWorkersNum == -1 && p.getResources()>=workerType.cost){
                    train(u,workerType);
                }else if(harvestWorkers<maxHarvestWorkersNum && p.getResources()>=workerType.cost){
                     train(u,workerType);
                }
                if(maxOffendWorkersNum==-1 && p.getResources()>=workerType.cost){
                    train(u,workerType);
                }else if(offendWorkers< maxOffendWorkersNum && p.getResources()>=workerType.cost){
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
    input: 
    */
    public void workersBehavior(List<Unit>harvestWorkers,List<Unit> builedWorkers, Player p, PhysicalGameState pgs,boolean isBuild,int barracksNum) {
        int nbarracks = 0;
        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        freeWorkers.addAll(harvestWorkers);

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }

        List<Integer> reservedPositions = new LinkedList<Integer>();
        if(isBuild){
            if (nbarracks < barracksNum && !builedWorkers.isEmpty()) {
                // build a barracks:
                if (p.getResources() >= barracksType.cost + resourcesUsed) {
                    Unit u = builedWorkers.remove(0);
                    buildIfNotAlreadyBuilding(u,barracksType,13,16,reservedPositions,p,pgs);
                    resourcesUsed += barracksType.cost;
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
    
    //防守
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
        if (closestEnemy!=null && (closestDistance < pgs.getHeight()/2 || mybase < pgs.getHeight()/2)) {
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