package br.ufpb.mercadinho.servico;

import br.ufpb.mercadinho.dominio.*;
import br.ufpb.mercadinho.dominio.desconto.PoliticaDesconto;

import java.util.List;

/** Facade: oferece uma porta de entrada única para os serviços do mercadinho. */
public class MercadinhoFacade {
    private final ProdutoService produtoService;
    private final PedidoService pedidoService;

    public MercadinhoFacade(ProdutoService produtoService, PedidoService pedidoService) {
        if (produtoService == null || pedidoService == null)
            throw new IllegalArgumentException("Os serviços de produto e pedido são obrigatórios.");
        this.produtoService = produtoService;
        this.pedidoService = pedidoService;
    }

    public void cadastrarProduto(Usuario usuario, Produto produto) { produtoService.cadastrar(usuario, produto); }
    public Produto buscarProduto(Usuario usuario, String codigo) { return produtoService.buscarPorCodigo(usuario, codigo); }
    public List<Produto> listarProdutos(Usuario usuario) { return produtoService.listarTodos(usuario); }
    public void registrarEntradaEstoque(Usuario usuario, String codigo, Quantidade quantidade) {
        produtoService.registrarEntrada(usuario, codigo, quantidade);
    }
    public void registrarSaidaEstoque(Usuario usuario, String codigo, Quantidade quantidade) {
        produtoService.registrarSaida(usuario, codigo, quantidade);
    }
    public void alterarPreco(Usuario usuario, String codigo, Dinheiro preco) {
        produtoService.alterarPreco(usuario, codigo, preco);
    }

    public Pedido criarPedido(Usuario usuario, String cliente, PoliticaDesconto desconto) {
        return pedidoService.criarPedido(usuario, cliente, desconto);
    }
    public Pedido adicionarItem(Usuario usuario, int numeroPedido, String codigoProduto, Quantidade quantidade) {
        return pedidoService.adicionarItem(usuario, numeroPedido, codigoProduto, quantidade);
    }
    public Cobranca confirmarPedido(Usuario usuario, int numeroPedido) {
        return pedidoService.confirmar(usuario, numeroPedido);
    }
    public void finalizarPedido(Usuario usuario, int numeroPedido) { pedidoService.finalizar(usuario, numeroPedido); }
    public void cancelarPedido(Usuario usuario, int numeroPedido) { pedidoService.cancelar(usuario, numeroPedido); }
    public Pedido buscarPedido(Usuario usuario, int numero) { return pedidoService.buscarPorNumero(usuario, numero); }
    public List<Pedido> listarPedidos(Usuario usuario) { return pedidoService.listarTodos(usuario); }
}
