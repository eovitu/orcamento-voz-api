package br.com.eovitu.voz;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.*;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.hamcrest.Matchers.containsString;

class SpringAiIntegracaoTest {
    @Test void usaModelosReaisComHttpSimuladoEToolCalling() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var retry = RetryTemplate.builder().maxAttempts(1).build();
        var orcamento = mock(OrcamentoService.class);
        when(orcamento.resumo()).thenReturn(new OrcamentoService.Resumo(
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("80.00")));
        var chatModelo = OpenAiChatModel.builder()
            .openAiApi(OpenAiApi.builder().apiKey("chave-ficticia-de-teste").restClientBuilder(builder).build())
            .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").build())
            .retryTemplate(retry).build();
        var audioApi = OpenAiAudioApi.builder().apiKey("chave-ficticia-de-teste").restClientBuilder(builder).build();
        var ia = new SpringAiInteligencia(
            ChatClient.builder(chatModelo).defaultTools(new OrcamentoTools(orcamento)).build(),
            new OpenAiAudioTranscriptionModel(audioApi,
                OpenAiAudioTranscriptionOptions.builder().model("whisper-1").responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON).build(), retry),
            new OpenAiAudioSpeechModel(audioApi,
                OpenAiAudioSpeechOptions.builder().model("tts-1").voice("alloy").build(), retry));
        server.expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
            .andExpect(content().string(containsString("audio.wav")))
            .andRespond(withSuccess("{\"text\":\"Qual meu saldo?\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andExpect(content().string(containsString("consultarOrcamento")))
            .andRespond(withSuccess("""
                {"id":"chat1","object":"chat.completion","created":1,"model":"gpt-4o-mini",
                "choices":[{"index":0,"message":{"role":"assistant","content":null,
                "tool_calls":[{"id":"tool1","type":"function","function":{"name":"consultarOrcamento","arguments":"{}"}}]},
                "finish_reason":"tool_calls"}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andExpect(content().string(containsString("80.00")))
            .andRespond(withSuccess("""
                {"id":"chat2","object":"chat.completion","created":2,"model":"gpt-4o-mini",
                "choices":[{"index":0,"message":{"role":"assistant","content":"Seu saldo e 80 reais."},"finish_reason":"stop"}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.openai.com/v1/audio/speech"))
            .andExpect(content().string(containsString("Seu saldo e 80 reais.")))
            .andRespond(withSuccess(new byte[]{1,2,3}, MediaType.parseMediaType("audio/mpeg")));
        var resposta = new AssistenteService(ia).audio(new byte[]{5,6}, "audio.wav");
        assertEquals("Qual meu saldo?", resposta.transcricao());
        assertEquals("Seu saldo e 80 reais.", resposta.resposta());
        assertEquals("AQID", resposta.audioBase64());
        verify(orcamento).resumo();
        server.verify();
    }
    @Test void falhaDoProvedorNaoExpoeConteudo() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var audioApi = OpenAiAudioApi.builder().apiKey("chave-ficticia").restClientBuilder(builder).build();
        var retry = RetryTemplate.builder().maxAttempts(1).build();
        var ia = new SpringAiInteligencia(null, null, new OpenAiAudioSpeechModel(audioApi,
            OpenAiAudioSpeechOptions.builder().model("tts-1").voice("alloy").build(), retry));
        server.expect(requestTo("https://api.openai.com/v1/audio/speech"))
            .andRespond(withServerError().body("informacao-privada-do-provedor"));
        var erro = assertThrows(IllegalStateException.class, () -> ia.falar("Ola"));
        assertFalse(erro.getMessage().contains("privada"));
        assertNull(erro.getCause());
        server.verify();
    }
}
