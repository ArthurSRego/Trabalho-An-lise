package br.ufpb.mercadinho.visao;

import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;
import br.ufpb.mercadinho.servico.observador.ObservadorEstoque;
import br.ufpb.mercadinho.servico.observador.ObservadorPedido;


public class AvisosNoConsole implements ObservadorPedido, ObservadorEstoque {

    private final ConsoleView view;

    public AvisosNoConsole(ConsoleView view) {
        this.view = view;
    }

    @Override
    public void statusDoPedidoMudou(Pedido pedido, StatusPedido statusAnterior, StatusPedido statusNovo) {
        view.mostrarNotificacao("Pedido #" + pedido.getNumero() + " (" + pedido.getCliente() + "): "
                + statusAnterior + " -> " + statusNovo);
    }

    @Override
    public void estoqueAtingiuMinimo(Produto produto) {
        view.mostrarAlerta(produto.getNome() + " (" + produto.getCodigo() + ") está com "
                + produto.getEstoqueDisponivel() + " unidade(s) disponível(is); o mínimo é "
                + produto.getEstoqueMinimo() + ". Avise o gerente para repor.");
    }
}
