package Servidor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.ObjectOutputStream;
import java.io.IOException;
import Estrutura.Message;
import Estrutura.Player;
import Estrutura.Question;
import Estrutura.Team;
import Servidor.ModifiedCountDownLatch;
import Servidor.Barrier;

public class GameState {

	private final String roomCode;
	private final List<Question> questions;
	private int currentQuestion = 0;
	private ModifiedCountDownLatch currentLatch;
	private final int numTeams;
	private final int numTeamPlayers;
	private Barrier currentBarrier;
	private final Map<String, Team> teams = new HashMap<>();
	private Map<Player, Integer> playersAnswers = new HashMap<>();
	private Map<String, Integer> scoreboard = new HashMap<>();
	private final List<ObjectOutputStream> outputStreams = new ArrayList<>();
	private Map<String, Integer> currentRoundAnswers = new ConcurrentHashMap<>();
	private int connectedPlayers = 0;
	private final int totalPlayersExpected;


	public GameState(String roomCode, int numTeams, int numTeamPlayers, List<Question> questions) {

		this.roomCode = roomCode;
		this.numTeams = numTeams;
		this.numTeamPlayers = numTeamPlayers;
		this.questions = questions;
		// a considerar
		this.totalPlayersExpected = numTeams * numTeamPlayers;

	}

	public Map<String, Team> getTeams() {
		return teams;
	}

	public List<Question> getQuestions() {
		return questions;
	}

	public Map<Player, Integer> getPlayersAnswers() {
		return playersAnswers;
	}

	public Map<String, Integer> getScoreboard() {
		return scoreboard;
	}

	public String getRoomCode() {
		return roomCode;
	}

	public Question getCurrentQuestion() {
		return questions.get(currentQuestion);
	}

	public void nextQuestion() {
		if (currentQuestion < questions.size() - 1) {
			currentQuestion++;
		}
	}

	public boolean isTeamFull(Team team) {
		return team.getNumPlayers() == numTeamPlayers;
	}

	public boolean reachedTeamLimit() {
		return teams.size() == numTeams;
	}

	public void addConnectedPlayers() {
		connectedPlayers++;
	}

	public synchronized boolean areAllPlayersConnected() {
		return connectedPlayers == totalPlayersExpected;
	}
	
	public synchronized Player getPlayer(String username) {
	    for (Team t : teams.values()) {
	        for (Player p : t.getPlayers()) {
	            if (p.getName().equals(username)) {
	                return p;
	            }
	        }
	    }
	    return null;
	}


	public List<String> getAllUsernames() {
		List<String> list = new ArrayList<>();

		for (Team t : teams.values()) {
			for (Player p : t.getPlayers()) {
				list.add(p.getName());
			}
		}
		return list;
	}

	// Método para registar um novo canal de output
    public synchronized void addPlayerStream(ObjectOutputStream out) {
        outputStreams.add(out);
    }

    // Método para enviar mensagem a TODOS os jogadores deste jogo
    public synchronized void broadcast(Message msg) {
        for (ObjectOutputStream out : outputStreams) {
            try {
                out.writeObject(msg);
                out.reset(); // Importante para evitar cache de objetos repetidos
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean repeatedUsername(String username){
    	List<String> usernames= getAllUsernames();
    	for (String u : usernames) {
			if (u.equals(username))
				return true;
		}
    	return false;
    }
    
    
    public synchronized LoginResult addTeamAndPlayer(ObjectOutputStream out, String teamName, String username){
    	if (repeatedUsername(username)) {
            return LoginResult.USERNAME_EXISTS;
        }
		Team team = teams.get(teamName);
		if (team == null) {
			if (reachedTeamLimit()) {
				 return LoginResult.TEAM_LIMIT_REACHED;
			}
			team = new Team(teamName);
			teams.put(teamName, team);
		} else {
			if (isTeamFull(team)) {
				return LoginResult.TEAM_FULL;
			}
		}

		Player newPlayer = new Player(username);
		team.addPlayer(newPlayer);
		

		
		addPlayerStream(out);

		addConnectedPlayers();
		
		return LoginResult.OK;
		
    }

    public synchronized ObjectOutputStream getPlayerStream(String username) {
        Player p = getPlayer(username);
        if (p != null) {
            return null; 
        }
        return null;
    }

    public synchronized void sendToPlayer(ObjectOutputStream targetOut, Message msg) {
        try {
            targetOut.writeObject(msg);
            targetOut.reset();
            targetOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public void runGame() {
        try {
            System.out.println("Início do Ciclo de Jogo [" + roomCode + "]");
            Thread.sleep(2000);

            for (Question q : questions) {
                currentQuestion = questions.indexOf(q);
                
                // --- ADICIONAR: Limpar respostas antigas ---
                currentRoundAnswers.clear(); 
                // ------------------------------------------

                broadcast(new Message(Message.Type.QUESTION, q, "Server"));

                if (q.isIndividualQuestion()) {
                    System.out.println(">>> Ronda Individual");
                    currentLatch = new ModifiedCountDownLatch(2, Math.max(1, connectedPlayers / 2), 30, connectedPlayers);
                    currentLatch.await(); 
                    currentBarrier = null;
                } else {
                    System.out.println(">>> Ronda de Equipa");
                    currentBarrier = new Barrier(connectedPlayers + 1);
                    currentLatch = null; 
                    
                    // Servidor espera na barreira (35s timeout)
                    currentBarrier.await(35); 
                    
                    // --- ADICIONAR: Calcular pontos da equipa aqui ---
                    calculateTeamScores(q);
                    // ------------------------------------------------
                }

                broadcast(new Message(Message.Type.SCORE_UPDATE, new HashMap<>(scoreboard), "Server"));
                Thread.sleep(3000);
            }
            broadcast(new Message(Message.Type.END_GAME, "O jogo terminou!", "Server"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

	// --- ADICIONAR ESTE MÉTODO NOVO ---
    private synchronized void calculateTeamScores(Question q) {
        for (Team team : teams.values()) {
            int correctCount = 0;
            
            // Verificar quantos acertaram na equipa usando o mapa currentRoundAnswers
            for (Player p : team.getPlayers()) {
                if (currentRoundAnswers.containsKey(p.getName())) {
                    int ans = currentRoundAnswers.get(p.getName());
                    if (ans == q.getCorrect()) {
                        correctCount++;
                    }
                }
            }
            
            int teamPoints = 0;
            // Regra: Todos acertam = Dobro. Pelo menos um acerta = Normal.
            if (correctCount == team.getNumPlayers() && team.getNumPlayers() > 0) {
                teamPoints = q.getPoints() * 2;
                System.out.println("Equipa " + team.getTeamName() + ": TODOS acertaram! (Bónus)");
            } else if (correctCount > 0) {
                teamPoints = q.getPoints();
                System.out.println("Equipa " + team.getTeamName() + ": Pelo menos um acertou.");
            }
            
            // Atribuir pontos a todos os membros da equipa
            if (teamPoints > 0) {
                for (Player p : team.getPlayers()) {
                    p.setScore(p.getScore() + teamPoints);
                    scoreboard.put(p.getName(), p.getScore());
                }
            }
        }
    }

    public synchronized int submitAnswer(String username, int answerIndex) {
        Player player = getPlayer(username);
        if (player == null) return 0;

        Question curQ = getCurrentQuestion();
        
        // --- ADICIONAR: Guardar a resposta ---
        currentRoundAnswers.put(username, answerIndex);
        // -------------------------------------

        int pointsAwarded = 0; 

        if (curQ.isIndividualQuestion()) {
            // Lógica Individual (igual ao que tinhas)
            if (answerIndex == curQ.getCorrect()) {
                int points = curQ.getPoints();
                if (currentLatch != null) {
                    int bonus = currentLatch.countDown();
                    points *= bonus;
                }
                pointsAwarded = points;
                player.setScore(player.getScore() + points);
                scoreboard.put(username, player.getScore()); 
            } else {
                if (currentLatch != null) currentLatch.countDown(); 
            }
            return pointsAwarded;
            
        } else {
			if (answerIndex == curQ.getCorrect()) {
                System.out.println(">> [Equipa] O jogador " + username + " ACERTOU! (Aguardar outros...)");
            } else {
                System.out.println(">> [Equipa] O jogador " + username + " ERROU! (Aguardar outros...)");
            }
			if (currentBarrier != null) {
                new Thread(() -> {
                    try { currentBarrier.await(35); } catch (InterruptedException e) {}
                }).start();
            }
			return -1; 
        }
    }
}
