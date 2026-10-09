package br.com.eovitu.voz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ApiController {
    public record Texto(@NotBlank @Size(max = 2000) String texto) {}
    private final OrcamentoService orcamento;
    private final AssistenteService assistente;
    private final Inteligencia inteligencia;
    public ApiController(OrcamentoService o, AssistenteService a, Inteligencia i) {
        orcamento = o; assistente = a; inteligencia = i;
    }
    @PostMapping("/transacoes") @ResponseStatus(HttpStatus.CREATED)
    public Transacao criar(@Valid @RequestBody OrcamentoService.NovoLancamento entrada) { return orcamento.criar(entrada); }
    @GetMapping("/transacoes") public List<Transacao> listar() { return orcamento.listar(); }
    @GetMapping("/orcamento") public OrcamentoService.Resumo resumo() { return orcamento.resumo(); }
    @PostMapping("/assistente/texto")
    public Map<String, String> texto(@Valid @RequestBody Texto entrada) { return Map.of("resposta", assistente.texto(entrada.texto())); }
    @PostMapping(value = "/assistente/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssistenteService.RespostaVoz audio(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return assistente.audio(validarAudio(arquivo), nomeSeguro(arquivo));
    }
    @PostMapping(value = "/transcricoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> transcrever(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return Map.of("texto", inteligencia.transcrever(validarAudio(arquivo), nomeSeguro(arquivo)));
    }
    @PostMapping(value = "/fala", produces = "audio/mpeg")
    public byte[] falar(@Valid @RequestBody Texto entrada) { return inteligencia.falar(entrada.texto()); }
    private byte[] validarAudio(MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty() || arquivo.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("Audio deve ter entre 1 byte e 10 MB");
        String nome = nomeSeguro(arquivo);
        String extensao = nome.substring(nome.lastIndexOf('.') + 1);
        if (!Set.of("mp3", "wav", "m4a", "webm", "mp4", "mpeg", "mpga").contains(extensao))
            throw new IllegalArgumentException("Formato de audio nao suportado");
        return arquivo.getBytes();
    }
    private String nomeSeguro(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        if (nome == null) throw new IllegalArgumentException("Nome de audio ausente");
        int ponto = nome.lastIndexOf('.');
        return "audio." + (ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT));
    }
}
