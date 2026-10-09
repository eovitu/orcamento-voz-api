package br.com.eovitu.voz;

import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class AssistenteService {
    public record RespostaVoz(String transcricao, String resposta, String audioBase64, String formato) {}
    private final Inteligencia inteligencia;
    public AssistenteService(Inteligencia inteligencia) { this.inteligencia = inteligencia; }
    public String texto(String texto) { return inteligencia.responder(texto); }
    public RespostaVoz audio(byte[] audio, String nome) {
        String transcricao = inteligencia.transcrever(audio, nome);
        if (transcricao == null || transcricao.isBlank()) throw new IllegalArgumentException("Audio sem fala reconhecida");
        if (transcricao.length() > 2000) throw new IllegalArgumentException("Transcricao muito longa");
        String resposta = inteligencia.responder(transcricao);
        byte[] voz = inteligencia.falar(resposta);
        return new RespostaVoz(transcricao, resposta, Base64.getEncoder().encodeToString(voz), "audio/mpeg");
    }
}
