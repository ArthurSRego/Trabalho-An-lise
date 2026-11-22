public class Item {
    private String nomeProduto;
    private int idProduto;
    private double quantiaProduto;
    private double preçoProduto;

    public Item(String nomeProduto, int idProduto, double quantiaProduto, double preçoProduto) {
        this.nomeProduto = nomeProduto;
        this.idProduto = idProduto;
        this.quantiaProduto = quantiaProduto;
        this.preçoProduto = preçoProduto;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public int getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(int idProduto) {
        this.idProduto = idProduto;
    }

    public double getQuantiaProduto() {
        return quantiaProduto;
    }

    public void setQuantiaProduto(double quantiaProduto) {
        this.quantiaProduto = quantiaProduto;
    }

    public double getPreçoProduto() {
        return preçoProduto;
    }

    public void setPreçoProduto(double preçoProduto) {
        this.preçoProduto = preçoProduto;
    }
}