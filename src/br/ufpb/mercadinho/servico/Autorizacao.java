package br.ufpb.mercadinho.servico;

import br.ufpb.mercadinho.dominio.usuario.*;
import br.ufpb.mercadinho.excecao.AcessoNegadoException;

public class Autorizacao {

    private Autorizacao() {
    }

    public static void exigirLogin(Usuario usuario) {
        if (usuario == null) {
            throw new AcessoNegadoException("Acesso negado: é preciso fazer login antes.");
        }
    }

    public static void exigirGerente(Usuario usuario, String acao) {
        exigirLogin(usuario);
        if (usuario.getPerfil() != Perfil.GERENTE) {
            throw new AcessoNegadoException("Acesso negado: somente o GERENTE pode " + acao
                    + ". Usuário atual: " + usuario.getLogin() + " (" + usuario.getPerfil() + ").");
        }
    }
}
