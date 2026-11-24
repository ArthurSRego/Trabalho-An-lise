import java.util.ArrayList;
import java.util.List;

import br.ufpb.mercadinho.dominio.Perfil;
import br.ufpb.mercadinho.dominio.Usuario;
import br.ufpb.mercadinho.excecao.PersistenciaException;


public class UsuarioRepositorioArquivo implements UsuarioRepositorio {

    private final BancoDeDados banco;

    public UsuarioRepositorioArquivo(BancoDeDados banco) {
        this.banco = banco;
    }

    @Override
    public void salvar(Usuario usuario) {
        banco.salvarLinha(BancoDeDados.TABELA_USUARIOS,
                usuario.getLogin() + ";" + BancoDeDados.limparTexto(usuario.getNome()) + ";" + usuario.getPerfil().name());
    }

    @Override
    public Usuario buscarPorLogin(String login) {
        if (login == null) {
            return null;
        }
        String linha = banco.buscarLinha(BancoDeDados.TABELA_USUARIOS, login.trim().toLowerCase());
        if (linha == null) {
            return null;
        }
        return deLinha(linha);
    }

    @Override
    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        for (String linha : banco.lerTabela(BancoDeDados.TABELA_USUARIOS)) {
            usuarios.add(deLinha(linha));
        }
        return usuarios;
    }

    private Usuario deLinha(String linha) {
        String[] campos = BancoDeDados.separarCampos(linha, 3, BancoDeDados.TABELA_USUARIOS);
        try {
            return new Usuario(campos[0], campos[1], Perfil.valueOf(campos[2]));
        } catch (RuntimeException e) {
            throw new PersistenciaException("Usuário inválido no arquivo de dados: \"" + linha + "\"", e);
        }
    }
}
