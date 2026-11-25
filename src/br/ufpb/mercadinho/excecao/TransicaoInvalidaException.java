package br.ufpb.mercadinho.excecao;

public class TransicaoInvalidaException extends RegraDeNegocioException{
    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
