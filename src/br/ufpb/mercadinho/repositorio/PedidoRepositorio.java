package br.ufpb.mercadinho.repositorio;

import br.ufpb.mercadinho.dominio.Pedido;

import java.util.List;

public interface PedidoRepositorio {

    int proximoNumero();

    void salvar(Pedido pedido);

    Pedido buscarPorNumero(int numero);

    List<Pedido> listarTodos();
}
