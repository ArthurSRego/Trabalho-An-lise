package br.ufpb.mercadinho.repositorio;


public interface Transacao {

    void iniciar();

    void efetivar();

    void desfazer();
}