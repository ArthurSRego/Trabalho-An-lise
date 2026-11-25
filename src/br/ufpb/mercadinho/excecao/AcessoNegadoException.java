package br.ufpb.mercadinho.excecao;

public class AcessoNegadoException extends RegraDeNegocioException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
