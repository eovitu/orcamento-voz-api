package br.com.eovitu.voz;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.ByteArrayResource;

public class SpringAiInteligencia implements Inteligencia {
    private final ChatClient chat;
    private final OpenAiAudioTranscriptionModel transcricao;
    private final OpenAiAudioSpeechModel fala;
    public SpringAiInteligencia(ChatClient chat, OpenAiAudioTranscriptionModel transcricao, OpenAiAudioSpeechModel fala) {
        this.chat = chat; this.transcricao = transcricao; this.fala = fala;
    }
    public String transcrever(byte[] audio, String nome) {
        try {
            return transcricao.call(new ByteArrayResource(audio) {
                @Override public String getFilename() { return nome; }
            });
        } catch (RuntimeException e) { throw new IllegalStateException("Falha no provedor de transcricao"); }
    }
    public String responder(String texto) {
        try {
            String resposta = chat.prompt().user(texto).call().content();
            if (resposta == null || resposta.isBlank() || resposta.length() > 4000)
                throw new IllegalStateException("Resposta invalida");
            return resposta;
        } catch (RuntimeException e) { throw new IllegalStateException("Falha no provedor de chat"); }
    }
    public byte[] falar(String texto) {
        try {
            byte[] audio = fala.call(texto);
            if (audio == null || audio.length == 0) throw new IllegalStateException("Audio vazio");
            return audio;
        } catch (RuntimeException e) { throw new IllegalStateException("Falha no provedor de voz"); }
    }
}
