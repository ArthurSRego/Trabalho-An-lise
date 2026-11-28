package br.ufpb.mercadinho.servico;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.mercadinho.dominio.valor.*;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.usuario.Usuario;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;
import br.ufpb.mercadinho.repositorio.ProdutoRepositorio;
import br.ufpb.mercadinho.repositorio.Transacao;
import br.ufpb.mercadinho.servico.observador.CentralDeNotificacoes;

public class ProdutoService {

    private final ProdutoRepositorio produtos;
    private final Transacao transacao;
    private final CentralDeNotificacoes notificacoes; 

    public ProdutoService(ProdutoRepositorio produtos, Transacao transacao, CentralDeNotificacoes notificacoes) {
        this.produtos = produtos;
        this.transacao = transacao;
        this.notificacoes = notificacoes;
    }
    
    public Produto cadastrarProduto(Usuario usuario, String codigo, String nome, Dinheiro preco,
                                    int estoqueInicial, int estoqueMinimo) {
        Autorizacao.exigirGerente(usuario, "cadastrar produtos");              
        Produto novo = new Produto(codigo, nome, preco, new Quantidade(estoqueInicial), new Quantidade(estoqueMinimo));
        if (produtos.buscarPorCodigo(novo.getCodigo()) != null) {
            throw new RegraDeNegocioException("Já existe um produto com o código " + novo.getCodigo() + ".");
        }

        transacao.iniciar();
        try {
            produtos.salvar(novo);
            transacao.efetivar();
        } catch (RuntimeException erro) {
            transacao.desfazer();
            throw erro;
        }
        return novo;
    }

    public Produto alterarPreco(Usuario usuario, String codigo, Dinheiro novoPreco) {
        Autorizacao.exigirGerente(usuario, "alterar preços");                   
        Produto produto = buscarProdutoObrigatorio(codigo);

        transacao.iniciar();
        try {
            produto.alterarPreco(novoPreco);
            produtos.salvar(produto);
            transacao.efetivar();
        } catch (RuntimeException erro) {
            transacao.desfazer();
            throw erro;
        }
        return produto;
    }

    public Produto registrarEntrada(Usuario usuario, String codigo, int quantidade) {
        Autorizacao.exigirGerente(usuario, "registrar entrada de estoque");    
        Produto produto = buscarProdutoObrigatorio(codigo);

        transacao.iniciar();
        try {
            produto.registrarEntrada(new Quantidade(quantidade));
            produtos.salvar(produto);
            transacao.efetivar();
        } catch (RuntimeException erro) {
            transacao.desfazer();
            throw erro;
        }
        return produto;
    }

    public Produto registrarSaida(Usuario usuario, String codigo, int quantidade) {
        Autorizacao.exigirGerente(usuario, "registrar saída de estoque");   
        Produto produto = buscarProdutoObrigatorio(codigo);

        transacao.iniciar();
        try {
            produto.registrarSaida(new Quantidade(quantidade));                  
            produtos.salvar(produto);
            transacao.efetivar();
        } catch (RuntimeException erro) {
            transacao.desfazer();
            throw erro;
        }

        if (produto.atingiuEstoqueMinimo()) {
            notificacoes.notificarEstoqueMinimo(produto);                         
        }
        return produto;
    }

    public List<Produto> listarProdutos(Usuario usuario) {
        Autorizacao.exigirLogin(usuario);                                          
        return produtos.listarTodos();
    }

    public List<Produto> listarProdutosParaRepor(Usuario usuario) {
        Autorizacao.exigirGerente(usuario, "ver o relatório de reposição");    
        List<Produto> paraRepor = new ArrayList<>();
        for (Produto produto : produtos.listarTodos()) {
            if (produto.atingiuEstoqueMinimo()) {
                paraRepor.add(produto);
            }
        }
        return paraRepor;
    }

    private Produto buscarProdutoObrigatorio(String codigo) {
        Produto produto = produtos.buscarPorCodigo(codigo);
        if (produto == null) {
            throw new RegraDeNegocioException("Produto \"" + codigo + "\" não encontrado.");
        }
        return produto;
    }
}

