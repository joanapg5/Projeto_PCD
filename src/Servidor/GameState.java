package Servidor;

import java.util.*;
import java.io.ObjectOutputStream;
import java.io.IOException;
import Estrutura.Message;
import Estrutura.Player;
import Estrutura.Question;
import Estrutura.Team;

public class GameState {

	private final String roomCode;
	private final List<Question> questions;
	private int currentQuestion = 0;

	private final int numTeams;
	private final int numTeamPlayers;
	// private final int numQuestions; //a considerar

	// o mapa das teams e p facilitar a busca
	private final Map<String, Team> teams = new HashMap<>();
	private Map<Player, Integer> playersAnswers = new HashMap<>();
	private Map<String, Integer> scoreboard = new HashMap<>();
	private final List<ObjectOutputStream> outputStreams = new ArrayList<>(); //para registar os canais de escrita de todos os jogadores
	// cronometro?
	// a considerar
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

}
