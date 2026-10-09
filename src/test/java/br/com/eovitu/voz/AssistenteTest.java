package br.com.eovitu.voz;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssistenteTest {
    @Test void orquestraTranscricaoChatEFalaEmOrdem() {
        Inteligencia ia = mock(Inteligencia.class);
        byte[] audio = {1,2};
        when(ia.transcrever(audio, "audio.wav")).thenReturn("Qual meu saldo?");
        when(ia.responder("Qual meu saldo?")).thenReturn("Saldo de 10 reais.");
        when(ia.falar("Saldo de 10 reais.")).thenReturn(new byte[]{3,4});
        var resposta = new AssistenteService(ia).audio(audio, "audio.wav");
        assertEquals("Qual meu saldo?", resposta.transcricao());
        assertEquals("Saldo de 10 reais.", resposta.resposta());
        assertEquals("AwQ=", resposta.audioBase64());
        assertEquals("audio/mpeg", resposta.formato());
        var ordem = inOrder(ia);
        ordem.verify(ia).transcrever(audio, "audio.wav");
        ordem.verify(ia).responder("Qual meu saldo?");
        ordem.verify(ia).falar("Saldo de 10 reais.");
    }
    @Test void transcricaoVaziaNaoAcionaChat() {
        Inteligencia ia = mock(Inteligencia.class);
        when(ia.transcrever(any(), any())).thenReturn(" ");
        assertThrows(IllegalArgumentException.class, () -> new AssistenteService(ia).audio(new byte[]{1}, "audio.wav"));
        verify(ia, never()).responder(any());
        verify(ia, never()).falar(any());
    }
    @Test void falhaNoChatNaoSintetizaVoz() {
        Inteligencia ia = mock(Inteligencia.class);
        when(ia.transcrever(any(), any())).thenReturn("Saldo?");
        when(ia.responder(any())).thenThrow(new IllegalStateException("Falha"));
        assertThrows(IllegalStateException.class, () -> new AssistenteService(ia).audio(new byte[]{1}, "audio.wav"));
        verify(ia, never()).falar(any());
    }
}
