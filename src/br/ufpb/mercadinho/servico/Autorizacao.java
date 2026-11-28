package br.ufpb.mercadinho.servico;

import br.ufpb.mercadinho.dominio.Usuario;
import br.ufpb.mercadinho.excecao.AcessoNegadoException;

/** Reúne as verificações de permissão utilizadas pelos serviços. */
public class Autorizacao {

    public void exigirUsuarioAutenticado(Usuario usuario) {
        if (usuario == null) {
            throw new AcessoNegadoException("É necessário informar um usuário autenticado.");
        }
    }

    public void exigirGerente(Usuario usuario) {
        exigirUsuarioAutenticado(usuario);
        if (!usuario.ehGerente()) {
            throw new AcessoNegadoException("A operação é permitida somente para usuários com perfil GERENTE.");
        }
    }

    public void exigirAtendenteOuGerente(Usuario usuario) {
        exigirUsuarioAutenticado(usuario);
    }
}
