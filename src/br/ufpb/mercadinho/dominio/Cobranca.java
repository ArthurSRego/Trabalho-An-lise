package br.ufpb.mercadinho.dominio;

import br.ufpb.mercadinho.excecao.RegraDeNegocioException;


public class Cobranca {

    private final int numero;            // identificador único da cobrança
    private final int numeroPedido;      // pedido ao qual pertence
    private final Dinheiro valor;        // valor "congelado" no momento da confirmação
    private StatusCobranca status;

    /** Cria uma cobrança NOVA (começa PENDENTE). */
    public Cobranca(int numero, int numeroPedido, Dinheiro valor) {
        this(numero, numeroPedido, valor, StatusCobranca.PENDENTE);
    }

    /** Construtor completo, usado pelo repositório ao ler do arquivo. */
    public Cobranca(int numero, int numeroPedido, Dinheiro valor, StatusCobranca status) {
        if (numero <= 0 || numeroPedido <= 0) {
            throw new RegraDeNegocioException("Número da cobrança e do pedido devem ser positivos.");
        }
        this.numero = numero;
        this.numeroPedido = numeroPedido;
        this.valor = valor;
        this.status = status;
    }

    /** Pedido finalizado: o cliente pagou. */
    public void registrarPagamento() {
        exigirPendente("registrar o pagamento");
        status = StatusCobranca.PAGA;
    }

    /** Pedido confirmado foi cancelado: a cobrança deixa de valer. */
    public void cancelar() {
        exigirPendente("cancelar a cobrança");
        status = StatusCobranca.CANCELADA;
    }

    /** Só uma cobrança PENDENTE pode ser paga ou cancelada. */
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
