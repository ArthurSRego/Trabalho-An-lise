package br.ufpb.mercadinho.TESTES.servico.observador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import br.ufpb.mercadinho.TESTES.AmbienteDeTeste;
import br.ufpb.mercadinho.TESTES.ProdutoRepositorioQueFalha;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import br.ufpb.mercadinho.excecao.PersistenciaException;
import br.ufpb.mercadinho.servico.PedidoService;

class ObservadorTest {

    private AmbienteDeTeste amb;

    @BeforeEach
    void preparar() throws IOException {
        amb = AmbienteDeTeste.novo();
    }

    @AfterEach
    void limpar() {
        amb.apagarArquivos();
    }

    @Test
    @DisplayName("Observer - o observador inscrito é avisado quando o pedido muda de status")
    void avisaMudancaDeStatus() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 1);

        amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());
        amb.pedidoService.finalizarPedido(amb.atendente, pedido.getNumero());

        assertTrue(amb.observador.avisos.contains("Pedido " + pedido.getNumero() + ": ABERTO -> CONFIRMADO"));
        assertTrue(amb.observador.avisos.contains("Pedido " + pedido.getNumero() + ": CONFIRMADO -> FINALIZADO"));
    }

    @Test
    @DisplayName("Observer (fronteira) - disponível chega EXATAMENTE ao mínimo: o alerta é enviado")
    void alertaQuandoChegaAoMinimo() {
        Pedido pedido = amb.criarPedido("SEM", "C01", 1);   // 6 - 1 = 5 = mínimo

        amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());

        assertTrue(amb.observador.avisos.contains("Estoque mínimo: C01"));
    }

    @Test
    @DisplayName("Observer (fronteira) - disponível fica 1 acima do mínimo: nenhum alerta")
    void semAlertaUmAcimaDoMinimo() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 7);   // 10 - 7 = 3 = mínimo + 1

        amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());

        assertFalse(amb.observador.avisos.contains("Estoque mínimo: A01"));
    }

    @Test
    @DisplayName("Observer - se a operação falha e é desfeita, ninguém é avisado")
    void operacaoQueFalhaNaoAvisaNinguem() {
        Pedido pedido = amb.criarPedido("SEM", "C01", 1, "A01", 1);
        PedidoService servicoComFalha = new PedidoService(amb.pedidos, new ProdutoRepositorioQueFalha(amb.produtos, 2),
                amb.cobrancas, amb.banco, amb.catalogo, amb.notificacoes);

        assertThrows(PersistenciaException.class,
                () -> servicoComFalha.confirmarPedido(amb.atendente, pedido.getNumero()));

        assertTrue(amb.observador.avisos.isEmpty());
    }

    @Test
    @DisplayName("Observer - depois de removido, o observador não recebe mais avisos")
    void observadorRemovidoNaoRecebeAvisos() {
        amb.notificacoes.removerObservadorDePedido(amb.observador);
        Pedido pedido = amb.criarPedido("SEM", "A01", 1);

        amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());

        assertEquals(0, amb.observador.avisos.size());
    }
}
