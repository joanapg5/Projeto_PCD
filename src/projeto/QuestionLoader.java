package projeto;
import com.google.gson.*;
import java.io.*;
import java.util.*;


public class QuestionLoader {
	public static List<Question> load(String filePath, String quizName) throws IOException{
		// para nao desformatar
		Reader reader = new InputStreamReader(new FileInputStream(filePath), java.nio.charset.StandardCharsets.UTF_8);
		Gson gson=new Gson();
		JsonObject root=gson.fromJson(reader, JsonObject.class);
		JsonArray quizzes=root.getAsJsonArray("quizzes");
		reader.close();
		
		JsonObject quiz=null;
		
		for (JsonElement elem : quizzes) {
		    JsonObject q=elem.getAsJsonObject();
		    String name=q.get("name").getAsString();
		    if(name.equals(quizName)){
		        quiz=q;
		        break;
		    }
		}
		
		if(quiz==null){
		    throw new IllegalArgumentException("Quiz "+quizName+" não encontrado");
		}
		
	
		
		List<Question> questionList=new ArrayList<>();
		JsonArray questions=quiz.getAsJsonArray("questions");
		
		for (JsonElement elem : questions) {
		    JsonObject question=elem.getAsJsonObject();
		    Question qst=gson.fromJson(question, Question.class);
		    questionList.add(qst);
		}

		
		return questionList;
	}

}
