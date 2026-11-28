package br.ufpb.mercadinho.dominio.usuario;

import br.ufpb.mercadinho.excecao.RegraDeNegocioException;

public class Usuario {

    private final String login;
    private final String nome;
    private final Perfil perfil;

    public Usuario(String login, String nome, Perfil perfil) {
        if (login == null || login.trim().isEmpty()) {
            throw new RegraDeNegocioException("O login do usuário é obrigatório.");
        }
        if (!Texto.temApenasLetrasENumeros(login.trim())) {
            throw new RegraDeNegocioException("O login deve ter apenas letras e números (recebido: \"" + login + "\").");
        }
        if (nome == null || nome.trim().isEmpty()) {
            throw new RegraDeNegocioException("O nome do usuário é obrigatório.");
        }
        if (perfil == null) {
            throw new RegraDeNegocioException("O perfil do usuário é obrigatório.");
        }
        this.login = login.trim().toLowerCase();   // "Ana" e "ana" são o mesmo login
        this.nome = nome.trim();
        this.perfil = perfil;
    }

    public String getLogin() {
        return login;
    }

    public String getNome() {
        return nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean ehGerente() {
        return perfil == Perfil.GERENTE;
    }

    @Override
    public String toString() {
        return login + " (" + perfil + ")";
    }
}
