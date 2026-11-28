package br.ufpb.mercadinho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import br.ufpb.mercadinho.TESTES.AmbienteDeTeste;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;
import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import br.ufpb.mercadinho.excecao.AcessoNegadoException;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;

/**
 * Testes do padrão FACADE: a tela consegue executar os casos de uso inteiros
 * conhecendo SÓ a MercadinhoFacade (sem tocar em serviços ou repositórios).
 * Também cobre a sessão (login) e a RN06 vista pela fachada.
 */
class MercadinhoFacadeTest {

    private AmbienteDeTeste amb;
    private MercadinhoFacade facade;

    @BeforeEach
    void preparar() throws IOException {
        amb = AmbienteDeTeste.novo();
        facade = amb.facade;
    }

    @AfterEach
    void limpar() {
        amb.apagarArquivos();
    }

    @Test
    @DisplayName("Facade - fluxo completo de pedido usando só a fachada (porta única da interface)")
    void fluxoCompletoPelaFachada() {
        facade.entrar("ana");
        Pedido pedido = facade.registrarPedido("Maria", "FIXO5");
        facade.adicionarItem(pedido.getNumero(), "a01", 1);   // código em minúsculas também funciona
        facade.adicionarItem(pedido.getNumero(), "F01", 2);   // R$ 25,00 + R$ 16,00 - R$ 5,00 = R$ 36,00

        assertTrue(facade.confirmarPedido(pedido.getNumero()));
        assertEquals(new Dinheiro(3600), facade.buscarCobrancaDoPedido(pedido.getNumero()).getValor());

        facade.finalizarPedido(pedido.getNumero());
        assertEquals(StatusPedido.FINALIZADO, facade.buscarPedido(pedido.getNumero()).getStatus());
        assertEquals(9, amb.fisico("A01"));
        assertEquals(3, amb.fisico("F01"));

        facade.sair();
        assertNull(facade.getUsuarioLogado());
    }

    @Test
    @DisplayName("Facade + RN06 - sem login a fachada não deixa consultar nem registrar pedidos")
    void semLoginNaoFazNada() {
        assertThrows(AcessoNegadoException.class, () -> facade.listarPedidos());
        assertThrows(AcessoNegadoException.class, () -> facade.registrarPedido("Maria", "SEM"));
        assertTrue(amb.pedidos.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("Facade + RN06 - mesma ação negada ao ATENDENTE e permitida ao GERENTE")
    void perfisComPermissoesDiferentes() {
        facade.entrar("ana");
        assertThrows(AcessoNegadoException.class, () -> facade.registrarEntradaDeEstoque("A01", 5));
        assertEquals(10, amb.fisico("A01"));

        facade.sair();
        facade.entrar("carlos");
        facade.registrarEntradaDeEstoque("A01", 5);
        assertEquals(15, amb.fisico("A01"));
    }

    @Test
    @DisplayName("Login inexistente é rejeitado e ninguém fica logado")
    void loginInexistente() {
        assertThrows(RegraDeNegocioException.class, () -> facade.entrar("fulano"));
        assertNull(facade.getUsuarioLogado());
    }
}
