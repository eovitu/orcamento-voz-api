package br.com.eovitu.voz;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OrcamentoTools {
    private final OrcamentoService service;
    public OrcamentoTools(OrcamentoService service) { this.service = service; }
    @Tool(description = "Consulta receitas, despesas e saldo reais do orcamento local. Nao altera dados.")
    public OrcamentoService.Resumo consultarOrcamento() { return service.resumo(); }
}
