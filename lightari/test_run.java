 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import ai.core.AI;
import ai.*;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import gui.PhysicalGameStatePanel;

import javax.swing.JFrame;

import agentP.AgentP;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

import ai.coac.CoacAI;
import ai.competition.rojobot.Rojo;
import ai.competition.IzanagiBot.Izanagi;
import ai.JZ.MixedBot;
import ai.competition.tiamat.Tiamat;
import mayariBot.mayari;
import lightari.lightari; //My Bot

 /**
 *
 * @author santi
 */
class GameVisualSimulationTest {
    public static void main(String[] args) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 5000;
        int PERIOD = 20;
        boolean gameover = false;
        
        AI ai1 = new lightari(utt);
        AI ai2 = getBotFromArg(args, utt);

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
        
        System.out.println("Game Over");
    }

    public static AI getBotFromArg(String[] args, UnitTypeTable utt) {
        if (args.length == 1) { 
            System.out.printf("=== Lightari vs %s ===", args[0]);
            switch (args[0].toLowerCase()) {
                case "mayari": return new mayari(utt);
                case "lightari": return new lightari(utt);
                case "workerrush": return new WorkerRush(utt, new BFSPathFinding());
                case "randombiased": return new RandomBiasedAI();
                case "coacai": return new CoacAI(utt);
                case "rojo": return new Rojo(utt);
                case "izanagi": return new Izanagi(utt);
                case "mixedbot": return new MixedBot(utt);
                case "tiamat": return new Tiamat(utt);
                case "agentp": return new AgentP(utt);
                default: System.out.print("Error: Invalid Bot Name. Defaulting to Mayari");
            };
        }
        return new mayari(utt); //Default
    }

}
