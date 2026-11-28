package br.ufpb.mercadinho.visao;

import java.util.List;
import java.util.Scanner;

import br.ufpb.mercadinho.dominio.financeiro.Cobranca;
import br.ufpb.mercadinho.dominio.pedido.*;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.usuario.Usuario;
import br.ufpb.mercadinho.dominio.desconto.PoliticaDesconto;

public class ConsoleView {

    private static final String LINHA = "------------------------------------------------------------------------";

    private final Scanner teclado;

    public ConsoleView(Scanner teclado) {
        this.teclado = teclado;
    }

    public void mostrar(String texto) {
        System.out.println(texto);
    }

    public void mostrarTitulo(String titulo) {
        System.out.println();
        System.out.println(LINHA);
        System.out.println("  " + titulo);
        System.out.println(LINHA);
    }

    public void mostrarSucesso(String texto) {
        System.out.println("[OK] " + texto);
    }

    public void mostrarErro(String texto) {
        System.out.println("[ERRO] " + texto);
    }

    public void mostrarAviso(String texto) {
        System.out.println("[AVISO] " + texto);
    }

    public void mostrarNotificacao(String texto) {
        System.out.println("[NOTIFICAÇÃO] " + texto);
    }

    public void mostrarAlerta(String texto) {
        System.out.println("[ALERTA DE ESTOQUE] " + texto);
    }

    public String lerTexto(String pergunta) {
        System.out.print(pergunta);
        if (!teclado.hasNextLine()) {
            throw new FimDaEntradaException();
        }
        return teclado.nextLine().trim();
    }

    public String lerTextoObrigatorio(String pergunta) {
        while (true) {
            String texto = lerTexto(pergunta);
            if (!texto.isEmpty()) {
                return texto;
            }
            mostrarErro("Este campo é obrigatório.");
        }
    }

    public int lerInteiro(String pergunta) {
        while (true) {
            String texto = lerTexto(pergunta);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                mostrarErro("Digite um número inteiro (ex.: 3).");
            }
        }
    }

    public long lerValorEmCentavos(String pergunta) {
        while (true) {
            String texto = lerTexto(pergunta);
            try {
                double valor = Double.parseDouble(texto.replace(',', '.'));
                return Math.round(valor * 100);
            } catch (NumberFormatException e) {
                mostrarErro("Valor inválido. Digite no formato 12,50.");
            }
        }
    }

    public void mostrarUsuariosDisponiveis(List<Usuario> usuarios) {
        System.out.println();
        System.out.print("Usuários de teste:");
        for (Usuario usuario : usuarios) {
            System.out.print("  " + usuario.getLogin() + " (" + usuario.getPerfil() + ")");
        }
        System.out.println();
    }

    public void mostrarMenu(Usuario usuario) {
        System.out.println();
        System.out.println(LINHA);
        System.out.println("  Usuário: " + usuario.getNome() + " (" + usuario.getLogin() + ") - Perfil: "
                + usuario.getPerfil());
        System.out.println(LINHA);
        System.out.println("  PEDIDOS");
        System.out.println("   1 - Registrar novo pedido");
        System.out.println("   2 - Incluir itens em um pedido ABERTO");
        System.out.println("   3 - Confirmar pedido (reserva o estoque)");
        System.out.println("   4 - Finalizar pedido / entregar (baixa o estoque)");
        System.out.println("   5 - Cancelar pedido");
        System.out.println("   6 - Listar pedidos");
        System.out.println("   7 - Ver detalhes de um pedido");
        System.out.println("  PRODUTOS E ESTOQUE");
        System.out.println("   8 - Listar produtos");
        System.out.println("   9 - Cadastrar produto            [GERENTE]");
        System.out.println("  10 - Alterar preço                [GERENTE]");
        System.out.println("  11 - Registrar entrada de estoque [GERENTE]");
        System.out.println("  12 - Registrar saída / perda      [GERENTE]");
        System.out.println("  13 - Produtos para repor          [GERENTE]");
        System.out.println("   0 - Sair (trocar de usuário)");
    }

    public void mostrarPoliticas(List<PoliticaDesconto> politicas) {
        System.out.println("Políticas de desconto disponíveis:");
        for (PoliticaDesconto politica : politicas) {
            System.out.println("   " + politica.getCodigo() + " - " + politica.getDescricao());
        }
    }

    public void mostrarProdutos(List<Produto> produtos) {
        System.out.println(String.format("%-6s %-28s %11s %7s %8s %7s %6s",
                "Código", "Produto", "Preço", "Físico", "Reserv.", "Disp.", "Mín."));
        for (Produto p : produtos) {
            String marca = "";
            if (p.atingiuEstoqueMinimo()) {
                marca = "  <- repor";
            }
            System.out.println(String.format("%-6s %-28s %11s %7d %8d %7d %6d%s",
                    p.getCodigo(), p.getNome(), p.getPreco(),
                    p.getEstoqueFisico().getValor(), p.getEstoqueReservado().getValor(),
                    p.getEstoqueDisponivel().getValor(), p.getEstoqueMinimo().getValor(), marca));
        }
    }

    public void mostrarPedidos(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            System.out.println("Nenhum pedido registrado.");
            return;
        }
        System.out.println(String.format("%-5s %-22s %-11s %-7s %6s %12s",
                "Nº", "Cliente", "Status", "Descto.", "Itens", "Total"));
        for (Pedido p : pedidos) {
            System.out.println(String.format("%-5d %-22s %-11s %-7s %6d %12s",
                    p.getNumero(), p.getCliente(), p.getStatus(), p.getPoliticaDesconto().getCodigo(),
                    p.getItens().size(), p.getTotal()));
        }
    }

    public void mostrarPedido(Pedido pedido, Cobranca cobranca) {
        System.out.println();
        System.out.println("Pedido #" + pedido.getNumero() + " | Cliente: " + pedido.getCliente()
                + " | Atendente: " + pedido.getLoginAtendente() + " | Status: " + pedido.getStatus());
        if (pedido.getItens().isEmpty()) {
            System.out.println("   (sem itens)");
        }
        for (ItemPedido item : pedido.getItens()) {
            System.out.println(String.format("   %-6s %-28s %3d x %11s = %12s",
                    item.getCodigoProduto(), item.getNomeProduto(), item.getQuantidade().getValor(),
                    item.getPrecoUnitario(), item.getSubtotal()));
        }
        System.out.println("   Subtotal: " + pedido.getSubtotal());
        System.out.println("   Desconto (" + pedido.getPoliticaDesconto().getDescricao() + "): - " + pedido.getDesconto());
        System.out.println("   Total:    " + pedido.getTotal());
        if (cobranca == null) {
            System.out.println("   Cobrança: ainda não gerada (é criada ao confirmar o pedido)");
        } else {
            System.out.println("   Cobrança #" + cobranca.getNumero() + ": " + cobranca.getValor()
                    + " (" + cobranca.getStatus() + ")");
        }
    }
}
