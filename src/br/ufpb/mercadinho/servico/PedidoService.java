package br.ufpb.mercadinho.servico;

import br.ufpb.mercadinho.dominio.*;
import br.ufpb.mercadinho.dominio.desconto.PoliticaDesconto;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;
import br.ufpb.mercadinho.repositorio.*;
import br.ufpb.mercadinho.servico.observador.CentralDeNotificacoes;

import java.util.ArrayList;
import java.util.List;

public class PedidoService {
    private final PedidoRepositorio pedidos;
    private final ProdutoRepositorio produtos;
    private final CobrancaRepositorio cobrancas;
    private final Transacao transacao;
    private final Autorizacao autorizacao;
    private final CentralDeNotificacoes notificacoes;

    public PedidoService(PedidoRepositorio pedidos, ProdutoRepositorio produtos,
                         CobrancaRepositorio cobrancas, Transacao transacao,
                         Autorizacao autorizacao, CentralDeNotificacoes notificacoes) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.cobrancas = cobrancas;
        this.transacao = transacao;
        this.autorizacao = autorizacao;
        this.notificacoes = notificacoes;
    }

    public Pedido criarPedido(Usuario usuario, String cliente, PoliticaDesconto desconto) {
        autorizacao.exigirAtendenteOuGerente(usuario);
        if (desconto == null) throw new RegraDeNegocioException("A política de desconto é obrigatória.");
        Pedido pedido = new Pedido(pedidos.proximoNumero(), cliente, usuario.getLogin(), desconto);
        executarTransacao(() -> pedidos.salvar(pedido));
        return pedido;
    }

    public Pedido buscarPorNumero(Usuario usuario, int numero) {
        autorizacao.exigirAtendenteOuGerente(usuario);
        Pedido pedido = pedidos.buscarPorNumero(numero);
        if (pedido == null) throw new RegraDeNegocioException("Pedido #" + numero + " não encontrado.");
        return pedido;
    }

    public List<Pedido> listarTodos(Usuario usuario) {
        autorizacao.exigirAtendenteOuGerente(usuario);
        return pedidos.listarTodos();
    }

    public Pedido adicionarItem(Usuario usuario, int numeroPedido, String codigoProduto, Quantidade quantidade) {
        autorizacao.exigirAtendenteOuGerente(usuario);
        Pedido pedido = buscarInterno(numeroPedido);
        Produto produto = buscarProduto(codigoProduto);
        executarTransacao(() -> {
            pedido.adicionarItem(produto, quantidade);
            pedidos.salvar(pedido);
        });
        return pedido;
    }

    public Cobranca confirmar(Usuario usuario, int numeroPedido) {
        autorizacao.exigirAtendenteOuGerente(usuario);
        Pedido pedido = buscarInterno(numeroPedido);
        StatusPedido anterior = pedido.getStatus();
        final Cobranca[] resultado = new Cobranca[1];
        executarTransacao(() -> {
            for (ItemPedido item : pedido.getItens()) {
                Produto produto = buscarProduto(item.getCodigoProduto());
                produto.reservar(item.getQuantidade());
                produtos.salvar(produto);
            }
            pedido.confirmar();
            pedidos.salvar(pedido);
            Cobranca cobranca = cobrancas.buscarPorPedido(numeroPedido);
            if (cobranca == null)
                cobranca = new Cobranca(cobrancas.proximoNumero(), numeroPedido, pedido.getTotal());
            cobrancas.salvar(cobranca);
            resultado[0] = cobranca;
        });
        notificarMudanca(pedido, anterior);
        for (ItemPedido item : pedido.getItens()) avisarEstoqueSeNecessario(buscarProduto(item.getCodigoProduto()));
        return resultado[0];
    }

    public void finalizar(Usuario usuario, int numeroPedido) {
        autorizacao.exigirAtendenteOuGerente(usuario);
        Pedido pedido = buscarInterno(numeroPedido);
        StatusPedido anterior = pedido.getStatus();
        executarTransacao(() -> {
            Cobranca cobranca = cobrancas.buscarPorPedido(numeroPedido);
            if (cobranca == null) throw new RegraDeNegocioException("O pedido não possui cobrança.");
            cobranca.registrarPagamento();
            cobrancas.salvar(cobranca);
            for (ItemPedido item : pedido.getItens()) {
                Produto produto = buscarProduto(item.getCodigoProduto());
                produto.baixarEstoqueReservado(item.getQuantidade());
                produtos.salvar(produto);
            }
            pedido.finalizar();
            pedidos.salvar(pedido);
        });
        notificarMudanca(pedido, anterior);
        for (ItemPedido item : pedido.getItens()) avisarEstoqueSeNecessario(buscarProduto(item.getCodigoProduto()));
    }

    public void cancelar(Usuario usuario, int numeroPedido) {
        autorizacao.exigirAtendenteOuGerente(usuario);
        Pedido pedido = buscarInterno(numeroPedido);
        StatusPedido anterior = pedido.getStatus();
        executarTransacao(() -> {
            if (anterior == StatusPedido.CONFIRMADO) {
                for (ItemPedido item : pedido.getItens()) {
                    Produto produto = buscarProduto(item.getCodigoProduto());
                    produto.liberarReserva(item.getQuantidade());
                    produtos.salvar(produto);
                }
                Cobranca cobranca = cobrancas.buscarPorPedido(numeroPedido);
                if (cobranca != null && cobranca.getStatus() == StatusCobranca.PENDENTE) {
                    cobranca.cancelar();
                    cobrancas.salvar(cobranca);
                }
            }
            pedido.cancelar();
            pedidos.salvar(pedido);
        });
        notificarMudanca(pedido, anterior);
    }

    private Pedido buscarInterno(int numero) {
        Pedido pedido = pedidos.buscarPorNumero(numero);
        if (pedido == null) throw new RegraDeNegocioException("Pedido #" + numero + " não encontrado.");
        return pedido;
    }

    private Produto buscarProduto(String codigo) {
        Produto produto = produtos.buscarPorCodigo(codigo);
        if (produto == null) throw new RegraDeNegocioException("Produto não encontrado: " + codigo + ".");
        return produto;
    }

    private void notificarMudanca(Pedido pedido, StatusPedido anterior) {
        if (notificacoes != null && anterior != pedido.getStatus())
            notificacoes.notificarMudancaDeStatus(pedido, anterior, pedido.getStatus());
    }

    private void avisarEstoqueSeNecessario(Produto produto) {
        if (notificacoes != null && produto.atingiuEstoqueMinimo())
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
