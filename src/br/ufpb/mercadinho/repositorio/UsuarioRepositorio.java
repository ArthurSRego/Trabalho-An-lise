package br.ufpb.mercadinho.repositorio;

import br.ufpb.mercadinho.dominio.usuario.Usuario;

import java.util.List;

public interface UsuarioRepositorio {

    void salvar(Usuario usuario);

    Usuario buscarPorLogin(String login);

    List<Usuario> listarTodos();
}
