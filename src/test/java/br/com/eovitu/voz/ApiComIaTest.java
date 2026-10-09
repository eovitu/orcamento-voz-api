package br.com.eovitu.voz;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:audio;DB_CLOSE_DELAY=-1", "app.ai.enabled=false"})
@AutoConfigureMockMvc
class ApiComIaTest {
    @Autowired MockMvc mvc;
    @MockitoBean Inteligencia inteligencia;
    @Test void endpointAudioDevolveFluxoCompleto() throws Exception {
        when(inteligencia.transcrever(any(), eq("audio.wav"))).thenReturn("Qual meu saldo?");
        when(inteligencia.responder("Qual meu saldo?")).thenReturn("Saldo de zero reais.");
        when(inteligencia.falar("Saldo de zero reais.")).thenReturn(new byte[]{1,2,3});
        mvc.perform(multipart("/api/assistente/audio").file(new MockMultipartFile("arquivo", "../../voz.wav", "audio/wav", new byte[]{7})))
            .andExpect(status().isOk()).andExpect(jsonPath("transcricao").value("Qual meu saldo?"))
            .andExpect(jsonPath("resposta").value("Saldo de zero reais."))
            .andExpect(jsonPath("audioBase64").value("AQID"));
    }
    @Test void endpointsIndependentesTranscrevemESintetizam() throws Exception {
        when(inteligencia.transcrever(any(), eq("audio.mp3"))).thenReturn("Ola");
        mvc.perform(multipart("/api/transcricoes").file(new MockMultipartFile("arquivo", "voz.mp3", "audio/mpeg", new byte[]{1})))
            .andExpect(status().isOk()).andExpect(jsonPath("texto").value("Ola"));
        when(inteligencia.falar("Ola")).thenReturn(new byte[]{4,5});
        mvc.perform(post("/api/fala").contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Ola\"}"))
            .andExpect(status().isOk()).andExpect(content().contentType("audio/mpeg"))
            .andExpect(content().bytes(new byte[]{4,5}));
    }
    @Test void falhaRemotaRetorna503SemDetalhes() throws Exception {
        when(inteligencia.responder(any())).thenThrow(new IllegalStateException("segredo-do-provedor"));
        mvc.perform(post("/api/assistente/texto").contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Saldo?\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo"))));
    }
    @Test void arquivoAusenteRetorna400() throws Exception {
        mvc.perform(multipart("/api/transcricoes")).andExpect(status().isBadRequest());
    }
}
