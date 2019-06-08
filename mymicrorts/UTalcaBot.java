/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mymicrorts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ai.abstraction.EMRDeterministico;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.SimpleEconomyRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.WorkerRushPlusPlus;
import ai.abstraction.cRush.CRush_V1;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.portfolio.portfoliogreedysearch.UnitScript;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

public class UTalcaBot extends AIWithComputationBudget {

	private AI ai=null;
	private UnitTypeTable m_utt;
	
	public UTalcaBot(UnitTypeTable utt) {
        super(-1,-1);
        m_utt = utt;
	}

	@Override
	public void reset() {
		setAi(null);

	}

        	@Override
	public void preGameAnalysis(GameState gs, long milliseconds) throws Exception
	{
		PhysicalGameState pgs=gs.getPhysicalGameState();
		Random rand=new Random();
		if(pgs.getUnits().stream().anyMatch(u -> u.getType()==getM_utt().getUnitType("Barracks"))) {//starting barracks
			setAi(new HeavyRush(getM_utt()));
			return ;
		}
		if(pgs.getHeight()*pgs.getWidth()<=64) {//8x8 or smaller			
			setAi(new WorkerRushPlusPlus(getM_utt()));			
			return ;
		}
		if(pgs.getHeight()==8 && pgs.getWidth()==9) {//NoWhereToRun9x8
			setAi(new SimpleEconomyRush(getM_utt()));
			return ;
		}
                if(pgs.getHeight()*pgs.getWidth()<=144) {//12x12 or smaller
                        //ai=new LightRush(m_utt);
			setAi(new CRush_V1(getM_utt()));
			return ;
		}
		if(pgs.getHeight()*pgs.getWidth()<=256) {//16x16 or smaller
                        //ai=new LightRush(m_utt);
			setAi(new CRush_V1(getM_utt()));
			return ;
		}
		if(pgs.getHeight()==24 && pgs.getWidth()==24 &&
				pgs.getTerrain(0, 11)==pgs.TERRAIN_WALL &&
				pgs.getTerrain(0, 12)==pgs.TERRAIN_WALL&&
				pgs.getTerrain(1, 11)==pgs.TERRAIN_WALL&&
				pgs.getTerrain(1, 12)==pgs.TERRAIN_WALL&&
				pgs.getTerrain(2, 11)==pgs.TERRAIN_WALL &&
				pgs.getTerrain(2, 12)==pgs.TERRAIN_WALL&&
				pgs.getTerrain(3, 11)==pgs.TERRAIN_WALL&&
				pgs.getTerrain(3, 12)==pgs.TERRAIN_WALL) {//DoubleGame24x24
			if(rand.nextBoolean()) {
				setAi(new WorkerRushPlusPlus(getM_utt()));
			}else {
				setAi(new WorkerRush(getM_utt()));
			}
			return ;
		}
		if(pgs.getHeight()*pgs.getWidth()<=1024) {//32x32 or smaller
			setAi(new LightRush(getM_utt()));
			return ;
		}
		setAi(new EMRDeterministico(getM_utt()));//for bigger maps
	}
        
        public void Runsimulate(int player, AI p1, AI p2, GameState gs) throws Exception
        {
            Simulate sim=new Simulate(getM_utt());
            List<Unit> playerUnits = new ArrayList<>();
            List<Unit> enemyUnits = new ArrayList<>();

            for(Unit u:gs.getUnits()) {
                if (u.getPlayer()==player) playerUnits.add(u);
                else if (u.getPlayer()>=0) enemyUnits.add(u);
            }
            setAi(sim.improve2(player, p1, enemyUnits, p2, playerUnits, gs));
        }
        
        public void print()
        {
            System.out.println("");
                   
        }
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception 
        {
            return getAi().getAction(player, gs);
        //  pa.fillWithNones(gs, player, 10);
	}

	 
	@Override
	public AI clone() {
		UTalcaBot bot=new UTalcaBot(getM_utt());
                bot.setAi(getAi());
		return bot;
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		 return new ArrayList<>();
	}

    /**
     * @return the ai
     */
    public AI getAi() {
        return ai;
    }

    /**
     * @param ai the ai to set
     */
    public void setAi(AI ai) {
        this.ai = ai;
    }

    /**
     * @return the m_utt
     */
    public UnitTypeTable getM_utt() {
        return m_utt;
    }

    /**
     * @param m_utt the m_utt to set
     */
    public void setM_utt(UnitTypeTable m_utt) {
        this.m_utt = m_utt;
    }

}

