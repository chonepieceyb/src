 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import QLAi.QLAi;
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
import bottom.Terminator;
import bottom.bottom;
import gui.PhysicalGameStatePanel;
import java.io.OutputStreamWriter;
import javax.swing.JFrame;
import mymicrorts.Simulate;
import mymicrorts.test;
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
        int j=9;
        int L=0;
        do{           
        do{
            //System.out.println("     J:"+j);
            System.out.println("     K:"+k);
        UnitTypeTable utt = new UnitTypeTable();
       AStarPathFinding aps= new AStarPathFinding();
        PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 3000;
        int PERIOD =10;
        boolean gameover = false;
        System.err.println("K（赢得）的值为\t"+k);
           System.err.println("L（输的）的值为\t"+L);
         System.err.println("J的值为\t"+j);
        AI ai2= new MyRtsAi(utt);  
       // AI ai1=new FirstRush(utt);
        AI ai1= null;
        switch(j%10)
        {
            case 0:
                ai1= new CRush_V2(utt);
                break;
            case 1:
                ai1 = new CRush_V1(utt);
                break;
            case 2:
                ai1 = new LightRush(utt);
                break;
            case 3:
                ai1 = new HeavyRush(utt);
                break;
            case 4:
                ai1 = new FirstRush(utt);
                break;
            case 5:
                ai1 = new LightDefense(utt);
                break;
            case 6:
                ai1 = new HeavyDefense(utt);
                break;
            case 7:
                ai1= new bottom(utt);
                break;
            case 8:
                ai1 = new Simulate(utt);
                break;
            case 9:
                ai1= new QLAi(utt);break;
                
                            
        }
       // AI ai2 = new RandomBiasedAI();

     JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_BLACK);
      //JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

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
        if(gs.winner()==1){
           k++;
           L=0;
        }
        else{
           L++;
           k=0;
        }
        System.out.println("Game Over");
        w.dispose();
         }while(k<10&&L<10);//(k%10!=0||k==0);
         j++;
        }while(k!=100);
    }    
       
}