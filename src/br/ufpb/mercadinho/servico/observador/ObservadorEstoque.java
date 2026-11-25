package br.ufpb.mercadinho.servico.observador;

import br.ufpb.mercadinho.dominio.Produto;

public interface ObservadorEstoque {

    void estoqueAtingiuMinimo(Produto produto);
}
