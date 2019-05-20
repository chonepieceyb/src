 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import ai.core.AI;
import ai.*;
import ai.abstraction.*;
import ai.abstraction.cRush.CRush_V1;
import ai.abstraction.cRush.CRush_V2;
import ai.abstraction.partialobservability.POLightRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.mcts.uct.UCT;
import ai.montecarlo.MonteCarlo;
import ai.montecarlo.lsi.LSI;
import ai.portfolio.portfoliogreedysearch.PGSAI;
import ai.scv.SCV;
import gui.PhysicalGameStatePanel;
import java.io.OutputStreamWriter;
import javax.swing.JFrame;
import myrtsai.AI2;
import myrtsai.FirstRush;
import myrtsai.MyRtsAi;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import util.XMLWriter;

/**
 *
 * @author santi
 */
//防御被打进攻
//Range升级
//造兵营
public class GameVisualSimulationTest {
    public static void main(String args[]) throws Exception {
        int k=0;
        //int j=1;
        //do{           
        do{
            //System.out.println("     J:"+j);
            System.out.println("     K:"+k);
        UnitTypeTable utt = new UnitTypeTable();
       AStarPathFinding aps= new AStarPathFinding();
        PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 3000;
        int PERIOD = 15;
        boolean gameover = false;
        
        AI ai1 = new MyRtsAi(utt);  
        AI ai2 =new MyRtsAi(utt);
        /*switch(j%7)
        {
            case 0:
                ai2 = new CRush_V1(utt);
                break;
            case 1:
                ai2 = new CRush_V2(utt);
                break;
            case 2:
                ai2 = new LightRush(utt);
                break;
            case 3:
                ai2 = new HeavyRush(utt);
                break;
            case 4:
                ai2 = new FirstRush(utt);
                break;
            case 5:
                ai2 = new LightDefense(utt);
                break;
            case 6:
                ai2 = new HeavyDefense(utt);
                break;
        }*/
       // AI ai2 = new RandomBiasedAI();

        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

        long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
        do{
            if (System.currentTimeMillis()>=nextTimeToUpdate) {
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);

                // simulate:
                gameover = gs.cycle();
                w.repaint();
                nextTimeToUpdate+=PERIOD;
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }while(!gameover && gs.getTime()<MAXCYCLES);
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        k++;
        //if(gs.winner()==0)
        //    k++;
        //else
         //   k=0;
        System.out.println("Game Over");
        w.dispose();
         }while(k!=1000);//(k%10!=0||k==0);
        //j++;
        //}while(k!=70);
    }    
       
}