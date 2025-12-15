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
	private final int numTeams;
	private final int numTeamPlayers;
	private final Map<String, Team> teams = new HashMap<>();
	private Map<String, Integer> scoreboard = new HashMap<>();
	private final List<ObjectOutputStream> outputStreams = new ArrayList<>();
	private Map<String, Integer> currentRoundAnswers = new ConcurrentHashMap<>();
	private int connectedPlayers = 0;
	private final int totalPlayersExpected;
	
	private ModifiedCountDownLatch currentLatch;
	private Barrier currentBarrier;


	public GameState(String roomCode, int numTeams, int numTeamPlayers, List<Question> questions) {

		this.roomCode = roomCode;
		this.numTeams = numTeams;
		this.numTeamPlayers = numTeamPlayers;
		this.questions = questions;
		this.totalPlayersExpected = numTeams * numTeamPlayers;

	}

	public Map<String, Team> getTeams() {
		return teams;
	}

	public List<Question> getQuestions() {
		return questions;
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
	
	public void setCurrentQuestion(int index){
		this.currentQuestion=index;
	}


	
	public void cleanAnswers(){
		this.currentRoundAnswers.clear();
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
	
	public int getConnectedPlayers(){
		return connectedPlayers;
	}
	
	public void setBarrier(Barrier b){
		this.currentBarrier=b;
	}
	
	public Barrier getBarrier(){
		return currentBarrier;
	}
	
	public ModifiedCountDownLatch getLatch(){
		return currentLatch;
	}
	
	public void setLatch(ModifiedCountDownLatch l){
		this.currentLatch=l;
	}

	public synchronized boolean areAllPlayersConnected() {
		return connectedPlayers == totalPlayersExpected;
	}
	
	public Player getPlayer(String username) {
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

	
    public synchronized void addPlayerStream(ObjectOutputStream out) {
        outputStreams.add(out);
    }
    
    

    
    public synchronized void broadcast(Message msg) {
        for (ObjectOutputStream out : outputStreams) {
            try {
                out.writeObject(msg);
                out.reset(); 
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

    

    public synchronized void sendToPlayer(ObjectOutputStream targetOut, Message msg) {
        try {
            targetOut.writeObject(msg);
            targetOut.reset();
            targetOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	
	
    public synchronized void calculateTeamScores(Question q) {
        for (Team team : teams.values()) {
            int correctCount = 0;
            
            for (Player p : team.getPlayers()) {
                if (currentRoundAnswers.containsKey(p.getName())) {
                    int ans = currentRoundAnswers.get(p.getName());
                    if (ans == q.getCorrect()) {
                        correctCount++;
                    }
                }
            }
            
            int teamPoints = 0;
            if (correctCount == team.getNumPlayers() && team.getNumPlayers() > 0) {
                teamPoints = q.getPoints() * 2;
                System.out.println("Equipa " + team.getTeamName() + ": TODOS acertaram! (Bonus)");
            } else if (correctCount > 0) {
                teamPoints = q.getPoints();
                System.out.println("Equipa " + team.getTeamName() + ": Pelo menos um acertou.");
            }
            
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
        
        currentRoundAnswers.put(username, answerIndex);

        int pointsAwarded = 0; 

        if (curQ.isIndividualQuestion()) {
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
                new Thread(() -> { // thread para aguardar na barreira
                    try { currentBarrier.await(35); } catch (InterruptedException e) {}
                }).start();
            }
			return -1; 
        }
    }
    
    
    public synchronized void playerDisconnected(String username, ObjectOutputStream out) {
        if (out != null) {
            outputStreams.remove(out);
            System.out.println("Stream removida para o jogador " + username + ".");
        }
        
        Player p = getPlayer(username);
        if (p == null) return;
        
        System.out.println("A processar saida do jogador: " + username);
        boolean jaRespondeu = currentRoundAnswers.containsKey(username);
        
        Iterator<Team> teamIterator = teams.values().iterator();
        while (teamIterator.hasNext()) {
            Team t = teamIterator.next();
            if (t.getPlayers().contains(p)) {
                t.getPlayers().remove(p); //remove o jogador da equipa correspondente
                
                if (t.getPlayers().isEmpty()) { //se a equipa ficar vazia removemo-la
                    teamIterator.remove(); 
                    System.out.println("Equipa " + t.getTeamName() + " ficou vazia e foi removida.");
                }
                break;
            }
        }
        
      
        scoreboard.remove(username);
        
        if (connectedPlayers > 0) {
            connectedPlayers--;
        }

        if (currentLatch != null) {
        	if (!jaRespondeu) { //para nao atrasar o jogo na ronda em q saiu, faz countdown na mesma se ainda nao tinha respondido
                currentLatch.countDown();
                System.out.println("Latch decrementado por saida (sem resposta).");
           }
        }

        if (currentBarrier != null) {
        	if(!jaRespondeu){
	            new Thread(() -> { //thread para nao atrasar o jogo dps do cliente se desconectar se ainda nao tinha respondido
	                try { 
	                    currentBarrier.await(1); 
	                } catch (InterruptedException e) {
	                 
	                }
	            }).start();
        	}
        }
    }
	
}
