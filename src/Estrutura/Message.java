package Estrutura;

import java.io.Serializable;

public class Message implements Serializable {

    // Para evitar warnings de serialização
    private static final long serialVersionUID = 1L;

    // Tipos de mensagens possíveis
    public enum Type {
        LOGIN,          // Pedido de login
        LOGIN_SUCCESS,  // Login aceite
        LOGIN_ERROR,    // Erro no login (nome repetido, etc)
        START_GAME,     // Jogo começou
        QUESTION,       // Envio de pergunta
        ANSWER,         // Envio de resposta
        ANSWER_RESULT,
        SCORE_UPDATE,   // Atualização de pontuação
        END_GAME        // Fim do jogo
    }

    private Type type;
    private Object content; // Pode ser uma String, uma Question, um Map de scores...
    private String sender;  // Quem enviou (opcional, útil para debug)

    public Message(Type type, Object content, String sender) {
        this.type = type;
        this.content = content;
        this.sender = sender;
    }

    // Getters
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