package br.ufpb.mercadinho.dominio.desconto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.mercadinho.dominio.Produto;

import br.ufpb.mercadinho.excecao.RegraDeNegocioException;


class PoliticaDescontoTest {

    private Produto arroz;   // R$ 25,00

    @BeforeEach
    void preparar() {
        arroz = new Produto("A01", "Arroz 5kg", new Dinheiro(2500), new Quantidade(10), new Quantidade(2));
    }

    /** Pedido com 2 pacotes de arroz: subtotal de R$ 50,00, com a política informada. */
    private Pedido pedidoDeCinquentaReais(PoliticaDesconto politica) {
        Pedido pedido = new Pedido(1, "Maria", "ana", politica);
        pedido.adicionarItem(arroz, new Quantidade(2));
        return pedido;
    }

    @Test
    @DisplayName("Strategy/RN05 - o mesmo Pedido muda o total só trocando a política (sem mudar a classe Pedido)")
    void mesmoPedidoComPoliticasDiferentes() {
        assertEquals(new Dinheiro(5000), pedidoDeCinquentaReais(new SemDesconto()).getTotal());
        assertEquals(new Dinheiro(4750), pedidoDeCinquentaReais(new DescontoPercentual("PERC5", 5)).getTotal());
        assertEquals(new Dinheiro(4500),
                pedidoDeCinquentaReais(new DescontoValorFixo("FIXO5", new Dinheiro(500))).getTotal());
    }

    @Test
    @DisplayName("RN05 - desconto percentual arredonda para o centavo mais próximo")
    void percentualArredondaParaOCentavo() {
        DescontoPercentual cincoPorCento = new DescontoPercentual("PERC5", 5);

        // 5% de R$ 74,27 = R$ 3,7135 -> R$ 3,71
        assertEquals(new Dinheiro(371), cincoPorCento.calcularDesconto(new Dinheiro(7427)));
        // 5% de R$ 0,10 = R$ 0,005 -> R$ 0,01 (meio centavo arredonda para cima)
        assertEquals(new Dinheiro(1), cincoPorCento.calcularDesconto(new Dinheiro(10)));
    }

    @Test
    @DisplayName("Extensão valor fixo (fronteira) - desconto IGUAL ao subtotal deixa o total em R$ 0,00")
    void valorFixoIgualAoSubtotal() {
        DescontoValorFixo cincoReais = new DescontoValorFixo("FIXO5", new Dinheiro(500));

        assertEquals(new Dinheiro(500), cincoReais.calcularDesconto(new Dinheiro(500)));
    }

    @Test
    @DisplayName("Extensão valor fixo (fronteira) - desconto MAIOR que o subtotal é limitado: total nunca negativo")
    void valorFixoMaiorQueOSubtotal() {
        Produto bala = new Produto("B01", "Bala", new Dinheiro(300), new Quantidade(10), new Quantidade(0));
        Pedido pedido = new Pedido(2, "João", "ana", new DescontoValorFixo("FIXO5", new Dinheiro(500)));
        pedido.adicionarItem(bala, new Quantidade(1));   // subtotal R$ 3,00

        assertEquals(new Dinheiro(300), pedido.getDesconto());
        assertEquals(Dinheiro.ZERO, pedido.getTotal());
    }

    @Test
    @DisplayName("RN05 - percentual fora do intervalo de 1 a 100 é rejeitado")
    void percentualForaDoIntervalo() {
        assertThrows(RegraDeNegocioException.class, () -> new DescontoPercentual("ZERO", 0));
        assertThrows(RegraDeNegocioException.class, () -> new DescontoPercentual("DEMAIS", 101));
    }

    @Test
    @DisplayName("Catálogo - encontra a política pelo código e rejeita código inexistente ou repetido")
    void catalogoDeDescontos() {
        CatalogoDescontos catalogo = new CatalogoDescontos();
        catalogo.registrar(new SemDesconto());
        catalogo.registrar(new DescontoPercentual("PERC10", 10));

        assertEquals("PERC10", catalogo.buscar("perc10").getCodigo());   // maiúsculas/minúsculas tanto faz
        assertThrows(RegraDeNegocioException.class, () -> catalogo.buscar("NAOEXISTE"));
        assertThrows(RegraDeNegocioException.class, () -> catalogo.registrar(new DescontoPercentual("PERC10", 15)));
        assertEquals(2, catalogo.listar().size());
    }
}
