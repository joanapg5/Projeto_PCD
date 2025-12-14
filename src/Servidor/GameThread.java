package Servidor;

import java.util.HashMap;
import java.util.List;

import Estrutura.Message;
import Estrutura.Question;

public class GameThread extends Thread{
	private GameState game;
	private ModifiedCountDownLatch currentLatch;
	private Barrier currentBarrier;
	
	public GameThread(GameState game){
		this.game=game;
	}
	
	@Override
	public void run() {
		try {
            System.out.println("Início do Ciclo de Jogo [" + game.getRoomCode() + "]");
            Thread.sleep(2000);
            List<Question> questions=game.getQuestions();

            for (Question q : questions) {
                game.setCurrentQuestion(questions.indexOf(q));
                
                // --- ADICIONAR: Limpar respostas antigas ---
                game.cleanAnswers();
                // ------------------------------------------

                game.broadcast(new Message(Message.Type.QUESTION, q, "Server"));

                if (q.isIndividualQuestion()) {
                    System.out.println(">>> Ronda Individual");
                    currentLatch = new ModifiedCountDownLatch(2, 2, 30, game.getConnectedPlayers());
                    currentLatch.await(); 
                    currentBarrier = null;
                } else {
                    System.out.println(">>> Ronda de Equipa");
                    currentBarrier = new Barrier(game.getConnectedPlayers() + 1);
                    currentLatch = null; 
                    
                    // Servidor espera na barreira (35s timeout)
                    currentBarrier.await(35); 
                    
                    // --- ADICIONAR: Calcular pontos da equipa aqui ---
                    game.calculateTeamScores(q);
                    // ------------------------------------------------
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
