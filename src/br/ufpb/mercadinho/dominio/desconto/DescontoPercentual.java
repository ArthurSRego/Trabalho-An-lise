package br.ufpb.mercadinho.dominio.desconto;

import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;


public class DescontoPercentual implements PoliticaDesconto {

    private final String codigo;
    private final int percentual;


    public DescontoPercentual(String codigo, int percentual) {
        if (percentual < 1 || percentual > 100) {
            throw new RegraDeNegocioException("O percentual de desconto deve estar entre 1 e 100 (recebido: "
                    + percentual + ").");
        }
        this.codigo = codigo;
        this.percentual = percentual;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    @Override
    public String getDescricao() {
        return percentual + "% de desconto";
    }

    @Override
    public Dinheiro calcularDesconto(Dinheiro subtotal) {
        // Como o percentual vai no máximo até 100, o desconto nunca passa do subtotal.
        return subtotal.percentual(percentual);
    }
}
