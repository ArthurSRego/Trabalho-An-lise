package br.ufpb.mercadinho.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import br.ufpb.mercadinho.TESTES.AmbienteDeTeste;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;
import br.ufpb.mercadinho.dominio.usuario.Perfil;
import br.ufpb.mercadinho.dominio.usuario.Usuario;
import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import br.ufpb.mercadinho.excecao.PersistenciaException;
import br.ufpb.mercadinho.repositorio.ProdutoRepositorio;


class PersistenciaTest {

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
    @DisplayName("Singleton - getInstancia() devolve sempre o MESMO objeto")
    void sempreAMesmaInstancia() {
        assertSame(BancoDeDados.getInstancia(), BancoDeDados.getInstancia());
        assertSame(amb.banco, BancoDeDados.getInstancia());
    }

    @Test
    @DisplayName("Singleton - repositórios diferentes enxergam os mesmos dados e a mesma transação")
    void repositoriosCompartilhamAConexao() {
        ProdutoRepositorio outroRepositorio = new ProdutoRepositorioArquivo(BancoDeDados.getInstancia());

        amb.banco.iniciar();
        outroRepositorio.salvar(new Produto("T01", "Tomate", new Dinheiro(100), new Quantidade(3), new Quantidade(0)));
        amb.usuarios.salvar(new Usuario("bia", "Bia", Perfil.ATENDENTE));
        assertNotNull(amb.produtos.buscarPorCodigo("T01"));   // o repositório original já enxerga o Tomate

        amb.banco.desfazer();   // UMA transação desfaz as alterações dos DOIS repositórios
        assertNull(amb.produtos.buscarPorCodigo("T01"));
        assertNull(amb.usuarios.buscarPorLogin("bia"));
    }

    @Test
    @DisplayName("Persistência - pedido, itens, desconto, cobrança e estoque continuam iguais após reiniciar")
    void dadosSobrevivemAoReinicio() {
        Pedido pedido = amb.criarPedido("PERC10", "A01", 2, "F01", 1);   // 50,00 + 8,00 = 58,00 - 10% = 52,20
        amb.pedidoService.confirmarPedido(amb.atendente, pedido.getNumero());

        AmbienteDeTeste depois = amb.reiniciar();   // tudo relido do arquivo

        Pedido lido = depois.pedidos.buscarPorNumero(pedido.getNumero());
        assertEquals(StatusPedido.CONFIRMADO, lido.getStatus());
        assertEquals("Maria", lido.getCliente());
        assertEquals(2, lido.getItens().size());
        assertEquals("PERC10", lido.getPoliticaDesconto().getCodigo());
        assertEquals(new Dinheiro(5220), lido.getTotal());
        assertEquals(new Dinheiro(5220), depois.cobrancas.buscarPorPedido(pedido.getNumero()).getValor());
        assertEquals(2, depois.reservado("A01"));
        assertEquals(1, depois.reservado("F01"));
        // identificador único: o próximo número continua a sequência
        assertEquals(pedido.getNumero() + 1, depois.pedidos.proximoNumero());
    }

    @Test
    @DisplayName("Persistência - sobra de gravação interrompida (.tmp) é descartada e os dados oficiais ficam")
    void temporarioDeGravacaoInterrompidaEhDescartado() throws IOException {
        Path temporario = Paths.get(amb.arquivo.toString() + ".tmp");
        Files.write(temporario, "[produtos]\nA01;Arroz pela met".getBytes(StandardCharsets.UTF_8));

        AmbienteDeTeste depois = amb.reiniciar();

        assertFalse(Files.exists(temporario));
        assertEquals(3, depois.produtos.listarTodos().size());
        assertEquals("Arroz 5kg", depois.produto("A01").getNome());
    }

    @Test
    @DisplayName("Persistência - alteração fora de uma transação é recusada (nada é gravado sem proteção)")
    void alteracaoForaDeTransacaoEhRecusada() {
        Produto tomate = new Produto("T01", "Tomate", new Dinheiro(100), new Quantidade(3), new Quantidade(0));

        assertThrows(IllegalStateException.class, () -> amb.produtos.salvar(tomate));
        assertNull(amb.produtos.buscarPorCodigo("T01"));
    }

    @Test
    @DisplayName("Persistência - arquivo estragado gera erro claro, com a linha e a causa original")
    void arquivoEstragadoGeraErroClaro() throws IOException {
        Files.write(amb.arquivo, "[produtos]\nP001;Arroz;caro;10;0;2\n".getBytes(StandardCharsets.UTF_8));
        amb.banco.conectar(amb.arquivo.toString());

        PersistenciaException erro = assertThrows(PersistenciaException.class,
                () -> new ProdutoRepositorioArquivo(amb.banco).listarTodos());

        assertTrue(erro.getMessage().contains("P001;Arroz;caro"));
        assertNotNull(erro.getCause());   // a causa original (NumberFormatException) foi preservada
    }
}
