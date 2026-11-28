package br.ufpb.mercadinho.TESTES;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;
import br.ufpb.mercadinho.servico.observador.ObservadorEstoque;
import br.ufpb.mercadinho.servico.observador.ObservadorPedido;

public class ObservadorDeTeste implements ObservadorPedido, ObservadorEstoque {

    public final List<String> avisos = new ArrayList<>();

    @Override
    public void statusDoPedidoMudou(Pedido pedido, StatusPedido statusAnterior, StatusPedido statusNovo) {
        avisos.add("Pedido " + pedido.getNumero() + ": " + statusAnterior + " -> " + statusNovo);
    }

    @Override
    public void estoqueAtingiuMinimo(Produto produto) {
        avisos.add("Estoque mínimo: " + produto.getCodigo());
    }
}
