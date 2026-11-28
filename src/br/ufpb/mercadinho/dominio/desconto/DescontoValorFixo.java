package br.ufpb.mercadinho.dominio.desconto;

import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;


public class DescontoValorFixo implements PoliticaDesconto {

    private final String codigo;
    private final Dinheiro valor;


    public DescontoValorFixo(String codigo, Dinheiro valor) {
        if (valor.ehZero()) {
            throw new RegraDeNegocioException("O valor do desconto fixo deve ser maior que zero.");
        }
        this.codigo = codigo;
        this.valor = valor;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    @Override
    public String getDescricao() {
        return valor + " de desconto";
    }

    @Override
    public Dinheiro calcularDesconto(Dinheiro subtotal) {
        // Nunca desconta mais do que o próprio subtotal: o total não pode ficar negativo.
        if (valor.ehMaiorQue(subtotal)) {
            return subtotal;
        }
        return valor;
    }
}
