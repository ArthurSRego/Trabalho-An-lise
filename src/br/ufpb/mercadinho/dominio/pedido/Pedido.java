package br.ufpb.mercadinho.dominio.pedido;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.mercadinho.dominio.desconto.PoliticaDesconto;
import br.ufpb.mercadinho.excecao.EstoqueInsuficienteException;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;
import br.ufpb.mercadinho.excecao.TransicaoInvalidaException;

public class Pedido {

    private final int numero;                        // identificador único (gerado pelo repositório)
    private final String cliente;
    private final String loginAtendente;             // quem registrou o pedido
    private StatusPedido status;
    private final PoliticaDesconto politicaDesconto; // STRATEGY
    private final List<ItemPedido> itens;

    // Cria um pedido aberto e sem itens
    public Pedido(int numero, String cliente, String loginAtendente, PoliticaDesconto politicaDesconto) {
        this(numero, cliente, loginAtendente, StatusPedido.ABERTO, politicaDesconto, new ArrayList<>());
    }

    public Pedido(int numero, String cliente, String loginAtendente, StatusPedido status,
                  PoliticaDesconto politicaDesconto, List<ItemPedido> itens) {
        if (numero <= 0) {
            throw new RegraDeNegocioException("O número do pedido deve ser positivo.");
        }
        if (cliente == null || cliente.trim().isEmpty()) {
            throw new RegraDeNegocioException("O nome do cliente é obrigatório.");
        }
        if (politicaDesconto == null) {
            throw new RegraDeNegocioException("O pedido precisa de uma política de desconto.");
        }
        this.numero = numero;
        this.cliente = cliente.trim();
        this.loginAtendente = loginAtendente;
        this.status = status;
        this.politicaDesconto = politicaDesconto;
        this.itens = new ArrayList<>(itens);   // cópia: quem passou a lista não consegue mexer na nossa
    }

    // Registrar pedids
    public void adicionarItem(Produto produto, Quantidade quantidade) {
        if (status != StatusPedido.ABERTO) {
            throw new TransicaoInvalidaException("Só é possível incluir itens em pedidos ABERTOS. O pedido #"
                    + numero + " está " + status + ".");
        }
        if (quantidade.ehZero()) {
            throw new RegraDeNegocioException("A quantidade do item deve ser de pelo menos 1 unidade.");
        }

        Quantidade totalDesejado = quantidadeDoProduto(produto.getCodigo()).somar(quantidade);
        if (totalDesejado.ehMaiorQue(produto.getEstoqueDisponivel())) {
            throw new EstoqueInsuficienteException("Estoque insuficiente para " + produto
                    + ": o pedido ficaria com " + totalDesejado + " unidade(s), mas só há "
                    + produto.getEstoqueDisponivel() + " disponível(is).");
        }

        for (ItemPedido item : itens) {
            if (item.getCodigoProduto().equals(produto.getCodigo())) {
                item.aumentarQuantidade(quantidade);   // produto já estava no pedido
                return;
            }
        }
        itens.add(new ItemPedido(produto.getCodigo(), produto.getNome(), produto.getPreco(), quantidade));
    }

    /** Quantas unidades deste produto já estão no pedido (zero se nenhuma). */
    public Quantidade quantidadeDoProduto(String codigoProduto) {
        for (ItemPedido item : itens) {
            if (item.getCodigoProduto().equals(codigoProduto)) {
                return item.getQuantidade();
            }
        }
        return Quantidade.ZERO;
    }

    //política de desconto via Strategy
    // Soma dos itens antes do desconto
    public Dinheiro getSubtotal() {
        Dinheiro soma = Dinheiro.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.somar(item.getSubtotal());
        }
        return soma;
    }

    public Dinheiro getDesconto() {
        return politicaDesconto.calcularDesconto(getSubtotal());
    }

    public Dinheiro getTotal() {
        return getSubtotal().subtrair(getDesconto());
    }

    // Mudanças de status
    public void confirmar() {
        verificarTransicao(StatusPedido.CONFIRMADO);
        if (itens.isEmpty()) {
            throw new RegraDeNegocioException("O pedido #" + numero + " não tem itens e não pode ser confirmado.");
        }
        status = StatusPedido.CONFIRMADO;
    }

    public void finalizar() {
        verificarTransicao(StatusPedido.FINALIZADO);
        status = StatusPedido.FINALIZADO;
    }

    public void cancelar() {
        verificarTransicao(StatusPedido.CANCELADO);
        status = StatusPedido.CANCELADO;
    }

    //lança exceção se a seta "status atual -> novo status" não existe no diagrama
    private void verificarTransicao(StatusPedido novoStatus) {
        if (!status.podeMudarPara(novoStatus)) {
            throw new TransicaoInvalidaException("Transição inválida: o pedido #" + numero + " está "
                    + status + " e não pode passar para " + novoStatus + ".");
        }
    }


    public int getNumero() {
        return numero;
    }

    public String getCliente() {
        return cliente;
    }

    public String getLoginAtendente() {
        return loginAtendente;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public PoliticaDesconto getPoliticaDesconto() {
        return politicaDesconto;
    }

    public List<ItemPedido> getItens() {
        return new ArrayList<>(itens);
    }
}
