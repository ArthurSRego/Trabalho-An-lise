package br.ufpb.mercadinho.repositorio;


public interface Transacao {

    /** Começa a juntar alterações. Nada vai para o disco ainda. */
    void iniciar();

    /** Grava de uma só vez tudo o que foi alterado desde iniciar(). */
    void efetivar();

    /** Descarta tudo o que foi alterado desde iniciar(). Se não há transação aberta, não faz nada. */
    void desfazer();
}