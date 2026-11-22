public class Produto {

    private String codigoProduto;
    private String nomeProduto;
    private Dinheiro precoUnitario;
    private Quantidade estoqueFisico;
    private Quantidade estoqueReservado;
    private Quantidade estoqueMinimo;

    public Produto(String codigoProduto, String nomeProduto, Dinheiro precoUnitario,
                   Quantidade estoqueFisico, Quantidade estoqueReservado,
                   Quantidade estoqueMinimo) {

        this.codigoProduto = codigoProduto;
        this.nomeProduto = nomeProduto;
        this.precoUnitario = precoUnitario;
        this.estoqueFisico = estoqueFisico;
        this.estoqueReservado = estoqueReservado;
        this.estoqueMinimo = estoqueMinimo;

    }
    public Quantidade getEstoqueDisponivel() {
        return estoqueFisico.subtrair(estoqueReservado);
    }

    public void reservar(Quantidade q) {
        if (getEstoqueDisponivel().compareTo(q) < 0) {
            throw new RegraDeNegocioException("Estoque insuficiente");
        }

        estoqueReservado = estoqueReservado.somar(q);
    }

    public void liberarReserva(Quantidade q) {
        estoqueReservado = estoqueReservado.subtrair(q);
    }

    public void baixarEstoqueReservado(Quantidade q) {
        estoqueFisico = estoqueFisico.subtrair(q);
        estoqueReservado = estoqueReservado.subtrair(q);
    }

    public void registrarEntrada(Quantidade q) {
        estoqueFisico = estoqueFisico.somar(q);
    }

    public void registrarSaida(Quantidade q) {
        estoqueFisico = estoqueFisico.subtrair(q);
    }

    public void alterarPreco(Dinheiro novoPreco) {
        preco = novoPreco;
    }

    public boolean atingiuEstoqueMinimo() {
        return getEstoqueDisponivel().compareTo(estoqueMinimo) <= 0;
    }
    
    public String getCodigoProduto() {
        return codigoProduto;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public Dinheiro getPrecoUnitario() {
        return precoUnitario;
    }
}
