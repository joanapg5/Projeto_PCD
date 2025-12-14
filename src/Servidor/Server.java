package Servidor;

import Estrutura.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
//ver da sincronizacao?

public class Server {
	public static final int PORT = 2025;

	private ServerSocket server; // server
	private Map<String, GameState> games = new ConcurrentHashMap<>();

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
						processCommand(cmd);

					}

				}
			}).start();
			while (true) {
				waitForConnection();

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
		System.out.println("Started new connection...");
	}

	// connection handler ï¿½ so para o servidor?
	private class DealWithClient extends Thread {
		private Socket connection;
		private ObjectInputStream in; // MUDOU de Scanner para ObjectInputStream
		private ObjectOutputStream out; // MUDOU de PrintWriter para ObjectOutputStream

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
			} catch (Exception e) { // Catch genÃ©rico para apanhar ClassNotFoundException tambÃ©m
				e.printStackTrace();
			} finally {
				closeConnection();
			}
		}

		private void setStreams() throws IOException {
			// ORDEM CRÃ�TICA: Primeiro o Output, Flush, depois o Input
			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush(); // Garante que o cabeÃ§alho Ã© enviado para o cliente nÃ£o bloquear
			in = new ObjectInputStream(connection.getInputStream());
		}

		private void processConnection() throws IOException, ClassNotFoundException {
			// --- PASSO 1: TRATAR O LOGIN (PRIMEIRA MENSAGEM) ---
			Object obj = in.readObject();
			if (obj instanceof Message) {
				Message msg = (Message) obj;
				if (msg.getType() == Message.Type.LOGIN) {
					String content = (String) msg.getContent();
					String[] s = content.split(" ");
					if (s.length == 3) {
						processFirstConnection(s[0], s[1], s[2]);
					} else {
						out.writeObject(new Message(Message.Type.LOGIN_ERROR, "Formato invÃ¡lido.", "Server"));
						closeConnection();
						return; // Sai se o formato for invÃ¡lido
					}
				}
			}

			// Se a conexÃ£o foi fechada por erro no login (ex: user repetido), saÃ­mos.
			if (connection.isClosed()) return;

			// --- PASSO 2: LOOP PRINCIPAL (AGUARDAR MENSAGENS DO JOGO) ---
			// A thread fica aqui presa e viva enquanto o cliente estiver ligado
			while (true) {
				try {
					Object nextObj = in.readObject(); // Bloqueia Ã  espera de mensagens (ex: Respostas)
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
					// O cliente desligou-se (ou o jogo acabou)
					System.out.println("Cliente desconectou-se.");
					break; // Sai do loop e vai para o finally fechar tudo limpo
				}
			}
		}

		// confirmar se nao podem existir usernames repetidos mesmo que em jogos dif
		private void processFirstConnection(String roomCode, String teamName, String username) throws IOException {
			//isto precisa de ser sinchronized?
			if (!games.containsKey(roomCode)) {
				out.writeObject(new Message(Message.Type.LOGIN_ERROR, "O jogo não existe.", "Server"));
				closeConnection();
				return;
			}
			GameState game = games.get(roomCode);
			//substituir aqui pela funcao
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
		            out.writeObject(new Message(Message.Type.LOGIN_ERROR, "A equipa está cheia.", "Server"));
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
    			game.broadcast(new Message(Message.Type.START_GAME, "O jogo vai começar", "Server"));
				GameThread gameThread = new GameThread(game);
				gameThread.start();
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

			System.out.println("Nova sala criada com o cÃ³digo: " + code
				+ "\nNÃºmero de equipas: " + numTeams
				+ "\nNÃºmero de jogadores por equipa: " + numTeamPlayers
				+ "\nNÃºmero de perguntas: " + numQuestions
			);
		}

	}

	public static void main(String[] args) {

		new Server().runServer();

	}
}