package br.ufpb.mercadinho.servico;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.mercadinho.dominio.financeiro.Cobranca;
import br.ufpb.mercadinho.dominio.pedido.*;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import br.ufpb.mercadinho.dominio.usuario.Usuario;
import br.ufpb.mercadinho.dominio.desconto.CatalogoDescontos;
import br.ufpb.mercadinho.dominio.desconto.PoliticaDesconto;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;
import br.ufpb.mercadinho.repositorio.CobrancaRepositorio;
import br.ufpb.mercadinho.repositorio.PedidoRepositorio;
import br.ufpb.mercadinho.repositorio.ProdutoRepositorio;
import br.ufpb.mercadinho.repositorio.Transacao;
import br.ufpb.mercadinho.servico.observador.CentralDeNotificacoes;

public class PedidoService {

    private final PedidoRepositorio pedidos;
    private final ProdutoRepositorio produtos;
    private final CobrancaRepositorio cobrancas;
    private final Transacao transacao;
    private final CatalogoDescontos catalogoDescontos; 
    private final CentralDeNotificacoes notificacoes;   

    public PedidoService(PedidoRepositorio pedidos, ProdutoRepositorio produtos, CobrancaRepositorio cobrancas,
                         Transacao transacao, CatalogoDescontos catalogoDescontos,
                         CentralDeNotificacoes notificacoes) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.cobrancas = cobrancas;
        this.transacao = transacao;
        this.catalogoDescontos = catalogoDescontos;
        this.notificacoes = notificacoes;
    }

    public Pedido registrarPedido(Usuario usuario, String cliente, String codigoPolitica) {
        Autorizacao.exigirLogin(usuario);                                          
        PoliticaDesconto politica = catalogoDescontos.buscar(codigoPolitica);       

        transacao.iniciar();
        try {
            Pedido pedido = new Pedido(pedidos.proximoNumero(), cliente, usuario.getLogin(), politica);
            pedidos.salvar(pedido);
            transacao.efetivar();                                                  
            return pedido;
        } catch (RuntimeException erro) {
            transacao.desfazer();                                                   
            throw erro;
        }
    }

    public Pedido adicionarItem(Usuario usuario, int numeroPedido, String codigoProduto, int quantidade) {
        Autorizacao.exigirLogin(usuario);                                           
        Pedido pedido = buscarPedidoObrigatorio(numeroPedido);
        Produto produto = buscarProdutoObrigatorio(codigoProduto);                

        transacao.iniciar();
        try {
            pedido.adicionarItem(produto, new Quantidade(quantidade));               
            pedidos.salvar(pedido);
            transacao.efetivar();                                                   
            return pedido;
        } catch (RuntimeException erro) {
            transacao.desfazer();                                                   
            throw erro;
        }
    }

    public boolean confirmarPedido(Usuario usuario, int numeroPedido) {
        Autorizacao.exigirLogin(usuario);                                           
        Pedido pedido = buscarPedidoObrigatorio(numeroPedido);

        if (pedido.getStatus() == StatusPedido.CONFIRMADO) {
            return false;                                                           
        }

        StatusPedido statusAnterior = pedido.getStatus();
        List<Produto> produtosReservados = new ArrayList<>();

        transacao.iniciar();
        try {

            pedido.confirmar();                                                     

            for (ItemPedido item : pedido.getItens()) {
                Produto produto = buscarProdutoObrigatorio(item.getCodigoProduto());
                produto.reservar(item.getQuantidade());                             
                produtos.salvar(produto);
                produtosReservados.add(produto);
            }

            Cobranca cobranca = new Cobranca(cobrancas.proximoNumero(), pedido.getNumero(), pedido.getTotal());
            cobrancas.salvar(cobranca);

            pedidos.salvar(pedido);

            transacao.efetivar();                                               
        } catch (RuntimeException erro) {
            transacao.desfazer();                                                  
            throw erro;
        }

        notificacoes.notificarMudancaDeStatus(pedido, statusAnterior, pedido.getStatus());
        avisarSeAtingiuEstoqueMinimo(produtosReservados);
        return true;
    }

    public void finalizarPedido(Usuario usuario, int numeroPedido) {
        Autorizacao.exigirLogin(usuario);                                           
        Pedido pedido = buscarPedidoObrigatorio(numeroPedido);
        StatusPedido statusAnterior = pedido.getStatus();

        transacao.iniciar();
        try {
            pedido.finalizar();                                                     
            for (ItemPedido item : pedido.getItens()) {
                Produto produto = buscarProdutoObrigatorio(item.getCodigoProduto());
                produto.baixarEstoqueReservado(item.getQuantidade());
                produtos.salvar(produto);
            }
            Cobranca cobranca = buscarCobrancaObrigatoria(numeroPedido);
            cobranca.registrarPagamento();
            cobrancas.salvar(cobranca);
            pedidos.salvar(pedido);
            transacao.efetivar();                                                   
        } catch (RuntimeException erro) {
            transacao.desfazer();                                                   
            throw erro;
        }

        notificacoes.notificarMudancaDeStatus(pedido, statusAnterior, pedido.getStatus());
    }

    public StatusPedido cancelarPedido(Usuario usuario, int numeroPedido) {
        Autorizacao.exigirLogin(usuario);                                           
        Pedido pedido = buscarPedidoObrigatorio(numeroPedido);
        StatusPedido statusAnterior = pedido.getStatus();

        transacao.iniciar();
        try {
            pedido.cancelar();                                                      
            if (statusAnterior == StatusPedido.CONFIRMADO) {
     
                for (ItemPedido item : pedido.getItens()) {
                    Produto produto = buscarProdutoObrigatorio(item.getCodigoProduto());
                    produto.liberarReserva(item.getQuantidade());
                    produtos.salvar(produto);
                }
                Cobranca cobranca = buscarCobrancaObrigatoria(numeroPedido);
                cobranca.cancelar();
                cobrancas.salvar(cobranca);
            }
  
            pedidos.salvar(pedido);
            transacao.efetivar();                                                   
        } catch (RuntimeException erro) {
            transacao.desfazer();                                                   
            throw erro;
        }

        notificacoes.notificarMudancaDeStatus(pedido, statusAnterior, pedido.getStatus());
        return statusAnterior;
    }
    
    public List<Pedido> listarPedidos(Usuario usuario) {
        Autorizacao.exigirLogin(usuario);
        return pedidos.listarTodos();
    }

    public Pedido buscarPedido(Usuario usuario, int numeroPedido) {
        Autorizacao.exigirLogin(usuario);
        return buscarPedidoObrigatorio(numeroPedido);
    }

    public Cobranca buscarCobranca(Usuario usuario, int numeroPedido) {
        Autorizacao.exigirLogin(usuario);
        return cobrancas.buscarPorPedido(numeroPedido);
    }

    private Pedido buscarPedidoObrigatorio(int numeroPedido) {
        Pedido pedido = pedidos.buscarPorNumero(numeroPedido);
        if (pedido == null) {
            throw new RegraDeNegocioException("Pedido #" + numeroPedido + " não encontrado.");
        }
        return pedido;
    }

    private Produto buscarProdutoObrigatorio(String codigoProduto) {
        Produto produto = produtos.buscarPorCodigo(codigoProduto);
        if (produto == null) {
            throw new RegraDeNegocioException("Produto \"" + codigoProduto + "\" não encontrado.");
        }
        return produto;
    }

    private Cobranca buscarCobrancaObrigatoria(int numeroPedido) {
        Cobranca cobranca = cobrancas.buscarPorPedido(numeroPedido);
        if (cobranca == null) {
            throw new RegraDeNegocioException("Inconsistência: o pedido #" + numeroPedido
                    + " está confirmado, mas não tem cobrança.");
        }
        return cobranca;
    }

    private void avisarSeAtingiuEstoqueMinimo(List<Produto> produtosAlterados) {
        for (Produto produto : produtosAlterados) {
            if (produto.atingiuEstoqueMinimo()) {
                notificacoes.notificarEstoqueMinimo(produto);                       
            }
        }
    }
}
