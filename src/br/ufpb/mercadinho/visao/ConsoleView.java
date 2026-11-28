package br.ufpb.mercadinho.visao;

import br.ufpb.mercadinho.dominio.*;
import br.ufpb.mercadinho.dominio.desconto.PoliticaDesconto;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

public class ConsoleView {
    private final Scanner entrada;
    private final PrintStream saida;

    public ConsoleView() {
        this(new Scanner(System.in), System.out);
    }

    public ConsoleView(Scanner entrada, PrintStream saida) {
        if (entrada == null || saida == null) throw new IllegalArgumentException("Entrada e saída são obrigatórias.");
        this.entrada = entrada;
        this.saida = saida;
    }

    public void exibirMensagem(String mensagem) { saida.println(mensagem); }

    public void exibirErro(String mensagem) { saida.println("Erro: " + mensagem); }

    public void exibirCabecalho(String titulo) {
        saida.println();
        saida.println("========================================");
        saida.println("  " + titulo);
        saida.println("========================================");
    }

    public void exibirMenuPrincipal() {
        exibirCabecalho("MERCADINHO BOM JESUS");
        saida.println("1 - Listar produtos");
        saida.println("2 - Criar pedido");
        saida.println("3 - Listar pedidos");
        saida.println("0 - Sair");
    }

    public String lerTexto(String mensagem) {
        saida.print(mensagem);
        saida.flush();
        if (!entrada.hasNextLine()) throw new FimDaEntradaException();
        return entrada.nextLine().trim();
    }

    public int lerInteiro(String mensagem) {
        while (true) {
            String texto = lerTexto(mensagem);
            try { return Integer.parseInt(texto); }
            catch (NumberFormatException e) { exibirErro("Digite um número inteiro válido."); }
        }
    }

    public long lerLong(String mensagem) {
        while (true) {
            String texto = lerTexto(mensagem);
            try { return Long.parseLong(texto); }
            catch (NumberFormatException e) { exibirErro("Digite um número válido."); }
        }
    }

    public void exibirProdutos(List<Produto> produtos) {
        exibirCabecalho("PRODUTOS");
        if (produtos == null || produtos.isEmpty()) { saida.println("Nenhum produto cadastrado."); return; }
        for (Produto p : produtos) {
            saida.println(p.getCodigo() + " - " + p.getNome() + " | Preço: " + p.getPreco()
                    + " | Disponível: " + p.getEstoqueDisponivel());
        }
    }

    public void exibirPedidos(List<Pedido> pedidos) {
        exibirCabecalho("PEDIDOS");
        if (pedidos == null || pedidos.isEmpty()) { saida.println("Nenhum pedido cadastrado."); return; }
        for (Pedido p : pedidos) {
            saida.println("#" + p.getNumero() + " - " + p.getCliente() + " | " + p.getStatus()
                    + " | Total: " + p.getTotal());
        }
    }

    public void exibirPedido(Pedido pedido) {
        exibirCabecalho("PEDIDO #" + pedido.getNumero());
        saida.println("Cliente: " + pedido.getCliente());
        saida.println("Atendente: " + pedido.getLoginAtendente());
        saida.println("Status: " + pedido.getStatus());
        for (ItemPedido item : pedido.getItens()) {
            saida.println(item.getQuantidade() + " x " + item.getNomeProduto() + " = " + item.getSubtotal());
        }
        saida.println("Subtotal: " + pedido.getSubtotal());
        saida.println("Desconto: " + pedido.getDesconto());
        saida.println("Total: " + pedido.getTotal());
    }

    public void exibirPoliticasDesconto(List<PoliticaDesconto> politicas) {
        exibirCabecalho("POLÍTICAS DE DESCONTO");
        for (PoliticaDesconto politica : politicas)
            saida.println(politica.getCodigo() + " - " + politica.getDescricao());
    }
}
