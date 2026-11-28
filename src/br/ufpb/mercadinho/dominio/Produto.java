package br.ufpb.mercadinho.dominio;

import br.ufpb.mercadinho.util.Texto;
import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import br.ufpb.mercadinho.excecao.EstoqueInsuficienteException;
import br.ufpb.mercadinho.excecao.RegraDeNegocioException;

public class Produto {

    private final String codigo;         
    private final String nome;
    private Dinheiro preco;             
    private Quantidade estoqueFisico;
    private Quantidade estoqueReservado;
    private final Quantidade estoqueMinimo;

    public Produto(String codigo, String nome, Dinheiro preco, Quantidade estoqueFisico, Quantidade estoqueMinimo) {
        this(codigo, nome, preco, estoqueFisico, Quantidade.ZERO, estoqueMinimo);
    }

    public Produto(String codigo, String nome, Dinheiro preco, Quantidade estoqueFisico,
                   Quantidade estoqueReservado, Quantidade estoqueMinimo) {
        if (codigo == null || !Texto.temApenasLetrasENumeros(codigo.trim())) {
            throw new RegraDeNegocioException("O código do produto deve ter apenas letras e números (recebido: \""
                    + codigo + "\").");
        }
        if (nome == null || nome.trim().isEmpty()) {
            throw new RegraDeNegocioException("O nome do produto é obrigatório.");
        }
        if (preco == null || preco.ehZero()) {
            throw new RegraDeNegocioException("O preço do produto deve ser maior que zero.");
        }
        if (estoqueReservado.ehMaiorQue(estoqueFisico)) {
            throw new RegraDeNegocioException("O estoque reservado (" + estoqueReservado
                    + ") não pode ser maior que o estoque físico (" + estoqueFisico + ").");
        }
        this.codigo = codigo.trim().toUpperCase();
        this.nome = nome.trim();
        this.preco = preco;
        this.estoqueFisico = estoqueFisico;
        this.estoqueReservado = estoqueReservado;
        this.estoqueMinimo = estoqueMinimo;
    }

    public Quantidade getEstoqueDisponivel() {
        return estoqueFisico.subtrair(estoqueReservado);
    }

    public void reservar(Quantidade quantidade) {
        if (quantidade.ehMaiorQue(getEstoqueDisponivel())) {
            throw new EstoqueInsuficienteException("Estoque insuficiente para " + descricaoCurta()
                    + ": foram pedidas " + quantidade + " unidade(s), mas só há "
                    + getEstoqueDisponivel() + " disponível(is).");
        }
        estoqueReservado = estoqueReservado.somar(quantidade);
    }

    public void liberarReserva(Quantidade quantidade) {
        if (quantidade.ehMaiorQue(estoqueReservado)) {
            throw new RegraDeNegocioException("Não é possível liberar " + quantidade + " unidade(s) de "
                    + descricaoCurta() + ": só há " + estoqueReservado + " reservada(s).");
        }
        estoqueReservado = estoqueReservado.subtrair(quantidade);
    }


    public void baixarEstoqueReservado(Quantidade quantidade) {
        if (quantidade.ehMaiorQue(estoqueReservado)) {
            throw new RegraDeNegocioException("Não é possível dar baixa em " + quantidade + " unidade(s) de "
                    + descricaoCurta() + ": só há " + estoqueReservado + " reservada(s).");
        }
        estoqueReservado = estoqueReservado.subtrair(quantidade);
        estoqueFisico = estoqueFisico.subtrair(quantidade);
    }

    public void registrarEntrada(Quantidade quantidade) {
        if (quantidade.ehZero()) {
            throw new RegraDeNegocioException("A quantidade de entrada deve ser de pelo menos 1 unidade.");
        }
        estoqueFisico = estoqueFisico.somar(quantidade);
    }


    public void registrarSaida(Quantidade quantidade) {
        if (quantidade.ehZero()) {
            throw new RegraDeNegocioException("A quantidade de saída deve ser de pelo menos 1 unidade.");
        }
        if (quantidade.ehMaiorQue(getEstoqueDisponivel())) {
            throw new EstoqueInsuficienteException("Não é possível retirar " + quantidade + " unidade(s) de "
                    + descricaoCurta() + ": só há " + getEstoqueDisponivel() + " disponível(is) ("
                    + estoqueReservado + " estão reservadas para pedidos confirmados).");
        }
        estoqueFisico = estoqueFisico.subtrair(quantidade);
    }

    public void alterarPreco(Dinheiro novoPreco) {
        if (novoPreco == null || novoPreco.ehZero()) {
            throw new RegraDeNegocioException("O preço do produto deve ser maior que zero.");
        }
        this.preco = novoPreco;
    }
    
    public boolean atingiuEstoqueMinimo() {
        return getEstoqueDisponivel().ehMenorOuIgualA(estoqueMinimo);
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public Dinheiro getPreco() {
        return preco;
    }

    public Quantidade getEstoqueFisico() {
        return estoqueFisico;
    }

    public Quantidade getEstoqueReservado() {
        return estoqueReservado;
    }

    public Quantidade getEstoqueMinimo() {
        return estoqueMinimo;
    }

    private String descricaoCurta() {
        return nome + " (" + codigo + ")";
    }

    @Override
    public String toString() {
        return descricaoCurta();
    }
}
