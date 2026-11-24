package br.ufpb.mercadinho.excecao;

public class EstoqueInsuficienteException extends RegraDeNegocioException {

    public EstoqueInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
