package Servidor;

import java.util.HashMap;
import java.util.List;

import Estrutura.Message;
import Estrutura.Question;

public class GameThread extends Thread{ //Thread do jogo : executa do ciclo do jogo
	private GameState game;
	
	
	public GameThread(GameState game){
		this.game=game;
	}
	
	@Override
	public void run() {
		try {
            System.out.println("Inicio do Ciclo de Jogo [" + game.getRoomCode() + "]");
            Thread.sleep(2000);
            List<Question> questions=game.getQuestions();

            for (Question q : questions) {
            	if (game.getConnectedPlayers() < 1) {
                    System.out.println("Jogo abortado. Jogadores insuficientes.");
                    game.broadcast(new Message(Message.Type.END_GAME, "O jogo terminou!", "Server"));
                    return; //termina a game thread
            	}
                game.setCurrentQuestion(questions.indexOf(q));
                
                game.cleanAnswers();

                game.broadcast(new Message(Message.Type.QUESTION, q, "Server"));

                if (q.isIndividualQuestion()) {
                	game.setBarrier(null);
                    System.out.println(">>> Ronda Individual");
                    game.setLatch(new ModifiedCountDownLatch(2, 2, 30, game.getConnectedPlayers()));
                    game.getLatch().await(); //espera que todos os jogadores respondam
                    
                } else {
                	game.setLatch(null);
                    System.out.println(">>> Ronda de Equipa");
                    game.setBarrier(new Barrier(game.getConnectedPlayers() + 1));
                    game.getBarrier().await(35); //espera que todos os jogadores respondam
                    game.calculateTeamScores(q);
                }

                game.broadcast(new Message(Message.Type.SCORE_UPDATE, new HashMap<>(game.getScoreboard()), "Server"));
                Thread.sleep(3000);
            }
            game.broadcast(new Message(Message.Type.END_GAME, "O jogo terminou!", "Server"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
		
	}

}
