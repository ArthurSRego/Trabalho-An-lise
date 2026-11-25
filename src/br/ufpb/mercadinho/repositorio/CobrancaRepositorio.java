package br.ufpb.mercadinho.repositorio;

import br.ufpb.mercadinho.dominio.Cobranca;

import java.util.List;

public interface CobrancaRepositorio {

    int proximoNumero();

    void salvar(Cobranca cobranca);

    Cobranca buscarPorPedido(int numeroPedido);

    List<Cobranca> listarTodas();
}
