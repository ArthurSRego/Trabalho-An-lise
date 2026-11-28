package br.ufpb.mercadinho.servico;

import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import br.ufpb.mercadinho.dominio.usuario.Usuario;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;
import br.ufpb.mercadinho.repositorio.ProdutoRepositorio;
import br.ufpb.mercadinho.repositorio.Transacao;
import br.ufpb.mercadinho.servico.observador.CentralDeNotificacoes;

import java.util.List;

public class ProdutoService {
    private final ProdutoRepositorio produtos;
    private final Transacao transacao;
    private final Autorizacao autorizacao;
    private final CentralDeNotificacoes notificacoes;

    public ProdutoService(ProdutoRepositorio produtos, Transacao transacao,
                          Autorizacao autorizacao, CentralDeNotificacoes notificacoes) {
        this.produtos = produtos;
        this.transacao = transacao;
        this.autorizacao = autorizacao;
        this.notificacoes = notificacoes;
    }

    public void cadastrar(Usuario usuario, Produto produto) {
        autorizacao.exigirGerente(usuario);
        if (produto == null) throw new RegraDeNegocioException("O produto é obrigatório.");
        if (produtos.buscarPorCodigo(produto.getCodigo()) != null)
            throw new RegraDeNegocioException("Já existe um produto com o código " + produto.getCodigo() + ".");
        executarTransacao(() -> produtos.salvar(produto));
        avisarSeEstoqueMinimo(produto);
    }

    public Produto buscarPorCodigo(Usuario usuario, String codigo) {
        autorizacao.exigirAtendenteOuGerente(usuario);
        Produto produto = produtos.buscarPorCodigo(codigo);
        if (produto == null) throw new RegraDeNegocioException("Produto não encontrado: " + codigo + ".");
        return produto;
    }

    public List<Produto> listarTodos(Usuario usuario) {
        autorizacao.exigirAtendenteOuGerente(usuario);
        return produtos.listarTodos();
    }

    public void registrarEntrada(Usuario usuario, String codigo, Quantidade quantidade) {
        autorizacao.exigirGerente(usuario);
        alterarEstoque(codigo, quantidade, true);
    }

    public void registrarSaida(Usuario usuario, String codigo, Quantidade quantidade) {
        autorizacao.exigirGerente(usuario);
        alterarEstoque(codigo, quantidade, false);
    }

    public void alterarPreco(Usuario usuario, String codigo, Dinheiro novoPreco) {
        autorizacao.exigirGerente(usuario);
        Produto produto = buscarInterno(codigo);
        executarTransacao(() -> {
            produto.alterarPreco(novoPreco);
            produtos.salvar(produto);
        });
    }

    private void alterarEstoque(String codigo, Quantidade quantidade, boolean entrada) {
        Produto produto = buscarInterno(codigo);
        executarTransacao(() -> {
            if (entrada) produto.registrarEntrada(quantidade);
            else produto.registrarSaida(quantidade);
            produtos.salvar(produto);
        });
        avisarSeEstoqueMinimo(produto);
    }

    private Produto buscarInterno(String codigo) {
        Produto produto = produtos.buscarPorCodigo(codigo);
        if (produto == null) throw new RegraDeNegocioException("Produto não encontrado: " + codigo + ".");
        return produto;
    }

    private void avisarSeEstoqueMinimo(Produto produto) {
        if (produto.atingiuEstoqueMinimo() && notificacoes != null)
            notificacoes.notificarEstoqueMinimo(produto);
    }

    private void executarTransacao(Runnable operacao) {
        transacao.iniciar();
        try {
            operacao.run();
            transacao.efetivar();
        } catch (RuntimeException e) {
            transacao.desfazer();
            throw e;
        }
    }
}
