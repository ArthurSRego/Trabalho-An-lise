package br.ufpb.mercadinho.repositorio;

import br.ufpb.mercadinho.dominio.Produto;

import java.util.List;

public interface ProdutoRepositorio {

    void salvar(Produto produto);

    Produto buscarPorCodigo(String codigo);

    List<Produto> listarTodos();
}
