package br.ufpb.mercadinho.persistencia;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.dominio.pedido.ItemPedido;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import br.ufpb.mercadinho.dominio.pedido.StatusPedido;
import br.ufpb.mercadinho.dominio.desconto.CatalogoDescontos;
import br.ufpb.mercadinho.dominio.desconto.PoliticaDesconto;
import br.ufpb.mercadinho.excecao.PersistenciaException;
import br.ufpb.mercadinho.repositorio.PedidoRepositorio;

public class PedidoRepositorioArquivo implements PedidoRepositorio {

    private final BancoDeDados banco;
    private final CatalogoDescontos catalogoDescontos;

    public PedidoRepositorioArquivo(BancoDeDados banco, CatalogoDescontos catalogoDescontos) {
        this.banco = banco;
        this.catalogoDescontos = catalogoDescontos;
    }

    /** Maior número já usado + 1. Pedidos nunca são apagados, então não há repetição. */
    @Override
    public int proximoNumero() {
        int maior = 0;
        for (String linha : banco.lerTabela(BancoDeDados.TABELA_PEDIDOS)) {
            String[] campos = BancoDeDados.separarCampos(linha, 5, BancoDeDados.TABELA_PEDIDOS);
            int numero = converterNumero(campos[0], linha);
            if (numero > maior) {
                maior = numero;
            }
        }
        return maior + 1;
    }

    @Override
    public void salvar(Pedido pedido) {
        // 1) a linha do pedido (insere ou substitui pelo número)
        banco.salvarLinha(BancoDeDados.TABELA_PEDIDOS, paraLinha(pedido));

        // 2) os itens: tira os itens antigos DESTE pedido e coloca os atuais
        String numero = String.valueOf(pedido.getNumero());
        List<String> itensAtualizados = new ArrayList<>();
        for (String linha : banco.lerTabela(BancoDeDados.TABELA_ITENS_PEDIDO)) {
            String[] campos = BancoDeDados.separarCampos(linha, 5, BancoDeDados.TABELA_ITENS_PEDIDO);
            if (!campos[0].equals(numero)) {
                itensAtualizados.add(linha);   // item de OUTRO pedido: mantém
            }
        }
        for (ItemPedido item : pedido.getItens()) {
            itensAtualizados.add(itemParaLinha(pedido.getNumero(), item));
        }
        banco.gravarTabela(BancoDeDados.TABELA_ITENS_PEDIDO, itensAtualizados);
        // Obs.: as duas tabelas só vão para o disco juntas, no efetivar() da transação.
    }

    @Override
    public Pedido buscarPorNumero(int numero) {
        String linha = banco.buscarLinha(BancoDeDados.TABELA_PEDIDOS, String.valueOf(numero));
        if (linha == null) {
            return null;
        }
        return deLinha(linha);
    }

    @Override
    public List<Pedido> listarTodos() {
        List<Pedido> pedidos = new ArrayList<>();
        for (String linha : banco.lerTabela(BancoDeDados.TABELA_PEDIDOS)) {
            pedidos.add(deLinha(linha));
        }
        return pedidos;
    }

    // ---------------- conversão objeto <-> linha ----------------

    private String paraLinha(Pedido pedido) {
        return pedido.getNumero()
                + ";" + BancoDeDados.limparTexto(pedido.getCliente())
                + ";" + pedido.getLoginAtendente()
                + ";" + pedido.getStatus().name()
                + ";" + pedido.getPoliticaDesconto().getCodigo();
    }

    private String itemParaLinha(int numeroPedido, ItemPedido item) {
        return numeroPedido
                + ";" + item.getCodigoProduto()
                + ";" + BancoDeDados.limparTexto(item.getNomeProduto())
                + ";" + item.getQuantidade().getValor()
                + ";" + item.getPrecoUnitario().getCentavos();
    }

    private Pedido deLinha(String linha) {
        String[] campos = BancoDeDados.separarCampos(linha, 5, BancoDeDados.TABELA_PEDIDOS);
        int numero = converterNumero(campos[0], linha);
        try {
            StatusPedido status = StatusPedido.valueOf(campos[3]);              // texto -> enum
            PoliticaDesconto politica = catalogoDescontos.buscar(campos[4]);   // código -> Strategy
            return new Pedido(numero, campos[1], campos[2], status, politica, lerItens(numero));
        } catch (PersistenciaException e) {
            throw e;   // já veio com uma mensagem clara (ex.: item com defeito)
        } catch (RuntimeException e) {
            throw new PersistenciaException("Pedido inválido no arquivo de dados: \"" + linha + "\"", e);
        }
    }

    /** Busca na tabela de itens só as linhas deste pedido. */
    private List<ItemPedido> lerItens(int numeroPedido) {
        String numero = String.valueOf(numeroPedido);
        List<ItemPedido> itens = new ArrayList<>();
        for (String linha : banco.lerTabela(BancoDeDados.TABELA_ITENS_PEDIDO)) {
            String[] campos = BancoDeDados.separarCampos(linha, 5, BancoDeDados.TABELA_ITENS_PEDIDO);
            if (campos[0].equals(numero)) {
                try {
                    itens.add(new ItemPedido(
                            campos[1],
                            campos[2],
                            new Dinheiro(Long.parseLong(campos[4])),
                            new Quantidade(Integer.parseInt(campos[3]))));
                } catch (RuntimeException e) {
                    throw new PersistenciaException("Item de pedido inválido no arquivo de dados: \"" + linha + "\"", e);
                }
            }
        }
        return itens;
    }

    private int converterNumero(String texto, String linha) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new PersistenciaException("Número de pedido inválido no arquivo de dados: \"" + linha + "\"", e);
        }
    }
}

