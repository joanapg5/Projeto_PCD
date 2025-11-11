package projeto;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

//aqui a ideia é apresentar as perguntas numa frame - passam 30s - mudar para a stats frame - mudar para a prox pergunta (sempre construir frame nova)

public class GUI {
	
	private JFrame frame;
	private JLabel questionLabel;
	private JButton[] optionButtons;
	private JLabel timerLabel;
	private JLabel titleLable;
	//private GameState gamestate; //nao tenho a certeza disto
	//private Player currentPlayer;
	
	//PROVISORIO
	private int current = 0;
	private List<Question> questions;


	
	public GUI(/*GameState gamestate, Player currentPlayer*/){ //ns!!
		
		
		//this.gamestate = gamestate;
	    //this.currentPlayer = currentPlayer;
		frame=new JFrame("Kahoot");
		
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		

	}
	
	public void setCurrent(int q){
		current=q;
	}
	
	public void setQuestions(List<Question> questions){
		this.questions=questions;
	}
	
	public void addQuestionFrame(Question q){ //recebe pergunta
		// Limpa o conteúdo anterior
	    frame.getContentPane().removeAll();
	    
		frame.setLayout(new BorderLayout(10,10));
		
		
		JPanel top=new JPanel();
		titleLable=new JLabel("IsKahoot", JLabel.CENTER);
		top.add(titleLable);
		
		frame.add(top, BorderLayout.NORTH);
		
		String qtext=q.getQuestion();
		JPanel center=new JPanel(new BorderLayout(10, 10));
		questionLabel=new JLabel(qtext, JLabel.CENTER); //vai ter a pergunta
		center.add(questionLabel, BorderLayout.NORTH);
		
		
		
		
		
		String[] options=q.getOptions();
		int gridsize=0;
		if(options.length%2==0){
			gridsize=options.length/2;
		}else{
			gridsize=(options.length + 1)/2;
		}
		JPanel answers=new JPanel(new GridLayout(gridsize,2, 10, 10));
		optionButtons=new JButton[4];
		Color[] colors = {new Color(171,0,11), new Color(0,2,154), new Color(139, 0, 139), new Color(4,138,0)};
		for (int i=0; i<options.length; i++) {
            optionButtons[i]=new JButton(options[i]);
            answers.add(optionButtons[i]);
            optionButtons[i].setBackground(colors[i]);
            optionButtons[i].setOpaque(true);
            optionButtons[i].setBorderPainted(false);
            optionButtons[i].setForeground(Color.WHITE);
        }
        center.add(answers, BorderLayout.CENTER);

        frame.add(center, BorderLayout.CENTER);
        
        JPanel bottom = new JPanel();
        timerLabel = new JLabel("Tempo", JLabel.CENTER); //vai ter o tempo
        bottom.add(timerLabel);
        

        frame.add(bottom, BorderLayout.SOUTH);
        
        for (int i=0; i<options.length; i++) {
        	int index = i;
            optionButtons[i].addActionListener(new ActionListener() {
            	//tenho de limitar um player a uma opcao btw nao ta ainda  funcionar isso
    			@Override
    			public void actionPerformed(ActionEvent e) {
    				//acao do botao: responder (gamestate submit ??) nao tenho a certeza!!
    				//gamestate.submit(currentPlayer,index);
    				//PROVISORIO
    				Map<String, Team> ts = new HashMap<>();
    				Map<String, Integer> scoreboard = new HashMap<>();
    				Player p1 = new Player(1,"Joao");
    		        Player p2 = new Player(2,"Artur");
    		        Player p3 = new Player(3,"Joana");
    		        Player p4 = new Player(4,"Mafalda");
    		        Team team1 = new Team(1, "Team 1", List.of(p1, p2), 0);
    		        Team team2 = new Team(2, "Team 2", List.of(p3, p4), 0);
    		        List<Team> teams = List.of(team1, team2);
    		        for (Team t : teams) {
    		            ts.put(t.getTeamName(), t);
    		            scoreboard.put(t.getTeamName(), 0);
    		        }
    				addStatsFrame(scoreboard);
    				
    			}
    		});
        }
        frame.pack();
        frame.revalidate();
	    frame.repaint();
        
	
	}
	
	
	
	
	public void addStatsFrame(Map<String, Integer> scoreboard) {
	    // Limpa o conteúdo anterior
	    frame.getContentPane().removeAll();
	    //vai buscar o scoreboard ao gamestate
	    //Map<String, Integer> scoreboard=gamestate.getScoreboard();
	    JPanel statsPanel = new JPanel();
	    statsPanel.setLayout(new GridLayout(scoreboard.size() + 1, 2, 10, 10));

	    statsPanel.add(new JLabel("Equipa", JLabel.CENTER));
	    statsPanel.add(new JLabel("Pontuação", JLabel.CENTER));
	    
	    for (String teamName : scoreboard.keySet()) {
	        int score = scoreboard.get(teamName);
	        statsPanel.add(new JLabel(teamName, JLabel.CENTER));
	        statsPanel.add(new JLabel(String.valueOf(score), JLabel.CENTER));
	    }


	    

	    frame.add(new JLabel("Fim da ronda", JLabel.CENTER), BorderLayout.NORTH);
	    frame.add(statsPanel, BorderLayout.CENTER);
	    
	    JPanel ok=new JPanel();
	    JButton b=new JButton("Ok");
	    b.addActionListener(new ActionListener() {
        	
			@Override
			public void actionPerformed(ActionEvent e) {
				//PROVISORIO
				if(current>=questions.size()){
					frame.setVisible(false);
				}
				addQuestionFrame(questions.get(current+1));
				current+=1;
				
			}
		});
	    ok.add(b);
	    frame.add(ok, BorderLayout.SOUTH);
	    frame.pack();
	    frame.revalidate();
	    frame.repaint();
	}
	
	

	
	public void open() {
		// para abrir a janela (torna-la visivel)
		frame.setVisible(true);
	}
	
	
	

}
