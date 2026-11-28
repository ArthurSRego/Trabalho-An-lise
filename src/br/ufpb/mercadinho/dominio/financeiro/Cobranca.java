package br.ufpb.mercadinho.dominio.financeiro;

import br.ufpb.mercadinho.excecao.RegraDeNegocioException;


public class Cobranca {

    private final int numero;            // identificador único da cobrança
    private final int numeroPedido;      // pedido ao qual pertence
    private final Dinheiro valor;        // valor "congelado" no momento da confirmação
    private StatusCobranca status;

    public Cobranca(int numero, int numeroPedido, Dinheiro valor) {
        this(numero, numeroPedido, valor, StatusCobranca.PENDENTE);
    }

    public Cobranca(int numero, int numeroPedido, Dinheiro valor, StatusCobranca status) {
        if (numero <= 0 || numeroPedido <= 0) {
            throw new RegraDeNegocioException("Número da cobrança e do pedido devem ser positivos.");
        }
        this.numero = numero;
        this.numeroPedido = numeroPedido;
        this.valor = valor;
        this.status = status;
    }

    public void registrarPagamento() {
        exigirPendente("registrar o pagamento");
        status = StatusCobranca.PAGA;
    }

    public void cancelar() {
        exigirPendente("cancelar a cobrança");
        status = StatusCobranca.CANCELADA;
    }


    private void exigirPendente(String acao) {
        if (status != StatusCobranca.PENDENTE) {
            throw new RegraDeNegocioException("Não é possível " + acao + ": a cobrança #" + numero
                    + " está " + status + ".");
        }
    }

    public int getNumero() {
        return numero;
    }

    public int getNumeroPedido() {
        return numeroPedido;
    }

    public Dinheiro getValor() {
        return valor;
    }

    public StatusCobranca getStatus() {
        return status;
    }
}
