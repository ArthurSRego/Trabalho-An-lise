package br.ufpb.mercadinho.servico;

import java.util.List;

import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.desconto.CatalogoDescontos;
import br.ufpb.mercadinho.dominio.desconto.PoliticaDesconto;
import br.ufpb.mercadinho.dominio.financeiro.Cobranca;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;
import br.ufpb.mercadinho.dominio.usuario.Usuario;
import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;
import br.ufpb.mercadinho.repositorio.UsuarioRepositorio;

public class MercadinhoFacade {

    private final UsuarioRepositorio usuarios;
    private final ProdutoService produtoService;
    private final PedidoService pedidoService;
    private final CatalogoDescontos catalogoDescontos;

    private Usuario usuarioLogado;   // null enquanto ninguém fez login

    public MercadinhoFacade(UsuarioRepositorio usuarios, ProdutoService produtoService,
                            PedidoService pedidoService, CatalogoDescontos catalogoDescontos) {
        this.usuarios = usuarios;
        this.produtoService = produtoService;
        this.pedidoService = pedidoService;
        this.catalogoDescontos = catalogoDescontos;
    }


    // Sessão


    public Usuario entrar(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new RegraDeNegocioException("Informe o login.");
        }
        Usuario usuario = usuarios.buscarPorLogin(login);
        if (usuario == null) {
            throw new RegraDeNegocioException("Usuário \"" + login.trim() + "\" não encontrado.");
        }
        usuarioLogado = usuario;
        return usuario;
    }

    public void sair() {
        usuarioLogado = null;
    }

    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public List<Usuario> listarUsuarios() {
        return usuarios.listarTodos();
    }

    public void verificarAcessoDeGerente(String acao) {
        Autorizacao.exigirGerente(usuarioLogado, acao);
    }


    // Pedidos


    public List<PoliticaDesconto> listarPoliticasDesconto() {
        Autorizacao.exigirLogin(usuarioLogado);
        return catalogoDescontos.listar();
    }

    public Pedido registrarPedido(String cliente, String codigoPoliticaDesconto) {
        return pedidoService.registrarPedido(usuarioLogado, cliente, codigoPoliticaDesconto);
    }

    public Pedido adicionarItem(int numeroPedido, String codigoProduto, int quantidade) {
        return pedidoService.adicionarItem(usuarioLogado, numeroPedido, codigoProduto, quantidade);
    }

    public boolean confirmarPedido(int numeroPedido) {
        return pedidoService.confirmarPedido(usuarioLogado, numeroPedido);
    }

    public void finalizarPedido(int numeroPedido) {
        pedidoService.finalizarPedido(usuarioLogado, numeroPedido);
    }


    public StatusPedido cancelarPedido(int numeroPedido) {
        return pedidoService.cancelarPedido(usuarioLogado, numeroPedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos(usuarioLogado);
    }

    public Pedido buscarPedido(int numeroPedido) {
        return pedidoService.buscarPedido(usuarioLogado, numeroPedido);
    }

    public Cobranca buscarCobrancaDoPedido(int numeroPedido) {
        return pedidoService.buscarCobranca(usuarioLogado, numeroPedido);
    }


    // Produtos e estoque


    public List<Produto> listarProdutos() {
        return produtoService.listarProdutos(usuarioLogado);
    }

    public Produto cadastrarProduto(String codigo, String nome, long precoEmCentavos,
                                    int estoqueInicial, int estoqueMinimo) {
        return produtoService.cadastrarProduto(usuarioLogado, codigo, nome, new Dinheiro(precoEmCentavos),
                estoqueInicial, estoqueMinimo);
    }

    public Produto alterarPreco(String codigo, long novoPrecoEmCentavos) {
        return produtoService.alterarPreco(usuarioLogado, codigo, new Dinheiro(novoPrecoEmCentavos));
    }

    public Produto registrarEntradaDeEstoque(String codigo, int quantidade) {
        return produtoService.registrarEntrada(usuarioLogado, codigo, quantidade);
    }

    public Produto registrarSaidaDeEstoque(String codigo, int quantidade) {
        return produtoService.registrarSaida(usuarioLogado, codigo, quantidade);
    }

    public List<Produto> listarProdutosParaRepor() {
        return produtoService.listarProdutosParaRepor(usuarioLogado);
    }
}
