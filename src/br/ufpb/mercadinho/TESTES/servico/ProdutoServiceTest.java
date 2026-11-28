package br.ufpb.mercadinho.TESTES.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import br.ufpb.mercadinho.TESTES.AmbienteDeTeste;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.excecao.AcessoNegadoException;
import br.ufpb.mercadinho.excecao.EstoqueInsuficienteException;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;


class ProdutoServiceTest {

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
    @DisplayName("UC04 - gerente cadastra produto e ele continua salvo depois de reiniciar")
    void gerenteCadastraProduto() {
        amb.produtoService.cadastrarProduto(amb.gerente, "m01", "Macarrão 500g", new Dinheiro(429), 20, 4);

        Produto salvo = amb.reiniciar().produto("M01");
        assertEquals("Macarrão 500g", salvo.getNome());
        assertEquals(20, salvo.getEstoqueFisico().getValor());
        assertEquals(new Dinheiro(429), salvo.getPreco());
    }

    @Test
    @DisplayName("RN06 - ATENDENTE não pode cadastrar produto (autorização negada) e nada é gravado")
    void atendenteNaoPodeCadastrar() {
        AcessoNegadoException erro = assertThrows(AcessoNegadoException.class,
                () -> amb.produtoService.cadastrarProduto(amb.atendente, "M01", "Macarrão 500g", new Dinheiro(429), 20, 4));

        assertTrue(erro.getMessage().contains("GERENTE"));
        assertNull(amb.produtos.buscarPorCodigo("M01"));
    }

    @Test
    @DisplayName("RN06 - ATENDENTE não pode registrar saída de estoque")
    void atendenteNaoPodeMexerNoEstoque() {
        assertThrows(AcessoNegadoException.class, () -> amb.produtoService.registrarSaida(amb.atendente, "A01", 1));
        assertEquals(10, amb.fisico("A01"));
    }

    @Test
    @DisplayName("Consistência - código de produto repetido é rejeitado e o produto original não muda")
    void codigoRepetidoEhRejeitado() {
        assertThrows(RegraDeNegocioException.class,
                () -> amb.produtoService.cadastrarProduto(amb.gerente, "a01", "Outro Arroz", new Dinheiro(100), 1, 0));

        assertEquals("Arroz 5kg", amb.produto("A01").getNome());
        assertEquals(3, amb.produtos.listarTodos().size());
    }

    @Test
    @DisplayName("Consistência (fronteira) - a saída pode zerar o disponível, mas não pode usar o reservado")
    void saidaNaoPodeUsarEstoqueReservado() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 4);
        amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());   // físico 10, reservado 4, disponível 6

        assertThrows(EstoqueInsuficienteException.class, () -> amb.produtoService.registrarSaida(amb.gerente, "A01", 7));
        assertEquals(10, amb.fisico("A01"));

        amb.produtoService.registrarSaida(amb.gerente, "A01", 6);   // exatamente o disponível: permitido
        assertEquals(4, amb.fisico("A01"));
        assertEquals(4, amb.reservado("A01"));
        assertEquals(0, amb.disponivel("A01"));
    }

    @Test
    @DisplayName("UC04 - entrada de mercadoria aumenta o físico e o disponível")
    void entradaAumentaODisponivel() {
        amb.produtoService.registrarEntrada(amb.gerente, "F01", 7);

        assertEquals(12, amb.fisico("F01"));
        assertEquals(12, amb.disponivel("F01"));
    }

    @Test
    @DisplayName("UC04 - novo preço vale para pedidos novos; itens já incluídos mantêm o preço antigo")
    void alterarPrecoNaoMudaPedidoExistente() {
        Pedido pedido = amb.criarPedido("SEM", "A01", 2);   // 2 x R$ 25,00

        amb.produtoService.alterarPreco(amb.gerente, "A01", new Dinheiro(3000));

        assertEquals(new Dinheiro(3000), amb.produto("A01").getPreco());
        assertEquals(new Dinheiro(5000), amb.pedidos.buscarPorNumero(pedido.getNumero()).getTotal());
    }
}
