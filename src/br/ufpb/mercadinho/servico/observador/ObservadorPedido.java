package br.ufpb.mercadinho.servico.observador;

import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;

public interface ObservadorPedido {

    void statusDoPedidoMudou(Pedido pedido, StatusPedido statusAnterior, StatusPedido statusNovo);
}
