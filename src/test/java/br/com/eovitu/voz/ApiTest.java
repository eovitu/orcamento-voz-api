package br.com.eovitu.voz;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "app.ai.enabled=false"})
@AutoConfigureMockMvc
class ApiTest {
    @Autowired MockMvc mvc;
    @Autowired TransacaoRepository repository;
    @Autowired OrcamentoTools tools;
    @BeforeEach void limpar() { repository.deleteAll(); }
    @Test void registraEConsultaPersistencia() throws Exception {
        mvc.perform(post("/api/transacoes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipo\":\"RECEITA\",\"valor\":100.10,\"descricao\":\"Salario ficticio\",\"data\":\"2026-10-08\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("id").isNumber());
        mvc.perform(post("/api/transacoes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipo\":\"DESPESA\",\"valor\":0.20,\"descricao\":\"Exemplo\",\"data\":\"2026-10-08\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/orcamento")).andExpect(status().isOk()).andExpect(jsonPath("saldo").value(99.90));
        mvc.perform(get("/api/transacoes")).andExpect(jsonPath("$.length()").value(2));
        assertEquals(0, new BigDecimal("99.90").compareTo(tools.consultarOrcamento().saldo()));
    }
    @Test void rejeitaZeroENegativoSemPersistir() throws Exception {
        for (String valor : new String[]{"0", "-1", "0.001", "100000000000000"}) {
            mvc.perform(post("/api/transacoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"DESPESA\",\"valor\":" + valor + ",\"descricao\":\"Teste\",\"data\":\"2026-10-08\"}"))
                .andExpect(status().isBadRequest());
        }
        assertEquals(0, repository.count());
    }
    @Test void rejeitaCamposAusentesETipoInvalido() throws Exception {
        for (String entrada : new String[]{"{}", "{\"tipo\":\"PIX\"}", "malformado"})
            mvc.perform(post("/api/transacoes").contentType(MediaType.APPLICATION_JSON).content(entrada))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("erro").value("Entrada invalida"));
    }
    @Test void iaDesativadaRetorna503SemChave() throws Exception {
        mvc.perform(post("/api/assistente/texto").contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Qual meu saldo?\"}"))
            .andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/fala").contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Oi\"}"))
            .andExpect(status().isServiceUnavailable());
    }
    @Test void validaTextoVazioELongo() throws Exception {
        for (String texto : new String[]{" ", "x".repeat(2001)})
            mvc.perform(post("/api/assistente/texto").contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"" + texto + "\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void rejeitaAudioVazioEExtensaoInvalida() throws Exception {
        mvc.perform(multipart("/api/assistente/audio").file(new MockMultipartFile("arquivo", "voz.wav", "audio/wav", new byte[0])))
            .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/transcricoes").file(new MockMultipartFile("arquivo", "script.exe", "audio/wav", new byte[]{1})))
            .andExpect(status().isBadRequest());
    }
    @Test void rejeitaAudioAcimaDoLimite() throws Exception {
        mvc.perform(multipart("/api/assistente/audio").file(new MockMultipartFile("arquivo", "voz.wav", "audio/wav", new byte[10*1024*1024+1])))
            .andExpect(status().isBadRequest());
    }
    @Test void orcamentoVazioTemSaldoZero() throws Exception {
        mvc.perform(get("/api/orcamento")).andExpect(status().isOk()).andExpect(jsonPath("saldo").value(0));
    }
}
