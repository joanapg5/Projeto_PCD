package Servidor;

import Estrutura.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
	public static final int PORT = 2025;

	private ServerSocket server;
	private Map<String, GameState> games = new ConcurrentHashMap<>(); 
	private GameThreadPool gamePool = new GameThreadPool(5);


	public void runServer() {
		try {
			server = new ServerSocket(PORT);
			new Thread(new Runnable() {
				@Override
				public void run() {
					Scanner sc = new Scanner(System.in);
					while (true) {
						System.out.print("> ");
						String cmd = sc.nextLine();
						processCommand(cmd); //processa o comando new <numequipas > < numjogadoresporequipa > < numperguntas >

					}

				}
			}).start();
			while (true) {
				waitForConnection(); //espera que um cliente se conecte

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (server != null) {
				try {
					server.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void waitForConnection() throws IOException {
		Socket connection = server.accept();
		DealWithClient handler = new DealWithClient(connection);
		handler.start();
		System.out.println("Comecada nova conexao...");
	}
	
	public String createCode() {
		Random random = new Random();
		String code = "";
		while (true) { 
			int codeSize = 4;
			for (int i = 0; i < codeSize; i++) {
				int n = (int) (Math.random() * 10);
				code += n;
			}
			if (!games.containsKey(code))
				break;
		}

		return code;

	}

	public void processCommand(String cmd) {
		String[] s = cmd.split(" ");
		if (s[0].equals("new")) {
			int numTeams = Integer.parseInt(s[1]);
			int numTeamPlayers = Integer.parseInt(s[2]);
			int numQuestions = Integer.parseInt(s[3]);
			
			if (numTeams <= 0 || numTeamPlayers <= 0 || numQuestions <= 0 || numQuestions > 7) { //limita a 7 para este ficheiro mas se mudasse o ficheiro mudavamos, e so para evitar erros
                System.out.println("Erro: O numero de equipas, jogadores ou perguntas invalido.");
                return; 
            }

			String code = createCode();
			List<Question> questions = null;
			try {
				questions = QuestionLoader.load("dados/quizzes.json", numQuestions);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			GameState g = new GameState(code, numTeams, numTeamPlayers, questions);
			games.put(code, g);

			System.out.println("Nova sala criada com o codigo: " + code
				+ "\numero de equipas: " + numTeams
				+ "\numero de jogadores por equipa: " + numTeamPlayers
				+ "\numero de perguntas: " + numQuestions
			);
		}

	}

	private class DealWithClient extends Thread { //Thread que gere a ligacao com o cliente
		private Socket connection;
		private ObjectInputStream in; 
		private ObjectOutputStream out; 

		private GameState myGame;
		private Team myTeam;
		private Player myPlayer;

		public DealWithClient(Socket connection) {
			this.connection = connection;
		}

		@Override
		public void run() {
			try {
				setStreams();
				processConnection();
			} catch (Exception e) { 
				e.printStackTrace();
			} finally {
				if (myGame != null && myPlayer != null) {
		            myGame.playerDisconnected(myPlayer.getName(), out);
		        }
				closeConnection();
			}
		}

		private void setStreams() throws IOException {
			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush(); 
			in = new ObjectInputStream(connection.getInputStream());
		}

		private void processConnection() throws IOException, ClassNotFoundException {
			Object obj = in.readObject();
			if (obj instanceof Message) {
				Message msg = (Message) obj;
				if (msg.getType() == Message.Type.LOGIN) {
					String content = (String) msg.getContent();
					String[] s = content.split(" ");
					if (s.length == 3) {
						processFirstConnection(s[0], s[1], s[2]);
					} else {
						out.writeObject(new Message(Message.Type.LOGIN_ERROR, "Formato invalido.", "Server"));
						closeConnection();
						return; 
					}
				}
			}

			if (connection.isClosed()) return;

			
			while (true) {
				try {
					Object nextObj = in.readObject(); 
					if (nextObj instanceof Message) {
						Message msg = (Message) nextObj;
						System.out.println("Mensagem recebida de " + msg.getSender() + ": " + msg.getType());
						if (msg.getType() == Message.Type.ANSWER) {
                            int answerIndex = (Integer) msg.getContent();
                            int points = myGame.submitAnswer(myPlayer.getName(), answerIndex);
                            myGame.sendToPlayer(out, new Message(Message.Type.ANSWER_RESULT, points, "Server"));
                        }
					}
				} catch (IOException e) {
					System.out.println("Cliente desconectou-se.");
					break; 
				}
			}
		}

		private void processFirstConnection(String roomCode, String teamName, String username) throws IOException {
			if (!games.containsKey(roomCode)) {
				out.writeObject(new Message(Message.Type.LOGIN_ERROR, "O jogo nao existe.", "Server"));
				closeConnection();
				return;
			}
			GameState game = games.get(roomCode);
			LoginResult r = game.addTeamAndPlayer(out, teamName, username);
			switch (r) {
		        case USERNAME_EXISTS:
		            out.writeObject(new Message(Message.Type.LOGIN_ERROR, "Username repetido.", "Server"));
		            closeConnection();
		            return;
	
		        case TEAM_LIMIT_REACHED:
		            out.writeObject(new Message(Message.Type.LOGIN_ERROR, "Limite de equipas atingido.", "Server"));
		            closeConnection();
		            return;
	
		        case TEAM_FULL:
		            out.writeObject(new Message(Message.Type.LOGIN_ERROR, "A equipa esta cheia.", "Server"));
		            closeConnection();
		            return;
	
		        case OK:
		            out.writeObject(new Message(Message.Type.LOGIN_SUCCESS, "Bem-vindo!", "Server"));
		            break;
			}
			this.myGame = game;
		    this.myTeam = game.getTeams().get(teamName);
		    this.myPlayer = game.getPlayer(username);

			if (game.areAllPlayersConnected()) {
				System.out.println("Todos ligados. A iniciar jogo " + roomCode + "...");
    			game.broadcast(new Message(Message.Type.START_GAME, "O jogo vai comecar", "Server"));
    			GameThread gameThread = new GameThread(game);
    			gamePool.submit(gameThread);

			}
			
		}

		

		public void closeConnection() {
			try {
				if (connection != null)
					connection.close();
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	

	public static void main(String[] args) {

		new Server().runServer();

	}
}