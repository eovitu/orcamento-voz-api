package br.com.eovitu.voz;

public interface Inteligencia {
    String transcrever(byte[] audio, String nome);
    String responder(String texto);
    byte[] falar(String texto);
}
