package br.ufpb.mercadinho.servico.observador;

import br.ufpb.mercadinho.dominio.Pedido;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.StatusPedido;

import java.util.ArrayList;
import java.util.List;


public class CentralDeNotificacoes {

    private final List<ObservadorPedido> observadoresDePedido = new ArrayList<>();
    private final List<ObservadorEstoque> observadoresDeEstoque = new ArrayList<>();

    public void inscreverObservadorDePedido(ObservadorPedido observador) {
        observadoresDePedido.add(observador);
    }

    public void inscreverObservadorDeEstoque(ObservadorEstoque observador) {
        observadoresDeEstoque.add(observador);
    }

    public void removerObservadorDePedido(ObservadorPedido observador) {
        observadoresDePedido.remove(observador);
    }

    public void removerObservadorDeEstoque(ObservadorEstoque observador) {
        observadoresDeEstoque.remove(observador);
    }

    public void notificarMudancaDeStatus(Pedido pedido, StatusPedido statusAnterior, StatusPedido statusNovo) {
        for (ObservadorPedido observador : observadoresDePedido) {
            try {
                observador.statusDoPedidoMudou(pedido, statusAnterior, statusNovo);
            } catch (RuntimeException e) {
                System.err.println("Aviso: o observador " + observador.getClass().getSimpleName()
                        + " falhou ao ser notificado: " + e);
            }
        }
    }

    public void notificarEstoqueMinimo(Produto produto) {
        for (ObservadorEstoque observador : observadoresDeEstoque) {
            try {
                observador.estoqueAtingiuMinimo(produto);
            } catch (RuntimeException e) {
                System.err.println("Aviso: o observador " + observador.getClass().getSimpleName()
                        + " falhou ao ser notificado: " + e);
            }
        }
    }
}
