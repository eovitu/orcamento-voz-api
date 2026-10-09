package br.com.eovitu.voz;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Transacao {
    public enum Tipo { RECEITA, DESPESA }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Tipo tipo;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false, length = 120)
    private String descricao;
    @Column(nullable = false)
    private LocalDate data;
    protected Transacao() {}
    public Transacao(Tipo tipo, BigDecimal valor, String descricao, LocalDate data) {
        this.tipo = tipo; this.valor = valor; this.descricao = descricao; this.data = data;
    }
    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public String getDescricao() { return descricao; }
    public LocalDate getData() { return data; }
}
