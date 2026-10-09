package br.com.eovitu.voz;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrcamentoService {
    public record NovoLancamento(@NotNull Transacao.Tipo tipo,
        @NotNull @DecimalMin("0.01") @Digits(integer = 14, fraction = 2) BigDecimal valor,
        @NotBlank @Size(max = 120) String descricao, @NotNull LocalDate data) {}
    public record Resumo(BigDecimal receitas, BigDecimal despesas, BigDecimal saldo) {}
    private final TransacaoRepository repository;
    public OrcamentoService(TransacaoRepository repository) { this.repository = repository; }
    @Transactional
    public Transacao criar(NovoLancamento entrada) {
        return repository.save(new Transacao(entrada.tipo(), entrada.valor(), entrada.descricao().trim(), entrada.data()));
    }
    @Transactional(readOnly = true)
    public List<Transacao> listar() { return repository.findAll(Sort.by("id")); }
    @Transactional(readOnly = true)
    public Resumo resumo() {
        BigDecimal receitas = BigDecimal.ZERO, despesas = BigDecimal.ZERO;
        for (Transacao t : listar()) {
            if (t.getTipo() == Transacao.Tipo.RECEITA) receitas = receitas.add(t.getValor());
            else despesas = despesas.add(t.getValor());
        }
        return new Resumo(receitas, despesas, receitas.subtract(despesas));
    }
}
