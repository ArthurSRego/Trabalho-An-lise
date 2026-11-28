package br.ufpb.mercadinho.visao;

import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;
import br.ufpb.mercadinho.servico.observador.ObservadorEstoque;
import br.ufpb.mercadinho.servico.observador.ObservadorPedido;

/** Observador concreto que apresenta notificações ao usuário no console. */
public class AvisosNoConsole implements ObservadorPedido, ObservadorEstoque {
    private final ConsoleView visao;

    public AvisosNoConsole(ConsoleView visao) {
        if (visao == null) throw new IllegalArgumentException("A visão do console é obrigatória.");
        this.visao = visao;
    }

    @Override
    public void statusDoPedidoMudou(Pedido pedido, StatusPedido statusAnterior, StatusPedido statusNovo) {
        visao.exibirMensagem("[PEDIDO] Pedido #" + pedido.getNumero() + " mudou de "
                + statusAnterior + " para " + statusNovo + ".");
    }

    @Override
    public void estoqueAtingiuMinimo(Produto produto) {
        visao.exibirMensagem("[ESTOQUE] Atenção: " + produto.getNome() + " (" + produto.getCodigo()
                + ") atingiu o estoque mínimo. Disponível: " + produto.getEstoqueDisponivel() + ".");
    }
}
