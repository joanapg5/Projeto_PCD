package Cliente;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

import Servidor.Server;
import Estrutura.Message;
import Estrutura.Question;

public class Client {

	private Socket connection;
	private ObjectInputStream in;  
    private ObjectOutputStream out; 
	private GUI gui;
	private String roomCode;
	private String teamName;
	private String username;

	public Client(String roomCode, String teamName, String username) {
		this.roomCode = roomCode;
		this.teamName = teamName;
		this.username = username;
	}

	public void runClient() {
		try {
			this.gui = new GUI(this, username);
			connectToServer();
			setStreams();
			processConnection();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeConnection();
		}
	}

	private void setStreams() throws IOException {
        out = new ObjectOutputStream(connection.getOutputStream());
        out.flush();
        in = new ObjectInputStream(connection.getInputStream());
    }

	void connectToServer() throws IOException {
		InetAddress endereco = InetAddress.getByName(null);
		System.out.println("Endereco:" + endereco);
		connection = new Socket(endereco, Server.PORT);
		System.out.println("Socket:" + connection);

	}

	void processConnection() throws IOException {
        try {
            String loginData = roomCode + " " + teamName + " " + username;
            out.writeObject(new Message(Message.Type.LOGIN, loginData, username));
            
            Object responseObj = in.readObject();
            if (responseObj instanceof Message) {
                Message response = (Message) responseObj;
                
                if (response.getType() == Message.Type.LOGIN_SUCCESS) {
                    System.out.println("Ligacao ao jogo estabelecida!");
                    waitForStart();
                } else {
                    System.out.println("Erro: " + response.getContent());
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

		
	}

	public void sendAnswer(int index) {
        try {
            
            out.writeObject(new Message(Message.Type.ANSWER, index, username));
            out.flush(); 
        } catch (IOException e) {
            System.out.println("Erro ao enviar resposta: " + e.getMessage());
        }
    }

	void waitForStart() throws IOException, ClassNotFoundException {
        System.out.println("A aguardar inicio do jogo...");

        gui.open();

        while (true) {
            try {
                Object obj = in.readObject(); 
                if (obj instanceof Message) {
                    Message msg = (Message) obj;

                    switch (msg.getType()) {
                        case START_GAME:
                            System.out.println("O JOGO COMECOU!");
                            break;

                        case QUESTION:
                            Question q = (Question) msg.getContent();
                            System.out.println("Recebi pergunta: " + q.getQuestion());
                            gui.addQuestionFrame(q);
                            break;

                        case ANSWER_RESULT:
                            int points = (Integer) msg.getContent();
                            if (points == -1) {
                                System.out.println("Resposta registada. A aguardar pela equipa...");
                                
                            } 
                            else if (points > 0) {
                                System.out.println("ACERTASTE! Ganhaste " + points + " pontos.");
                                gui.showFeedback(true, points);
                                
                            } else {
                                System.out.println("ERRASTE!");
                                gui.showFeedback(false, 0);
                            }
                            break;

                        case SCORE_UPDATE:
                            Map<String, Integer> scores = (Map<String, Integer>) msg.getContent();
                            gui.addStatsFrame(scores);
                            break;

                        case END_GAME:
                            System.out.println("Fim do jogo!");
                            gui.endOfGame();
                            gui.close();
                            return;

                        default:
                            System.out.println("Msg desconhecida: " + msg.getType());
                    }
                }
            } catch (java.io.StreamCorruptedException e) {
                System.err.println("Erro critico de sincronizacao de stream. A tentar recuperar...");
                break; 
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Ligacao perdida ou erro de leitura.");
                break;
            }
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

	public static void main(String[] args) {

		if (args.length != 3) {
			System.out.println("Insira os dados no formato <Jogo> <Equipa> <Username>");
			return;
		}

		String roomCode = args[0];
		String teamName = args[1];
		String username = args[2];

		new Client(roomCode, teamName, username).runClient();
	}

}
