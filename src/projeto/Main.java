package projeto;
import java.util.*;
//teste para ver se o eclipse da update do git


public class Main {
	
	
	public static void main(String[] args){
		
        
     
        List<Question> questions = null;
        try {
            questions = QuestionLoader.load("dados/perguntas.json", "PCD - 1"); 
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


       
        Player p1 = new Player(1,"Joao");
        Player p2 = new Player(2,"Artur");
        Player p3 = new Player(3,"Joana");
        Player p4 = new Player(4,"Mafalda");
        Team team1 = new Team(1, "Team 1", List.of(p1, p2), 0);
        Team team2 = new Team(2, "Team 2", List.of(p3, p4), 0);
        List<Team> teams = List.of(team1, team2);
        

       
        GameState gs = new GameState("ABC123", teams, questions);

        
        GUI window = new GUI();
        window.open();
        
        
        window.setQuestions(questions);

        
        window.addQuestionFrame(questions.get(0));

        
        
        
        
    }
		
		
	

}
