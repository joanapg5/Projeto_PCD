package Estrutura;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN,          
        LOGIN_SUCCESS,  
        LOGIN_ERROR,    
        START_GAME,     
        QUESTION,       
        ANSWER,         
        ANSWER_RESULT,
        SCORE_UPDATE,   
        END_GAME       
    }

    private Type type;
    private Object content;
    private String sender;  

    public Message(Type type, Object content, String sender) {
        this.type = type;
        this.content = content;
        this.sender = sender;
    }

   
    public Type getType() {
        return type;
    }

    public Object getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "Msg[" + type + ": " + content + "]";
    }
}