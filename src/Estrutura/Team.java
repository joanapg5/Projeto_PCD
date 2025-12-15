package Estrutura;

import java.util.List;
import java.util.ArrayList;

public class Team {

	private final String teamName;
	private final List<Player> players = new ArrayList<>();

	public Team(String teamName) {
		this.teamName = teamName;
	}

	

	public void addPlayer(Player player) {
		this.players.add(player);
	}

	public String getTeamName() {
		return teamName;
	}

	public List<Player> getPlayers() {
		return players;
	}

	public int getNumPlayers() {
		return players.size();
	}

	
}
