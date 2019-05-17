package myrtsai;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.cRush.RangedAttack;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;


/**
 * 
 * @author yang
 * modify by CRush_V1
 */
public class FirstRush extends AbstractionLayerAI{

	Random random;
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType rangedType;
    UnitType heavyType;
    UnitType lightType;
    int baseX;
    int baseY;
    
    private static final int DEFENSE_DISTANCE = 8;
    
    boolean buildingRacks = false;
    int resourcesUsed = 0;
    
    public FirstRush(UnitTypeTable a_utt) {
    	super(new AStarPathFinding());
        reset(a_utt);
        baseX = 0;
        baseY = 0;
        random = new Random();
    }
    


	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        

        List<Unit> workers = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) {
                workers.add(u);
            }
        }
        // behavior of workers
        workersBehavior(workers, p, pgs, gs);

        // behavior of bases:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
            		if(baseX != u.getX() || baseY != u.getY()) {
            			baseX = u.getX();
            			baseY = u.getY();
            		}
                    baseBehavior(u, p, pgs);
            }
        }

        // behavior of barracks:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs);
            }
        }

        // behavior of melee units:
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                if (u.getType() == rangedType) {
                    rangedUnitBehavior(u, p, gs);
                } else {
                    meleeUnitBehavior(u, p, gs);
                }
            }
        }

        for (Unit u: pgs.getUnits()) {
        	if (u.getType().canAttack && !u.getType().canHarvest
        			&& u.getPlayer() == player
        			&& gs.getActionAssignment(u) == null) {
        		if (u.getType() == lightType) {
        			lightUnitBehavior(u, p, gs);
        		}
        	}
        }
        return translateActions(player, gs);
	}

    private void rangedUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        Unit closestRacks = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
            if (u2.getType() == barracksType && u2.getPlayer() == p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestRacks == null || d < closestDistance) {
                    closestRacks = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
            rangedAttack(u, closestEnemy, closestRacks);
        }		
	}



	private void rangedAttack(Unit u, Unit target, Unit racks) {
	    actions.put(u, new RangedAttack(u, target, racks, pf));
	}



	private void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {

        int nbases = 0;
        int nbarracks = 0;
        int nworkers = 0;
        int resources = p.getResources();

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                nbases++;
            }
        }
        
        if (nworkers < (nbases + 1) && p.getResources() >= workerType.cost) {
            train(u, workerType);
        }

        //Buffers the resources that are being used for barracks
        if (resourcesUsed != barracksType.cost * nbarracks) {
            resources = resources - barracksType.cost;
        }

        if (buildingRacks && (resources >= workerType.cost + rangedType.cost)) {
            train(u, workerType);
        }
	}



	private void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs, GameState gs) {
        int nbases = 0;
        int nbarracks = 0;
        int nworkers = 0;
        resourcesUsed = 0;
        
        List<Unit> freeWorkers = new LinkedList<Unit>();
        List<Unit> battleWorkers = new LinkedList<Unit>();

        int closeEnemyDistance = 999;
        
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                nbases++;
            }
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
            	int d = Math.abs(u2.getX() - baseX) + Math.abs(u2.getY() - baseY);
            	if (d < closeEnemyDistance) {
            		closeEnemyDistance = d;
            	}
            }
        }

        if (workers.size() > (nbases + 1)
        		&& closeEnemyDistance <= DEFENSE_DISTANCE 
        		|| workers.size()>3 ) {
            for (int n = 0; n < 3; n++) {
            	if (workers.isEmpty()) break;
                freeWorkers.add(workers.get(0));
                workers.remove(0);
            }
            battleWorkers.addAll(workers);
        } else {
            freeWorkers.addAll(workers);
        }

        if (workers.isEmpty()) {
            return;
        }
        
//        System.out.println(battleWorkers.size()+" "+freeWorkers.size());
        
        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (nbases == 0 && !freeWorkers.isEmpty()) {
            // build a base:
            if (p.getResources() >= baseType.cost) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
                //resourcesUsed += baseType.cost;
            }
        }
        if ((nbarracks == 0) && (!freeWorkers.isEmpty()) && nworkers > 1
                && p.getResources() >= barracksType.cost) {
            
            int resources = p.getResources();
            Unit u = freeWorkers.remove(0);
            buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY()+3*(p.getID()==0?1:0),reservedPositions,p,pgs);
            resourcesUsed += barracksType.cost;
            buildingRacks = true;
                
                //The problem with this right now is that we can only track when a build command is sent
                //Not when it actually starts building the building.
        } else {
            resourcesUsed =  barracksType.cost * nbarracks;
        }
        
        if (nbarracks > 1) {
            buildingRacks = true;
        }
        // TODO: battleWorker's behavior can be changed
        for (Unit u : battleWorkers) {
            meleeUnitBehavior(u, p, gs);
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
                if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
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
                    Harvest h_aa = (Harvest) aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {
                        harvest(u, closestResource, closestBase);
                    }
                } else {
                    harvest(u, closestResource, closestBase);
                }
            }
        }
	}


	private void lightUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        Unit closestRanged = null;
        Unit closestLight = null;
        int allyDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
//            if (u2.getPlayer() >= 0 
//            		&& u2.getPlayer() == p.getID()
//            		&& u2.getType() == rangedType) {
//            	int rad = Math.abs(u2.getX() - u.getX())+Math.abs(u2.getY() - u.getX());
//            	if (rad < allyDistance || closestRanged == null) {
//					closestRanged = u2;
//					allyDistance = rad;
//				}
//			}
//            if (u2.getPlayer() >= 0 
//            		&& u2.getPlayer() == p.getID()
//            		&& u2.getType() == rangedType) {
//            	int lad = Math.abs(u2.getX() - u.getX())+Math.abs(u2.getY() - u.getX());
//               	if (lad < allyDistance || closestRanged == null) {
//    					closestLight = u2;
//    					allyDistance = lad;
//    			}
//            }
           
        }
		if (closestEnemy != null) {
			attack(u, closestEnemy);
			return;
		}
	}



	private void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
		
		int nlights = 0;
		int nranged = 0;
		
		for(Unit u2: pgs.getUnits()) {
			if (u2.getPlayer() == p.getID() && u2.getType() == lightType) {
				nlights++;
			}
			if (u2.getPlayer() == p.getID() && u2.getType() == rangedType) {
				nranged++;
			}
		}

		if ((nlights < 2 || nranged >=4)  && p.getResources() >= lightType.cost) {
			train(u, lightType);
			return;
		}
//		 produce ranged
		if (p.getResources() >= rangedType.cost) {
         	train(u, rangedType);
		}
//		if(p.getResources() >= lightType.cost) {
//			train(u, lightType);
//		}
	}



	private void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
            attack(u, closestEnemy);
        }
    }
    
	@Override
	public AI clone() {
		return new FirstRush(utt);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
        return parameters;
	}

    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        rangedType = utt.getUnitType("Ranged");
        lightType = utt.getUnitType("Light");
    }
}
