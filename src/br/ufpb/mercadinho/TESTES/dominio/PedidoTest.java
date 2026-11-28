package br.ufpb.mercadinho.TESTES.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;
import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.mercadinho.dominio.desconto.SemDesconto;
import br.ufpb.mercadinho.excecao.EstoqueInsuficienteException;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;
import br.ufpb.mercadinho.excecao.TransicaoInvalidaException;


class PedidoTest {

    private Produto arroz;    // R$ 25,00 - 10 unidades
    private Produto feijao;   // R$  8,00 -  5 unidades

    @BeforeEach
    void preparar() {
        arroz = new Produto("A01", "Arroz 5kg", new Dinheiro(2500), new Quantidade(10), new Quantidade(2));
        feijao = new Produto("F01", "Feijão 1kg", new Dinheiro(800), new Quantidade(5), new Quantidade(1));
    }

    private Pedido novoPedido() {
        return new Pedido(1, "Maria", "ana", new SemDesconto());
    }

    private Pedido pedidoConfirmado() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(arroz, new Quantidade(1));
        pedido.confirmar();
        return pedido;
    }

    // ---------------- RN02 - transições ----------------

    @Test
    @DisplayName("RN02 - caminho permitido: ABERTO -> CONFIRMADO -> FINALIZADO")
    void cicloPermitido() {
        Pedido pedido = pedidoConfirmado();
        assertEquals(StatusPedido.CONFIRMADO, pedido.getStatus());

        pedido.finalizar();

        assertEquals(StatusPedido.FINALIZADO, pedido.getStatus());
    }

    @Test
    @DisplayName("RN02 - pedido FINALIZADO não pode ser cancelado (e o status não muda)")
    void naoCancelaPedidoFinalizado() {
        Pedido pedido = pedidoConfirmado();
        pedido.finalizar();

        assertThrows(TransicaoInvalidaException.class, () -> pedido.cancelar());
        assertEquals(StatusPedido.FINALIZADO, pedido.getStatus());
    }

    @Test
    @DisplayName("RN02 - não dá para pular a confirmação: ABERTO -> FINALIZADO é proibido")
    void naoFinalizaPedidoAberto() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(arroz, new Quantidade(1));

        assertThrows(TransicaoInvalidaException.class, () -> pedido.finalizar());
        assertEquals(StatusPedido.ABERTO, pedido.getStatus());
    }

    @Test
    @DisplayName("RN02 - pedido CANCELADO não volta a ser confirmado")
    void canceladoNaoPodeSerConfirmado() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(arroz, new Quantidade(1));
        pedido.cancelar();

        assertThrows(TransicaoInvalidaException.class, () -> pedido.confirmar());
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
    }

    @Test
    @DisplayName("RN02 - depois de CONFIRMADO o pedido não aceita novos itens")
    void naoIncluiItemEmPedidoConfirmado() {
        Pedido pedido = pedidoConfirmado();

        assertThrows(TransicaoInvalidaException.class, () -> pedido.adicionarItem(feijao, new Quantidade(1)));
        assertEquals(1, pedido.getItens().size());
    }

    @Test
    @DisplayName("Pedido sem itens não pode ser confirmado")
    void pedidoVazioNaoPodeSerConfirmado() {
        Pedido pedido = novoPedido();

        assertThrows(RegraDeNegocioException.class, () -> pedido.confirmar());
        assertEquals(StatusPedido.ABERTO, pedido.getStatus());
    }

    // ---------------- RN01 - disponibilidade (fronteiras) ----------------

    @Test
    @DisplayName("RN01 (fronteira) - pedir EXATAMENTE o disponível é permitido")
    void quantidadeIgualAoDisponivel() {
        Pedido pedido = novoPedido();

        pedido.adicionarItem(feijao, new Quantidade(5));   // disponível = 5

        assertEquals(new Quantidade(5), pedido.quantidadeDoProduto("F01"));
        assertEquals(new Dinheiro(4000), pedido.getTotal());   // 5 x R$ 8,00
    }

    @Test
    @DisplayName("RN01 (fronteira) - pedir 1 unidade a mais que o disponível é rejeitado")
    void quantidadeUmAcimaDoDisponivel() {
        Pedido pedido = novoPedido();

        assertThrows(EstoqueInsuficienteException.class, () -> pedido.adicionarItem(feijao, new Quantidade(6)));
        assertTrue(pedido.getItens().isEmpty());
    }

    @Test
    @DisplayName("RN01 - mesmo produto incluído duas vezes soma na mesma linha e respeita o disponível")
    void mesmoProdutoSomaNaMesmaLinha() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(feijao, new Quantidade(3));
        pedido.adicionarItem(feijao, new Quantidade(2));   // total 5 = disponível: ok

        assertEquals(1, pedido.getItens().size());
        assertEquals(new Quantidade(5), pedido.quantidadeDoProduto("F01"));
        // mais 1 deixaria o pedido com 6, acima do disponível
        assertThrows(EstoqueInsuficienteException.class, () -> pedido.adicionarItem(feijao, new Quantidade(1)));
        assertEquals(new Quantidade(5), pedido.quantidadeDoProduto("F01"));
    }
}
