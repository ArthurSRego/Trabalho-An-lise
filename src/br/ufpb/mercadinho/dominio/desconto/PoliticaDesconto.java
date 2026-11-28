package br.ufpb.mercadinho.dominio.desconto;

import br.ufpb.mercadinho.dominio.valor.Dinheiro;


public interface PoliticaDesconto {

    String getCodigo();


    String getDescricao();


    Dinheiro calcularDesconto(Dinheiro subtotal);
}
