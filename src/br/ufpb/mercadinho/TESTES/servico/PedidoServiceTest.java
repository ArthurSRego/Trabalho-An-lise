package br.ufpb.mercadinho.TESTES.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import br.ufpb.mercadinho.TESTES.AmbienteDeTeste;
import br.ufpb.mercadinho.TESTES.ProdutoRepositorioQueFalha;
import br.ufpb.mercadinho.dominio.financeiro.Cobranca;
import br.ufpb.mercadinho.dominio.financeiro.StatusCobranca;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;
import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import br.ufpb.mercadinho.servico.PedidoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import br.ufpb.mercadinho.excecao.AcessoNegadoException;
import br.ufpb.mercadinho.excecao.EstoqueInsuficienteException;
import br.ufpb.mercadinho.excecao.PersistenciaException;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;
import br.ufpb.mercadinho.excecao.TransicaoInvalidaException;


class PedidoServiceTest {

    private AmbienteDeTeste amb;

    @BeforeEach
    void preparar() throws IOException {
        amb = AmbienteDeTeste.novo();
    }

    @AfterEach
    void limpar() {
        amb.apagarArquivos();
    }

    private StatusPedido statusNoArquivo(int numeroPedido) {
        return amb.pedidos.buscarPorNumero(numeroPedido).getStatus();
    }

    // ---------------- UC02 - confirmar e finalizar ----------------

    @Test
    @DisplayName("UC02 - confirmar RESERVA o estoque (o físico não muda) e gera UMA cobrança pendente")
    void confirmarReservaEstoqueEGeraCobranca() {
        Pedido pedido = amb.criarPedido("PERC10", "A01", 2);   // 2 x R$ 25,00 = R$ 50,00 - 10% = R$ 45,00

        boolean confirmouAgora = amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());

        assertTrue(confirmouAgora);
        assertEquals(StatusPedido.CONFIRMADO, statusNoArquivo(pedido.getNumero()));
        assertEquals(10, amb.fisico("A01"));
        assertEquals(2, amb.reservado("A01"));
        assertEquals(8, amb.disponivel("A01"));
        Cobranca cobranca = amb.cobrancas.buscarPorPedido(pedido.getNumero());
        assertEquals(new Dinheiro(4500), cobranca.getValor());
        assertEquals(StatusCobranca.PENDENTE, cobranca.getStatus());
    }

    @Test
    @DisplayName("Finalizar BAIXA o estoque reservado (físico e reservado diminuem) e a cobrança fica PAGA")
    void finalizarBaixaOEstoque() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 2);
        amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());

        amb.pedidoService.finalizarPedido(amb.atendente, pedido.getNumero());

        assertEquals(StatusPedido.FINALIZADO, statusNoArquivo(pedido.getNumero()));
        assertEquals(8, amb.fisico("A01"));
        assertEquals(0, amb.reservado("A01"));
        assertEquals(StatusCobranca.PAGA, amb.cobrancas.buscarPorPedido(pedido.getNumero()).getStatus());
    }

    @Test
    @DisplayName("RN03 - confirmar DUAS vezes não reserva de novo nem gera outra cobrança (nem após reiniciar)")
    void confirmacaoRepetidaNaoDuplicaEfeitos() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 3);

        assertTrue(amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero()));
        assertFalse(amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero()));   // repetição

        assertEquals(3, amb.reservado("A01"));                       // reservou uma vez só
        assertEquals(1, amb.cobrancasDoPedido(pedido.getNumero()));   // uma única cobrança

        AmbienteDeTeste depois = amb.reiniciar();                     // e continua assim no arquivo
        assertEquals(3, depois.reservado("A01"));
        assertEquals(1, depois.cobrancasDoPedido(pedido.getNumero()));
    }

    // ---------------- UC03 - cancelar ----------------

    @Test
    @DisplayName("UC03 - cancelar pedido ABERTO: nada a desfazer (estoque igual e sem cobrança)")
    void cancelarPedidoAberto() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 2);

        StatusPedido statusAnterior = amb.pedidoService.cancelarPedido(amb.atendente, pedido.getNumero());

        assertEquals(StatusPedido.ABERTO, statusAnterior);
        assertEquals(StatusPedido.CANCELADO, statusNoArquivo(pedido.getNumero()));
        assertEquals(10, amb.fisico("A01"));
        assertEquals(0, amb.reservado("A01"));
        assertNull(amb.cobrancas.buscarPorPedido(pedido.getNumero()));
    }

    @Test
    @DisplayName("UC03 - cancelar pedido CONFIRMADO libera a reserva e cancela a cobrança")
    void cancelarPedidoConfirmado() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 2);
        amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());

        StatusPedido statusAnterior = amb.pedidoService.cancelarPedido(amb.atendente, pedido.getNumero());

        assertEquals(StatusPedido.CONFIRMADO, statusAnterior);
        assertEquals(0, amb.reservado("A01"));
        assertEquals(10, amb.fisico("A01"));   // o produto nunca saiu da loja
        assertEquals(StatusCobranca.CANCELADA, amb.cobrancas.buscarPorPedido(pedido.getNumero()).getStatus());
    }

    @Test
    @DisplayName("RN02 - cancelar pedido FINALIZADO é proibido e nada muda (status, estoque e cobrança)")
    void naoCancelaPedidoFinalizado() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 2);
        amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());
        amb.pedidoService.finalizarPedido(amb.atendente, pedido.getNumero());

        assertThrows(TransicaoInvalidaException.class,
                () -> amb.pedidoService.cancelarPedido(amb.atendente, pedido.getNumero()));

        assertEquals(StatusPedido.FINALIZADO, statusNoArquivo(pedido.getNumero()));
        assertEquals(8, amb.fisico("A01"));
        assertEquals(StatusCobranca.PAGA, amb.cobrancas.buscarPorPedido(pedido.getNumero()).getStatus());
    }

    // ---------------- RN01 - disponibilidade ----------------

    @Test
    @DisplayName("RN01 - ao incluir item vale o DISPONÍVEL (físico - reservado), e não o estoque físico")
    void incluirItemConsideraOEstoqueReservado() {
        Pedido primeiro = amb.criarPedido("SEM", "F01", 3);
        amb.pedidoService.confirmarPedido(amb.atendente, primeiro.getNumero());   // F01: físico 5, reservado 3, disponível 2
        Pedido segundo = amb.pedidoService.registrarPedido(amb.atendente, "João", "SEM");

        assertThrows(EstoqueInsuficienteException.class,
                () -> amb.pedidoService.adicionarItem(amb.atendente, segundo.getNumero(), "F01", 3));

        amb.pedidoService.adicionarItem(amb.atendente, segundo.getNumero(), "F01", 2);   // exatamente o disponível
        assertEquals(new Quantidade(2), amb.pedidos.buscarPorNumero(segundo.getNumero()).quantidadeDoProduto("F01"));
    }

    // ---------------- RN01 + RN04 - falhas no meio da confirmação ----------------

    @Test
    @DisplayName("RN01 + RN04 - faltou estoque no 2º item ao confirmar: o 1º item NÃO fica reservado")
    void faltaDeEstoqueNaConfirmacaoNaoDeixaReservaParcial() {
        // Pedido 1: 2 arroz + 5 feijão (cabe no disponível quando os itens são incluídos).
        Pedido pedido1 = amb.criarPedido("SEM", "A01", 2, "F01", 5);
        // Pedido 2 pega 1 feijão e é confirmado antes: sobram só 4 feijões disponíveis.
        Pedido pedido2 = amb.criarPedido("SEM", "F01", 1);
        amb.pedidoService.confirmarPedido(amb.atendente, pedido2.getNumero());

        assertThrows(EstoqueInsuficienteException.class,
                () -> amb.pedidoService.confirmarPedido(amb.atendente, pedido1.getNumero()));

        assertEquals(0, amb.reservado("A01"));   // o arroz (1º item) foi desfeito
        assertEquals(1, amb.reservado("F01"));   // só a reserva do pedido 2
        assertEquals(StatusPedido.ABERTO, statusNoArquivo(pedido1.getNumero()));
        assertNull(amb.cobrancas.buscarPorPedido(pedido1.getNumero()));
    }

    @Test
    @DisplayName("RN04 (desafio) - falha NO MEIO da confirmação: nada é gravado, nem na memória nem no arquivo")
    void falhaNoMeioDaConfirmacaoDesfazTudo() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 2, "F01", 3);
        // Mesmo serviço, mas com um repositório que falha no 2º salvar() (depois de reservar o arroz).
        PedidoService servicoComFalha = new PedidoService(amb.pedidos, new ProdutoRepositorioQueFalha(amb.produtos, 2),
                amb.cobrancas, amb.banco, amb.catalogo, amb.notificacoes);

        assertThrows(PersistenciaException.class,
                () -> servicoComFalha.confirmarPedido(amb.atendente, pedido.getNumero()));

        // Na memória: tudo como antes.
        assertEquals(0, amb.reservado("A01"));
        assertEquals(0, amb.reservado("F01"));
        assertEquals(StatusPedido.ABERTO, statusNoArquivo(pedido.getNumero()));
        assertEquals(0, amb.cobrancasDoPedido(pedido.getNumero()));

        // No arquivo (reiniciando o sistema): também tudo como antes...
        AmbienteDeTeste depois = amb.reiniciar();
        assertEquals(0, depois.reservado("A01"));
        assertEquals(StatusPedido.ABERTO, depois.pedidos.buscarPorNumero(pedido.getNumero()).getStatus());
        // ...e o pedido continua podendo ser confirmado normalmente.
        assertTrue(depois.pedidoService.confirmarPedido(depois.atendente, pedido.getNumero()));
        assertEquals(2, depois.reservado("A01"));
        assertEquals(3, depois.reservado("F01"));
    }

    @Test
    @DisplayName("RN04 - falha ao GRAVAR no disco: a operação é desfeita e o arquivo anterior fica intacto")
    void falhaAoGravarNoDisco() throws IOException {
        Pedido pedido = amb.criarPedido("SEM", "A01", 2);
        // Uma PASTA com o nome do arquivo temporário impede a gravação (simula um problema no disco).
        Path bloqueio = Paths.get(amb.arquivo.toString() + ".tmp");
        Files.createDirectory(bloqueio);

        assertThrows(PersistenciaException.class,
                () -> amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero()));
        assertEquals(0, amb.reservado("A01"));
        assertEquals(StatusPedido.ABERTO, statusNoArquivo(pedido.getNumero()));

        Files.delete(bloqueio);
        AmbienteDeTeste depois = amb.reiniciar();
        assertEquals(0, depois.reservado("A01"));
        assertEquals(StatusPedido.ABERTO, depois.pedidos.buscarPorNumero(pedido.getNumero()).getStatus());
    }

    // ---------------- consistência e autorização ----------------

    @Test
    @DisplayName("Consistência - não aceita item de produto inexistente")
    void naoAceitaProdutoInexistente() {
        Pedido pedido = amb.pedidoService.registrarPedido(amb.atendente, "Maria", "SEM");

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> amb.pedidoService.adicionarItem(amb.atendente, pedido.getNumero(), "X99", 1));

        assertTrue(erro.getMessage().contains("X99"));
        assertTrue(amb.pedidos.buscarPorNumero(pedido.getNumero()).getItens().isEmpty());
    }

    @Test
    @DisplayName("RN06 - sem login não é possível registrar pedido")
    void semLoginNaoRegistraPedido() {
        assertThrows(AcessoNegadoException.class, () -> amb.pedidoService.registrarPedido(null, "Maria", "SEM"));
        assertTrue(amb.pedidos.listarTodos().isEmpty());
    }
}
