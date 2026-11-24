package br.ufpb.mercadinho.excecao;

public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(String message) {
        super(message);
    }
}
