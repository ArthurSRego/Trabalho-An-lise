package br.ufpb.mercadinho.dominio;

import br.ufpb.mercadinho.excecao.RegraDeNegocioException;

public class Quantidade {

    public static final Quantidade ZERO = new Quantidade(0);

    private final int valor;

    public Quantidade(int valor) {
        if (valor < 0) {
            throw new RegraDeNegocioException("br.ufpb.mercadinho.dominio.Quantidade não pode ser negativa (recebido: " + valor + ").");
        }
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }

    public Quantidade somar(Quantidade outra) {
        return new Quantidade(this.valor + outra.valor);
    }

    public Quantidade subtrair(Quantidade outra) {
        return new Quantidade(this.valor - outra.valor);
    }

    public boolean ehMaiorQue(Quantidade outra) {
        return this.valor > outra.valor;
    }

    public boolean ehMenorOuIgualA(Quantidade outra) {
        return this.valor <= outra.valor;
    }

    public boolean ehZero() {
        return valor == 0;
    }

    @Override
    public boolean equals(Object outroObjeto) {
        if (this == outroObjeto) {
            return true;
        }
        if (!(outroObjeto instanceof Quantidade)) {
            return false;
        }
        Quantidade outra = (Quantidade) outroObjeto;
        return this.valor == outra.valor;
    }
}