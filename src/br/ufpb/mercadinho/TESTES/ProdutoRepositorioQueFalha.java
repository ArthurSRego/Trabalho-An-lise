package br.ufpb.mercadinho.TESTES;

import java.util.List;

import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.excecao.PersistenciaException;
import br.ufpb.mercadinho.repositorio.ProdutoRepositorio;


public class ProdutoRepositorioQueFalha implements ProdutoRepositorio {

    private final ProdutoRepositorio repositorioReal;
    private final int chamadaQueFalha;
    private int chamadas = 0;

    /**
     * @param repositorioReal repositório verdadeiro, usado nas chamadas que não falham
     * @param chamadaQueFalha em qual chamada de salvar() a falha acontece (1 = primeira)
     */
    public ProdutoRepositorioQueFalha(ProdutoRepositorio repositorioReal, int chamadaQueFalha) {
        this.repositorioReal = repositorioReal;
        this.chamadaQueFalha = chamadaQueFalha;
    }

    @Override
    public void salvar(Produto produto) {
        chamadas++;
        if (chamadas == chamadaQueFalha) {
            throw new PersistenciaException("Falha simulada ao salvar o produto " + produto.getCodigo() + ".");
        }
        repositorioReal.salvar(produto);
    }

    @Override
    public Produto buscarPorCodigo(String codigo) {
        return repositorioReal.buscarPorCodigo(codigo);
    }

    @Override
    public List<Produto> listarTodos() {
        return repositorioReal.listarTodos();
    }
}
