package br.ufpb.mercadinho.dominio.desconto;

import br.ufpb.mercadinho.dominio.valor.Dinheiro;

public class SemDesconto implements PoliticaDesconto {

    @Override
    public String getCodigo() {
        return "SEM";
    }

    @Override
    public String getDescricao() {
        return "Sem desconto";
    }

    @Override
    public Dinheiro calcularDesconto(Dinheiro subtotal) {
        return Dinheiro.ZERO;
    }
}
