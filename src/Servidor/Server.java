package Servidor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;



public class Server {
	public static final int PORT=2025;
	
	private ServerSocket server; //server
	
	
	public void runServer(){
		try{
			server = new ServerSocket(PORT);
			while(true){
				waitForConnection();
				
			}
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(server != null){
				try{
					server.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	
	private void waitForConnection() throws IOException{
		Socket connection = server.accept();
		ConnectionHandler handler= new ConnectionHandler(connection);
		handler.start();
		System.out.println("Started new connection...");
	}
	

	
	// connection handler é so para o servidor?
	private class ConnectionHandler extends Thread{
		private Socket connection; //connection
		private Scanner in; //stream reader
		private PrintWriter out; //stream writer
		
		public ConnectionHandler(Socket connection){
			this.connection=connection;
		}
		
		@Override
		public void run(){
			try{
				setStreams();
				processConnection();
			}catch(IOException e){
				e.printStackTrace();
			}finally{
				closeConnection();
			}
			
		}
		
		
		private void setStreams() throws IOException{
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream())), true);
			in = new Scanner(connection.getInputStream());
		}
		
		private void processConnection(){
			while(true){
				//TODO
			}
		}
		
		public void closeConnection(){
			try{
				if(connection!=null)
					connection.close();
				if(in != null)
					in.close();
				if(out != null)
					out.close();
				
			}catch (IOException e){
				e.printStackTrace();
				
			}
		}
		
		
		
	}
	
	
	public static void main(String[] args){
		
		new Server().runServer();
		
	}
}