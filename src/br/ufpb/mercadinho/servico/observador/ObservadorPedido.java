package br.ufpb.mercadinho.servico.observador;

import br.ufpb.mercadinho.dominio.Pedido;
import br.ufpb.mercadinho.dominio.StatusPedido;

public interface ObservadorPedido {

    void statusDoPedidoMudou(Pedido pedido, StatusPedido statusAnterior, StatusPedido statusNovo);
}
