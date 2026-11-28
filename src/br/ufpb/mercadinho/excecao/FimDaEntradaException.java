package br.ufpb.mercadinho.excecao;

public class FimDaEntradaException extends RuntimeException {
    public FimDaEntradaException() {
        super("Fim da entrada de dados.");
    }
}
