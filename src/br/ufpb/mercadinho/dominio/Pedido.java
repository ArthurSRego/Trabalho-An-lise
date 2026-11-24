package br.ufpb.mercadinho.dominio;

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

    /** Cria um pedido NOVO: começa ABERTO e sem itens. */
    public Pedido(int numero, String cliente, String loginAtendente, PoliticaDesconto politicaDesconto) {
        this(numero, cliente, loginAtendente, StatusPedido.ABERTO, politicaDesconto, new ArrayList<>());
    }

    /** Construtor completo, usado pelo repositório ao ler um pedido do arquivo. */
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

    // ------------------------------------------------------------------
    // Itens (UC01 - Registrar pedido)
    // ------------------------------------------------------------------

    /**
     * Inclui um produto no pedido.
     *
     * RN02: só é possível incluir itens enquanto o pedido está ABERTO.
     * RN01: a quantidade TOTAL deste produto no pedido não pode passar do
     *       estoque disponível. (A reserva de verdade só acontece ao confirmar.)
     * Se o produto já está no pedido, a quantidade da mesma linha aumenta
     * (não criamos linhas repetidas para o mesmo produto).
     */
    public void adicionarItem(Produto produto, Quantidade quantidade) {
        if (status != StatusPedido.ABERTO) {
            throw new TransicaoInvalidaException("Só é possível incluir itens em pedidos ABERTOS. O pedido #"
                    + numero + " está " + status + ".");
        }
        if (quantidade.ehZero()) {
            throw new RegraDeNegocioException("A quantidade do item deve ser de pelo menos 1 unidade.");
        }

        // RN01: o que já está no pedido + o que está entrando agora
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

    // ------------------------------------------------------------------
    // Valores (RN05 - política de desconto via Strategy)
    // ------------------------------------------------------------------

    /** Soma dos itens, antes do desconto. */
    public Dinheiro getSubtotal() {
        Dinheiro soma = Dinheiro.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.somar(item.getSubtotal());
        }
        return soma;
    }

    /** STRATEGY em ação: o pedido delega o cálculo para a política escolhida. */
    public Dinheiro getDesconto() {
        return politicaDesconto.calcularDesconto(getSubtotal());
    }

    /** Total a pagar = subtotal - desconto. */
    public Dinheiro getTotal() {
        return getSubtotal().subtrair(getDesconto());
    }

    // ------------------------------------------------------------------
    // Mudanças de status (RN02)
    // ------------------------------------------------------------------

    /** ABERTO -> CONFIRMADO. Um pedido vazio não pode ser confirmado. */
    public void confirmar() {
        verificarTransicao(StatusPedido.CONFIRMADO);
        if (itens.isEmpty()) {
            throw new RegraDeNegocioException("O pedido #" + numero + " não tem itens e não pode ser confirmado.");
        }
        status = StatusPedido.CONFIRMADO;
    }

    /** CONFIRMADO -> FINALIZADO (pedido entregue ao cliente). */
    public void finalizar() {
        verificarTransicao(StatusPedido.FINALIZADO);
        status = StatusPedido.FINALIZADO;
    }

    /** ABERTO ou CONFIRMADO -> CANCELADO. */
    public void cancelar() {
        verificarTransicao(StatusPedido.CANCELADO);
        status = StatusPedido.CANCELADO;
    }

    /** RN02: lança exceção se a seta "status atual -> novo status" não existe no diagrama. */
    private void verificarTransicao(StatusPedido novoStatus) {
        if (!status.podeMudarPara(novoStatus)) {
            throw new TransicaoInvalidaException("Transição inválida: o pedido #" + numero + " está "
                    + status + " e não pode passar para " + novoStatus + ".");
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

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

    /** Devolve uma CÓPIA da lista: para mudar itens é preciso passar por adicionarItem(). */
    public List<ItemPedido> getItens() {
        return new ArrayList<>(itens);
    }
}