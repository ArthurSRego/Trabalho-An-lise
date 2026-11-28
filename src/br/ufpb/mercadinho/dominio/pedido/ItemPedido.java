package br.ufpb.mercadinho.dominio.pedido;

import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;

public class ItemPedido {

    private final String codigoProduto;
    private final String nomeProduto;
    private final Dinheiro precoUnitario;
    private Quantidade quantidade;

    public ItemPedido(String codigoProduto, String nomeProduto, Dinheiro precoUnitario, Quantidade quantidade) {
        if (quantidade.ehZero()) {
            throw new RegraDeNegocioException("A quantidade do item deve ser de pelo menos 1 unidade.");
        }
        this.codigoProduto = codigoProduto;
        this.nomeProduto = nomeProduto;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    public Dinheiro getSubtotal() {
        return precoUnitario.multiplicar(quantidade.getValor());
    }
    
    void aumentarQuantidade(Quantidade adicional) {
        quantidade = quantidade.somar(adicional);
    }

    public String getCodigoProduto() {
        return codigoProduto;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public Dinheiro getPrecoUnitario() {
        return precoUnitario;
    }

    public Quantidade getQuantidade() {
        return quantidade;
    }
}
