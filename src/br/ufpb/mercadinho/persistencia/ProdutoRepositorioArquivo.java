package br.ufpb.mercadinho.persistencia;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.mercadinho.dominio.Dinheiro;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.Quantidade;
import br.ufpb.mercadinho.excecao.PersistenciaException;


public class ProdutoRepositorioArquivo implements ProdutoRepositorio {

    private final BancoDeDados banco;   // a conexão única (Singleton), recebida pelo construtor

    public ProdutoRepositorioArquivo(BancoDeDados banco) {
        this.banco = banco;
    }

    @Override
    public void salvar(Produto produto) {
        banco.salvarLinha(BancoDeDados.TABELA_PRODUTOS, paraLinha(produto));
    }

    @Override
    public Produto buscarPorCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        String linha = banco.buscarLinha(BancoDeDados.TABELA_PRODUTOS, codigo.trim().toUpperCase());
        if (linha == null) {
            return null;
        }
        return deLinha(linha);
    }

    @Override
    public List<Produto> listarTodos() {
        List<Produto> produtos = new ArrayList<>();
        for (String linha : banco.lerTabela(BancoDeDados.TABELA_PRODUTOS)) {
            produtos.add(deLinha(linha));
        }
        return produtos;
    }

    // ---------------- conversão objeto <-> linha ----------------

    private String paraLinha(Produto produto) {
        return produto.getCodigo()
                + ";" + BancoDeDados.limparTexto(produto.getNome())
                + ";" + produto.getPreco().getCentavos()
                + ";" + produto.getEstoqueFisico().getValor()
                + ";" + produto.getEstoqueReservado().getValor()
                + ";" + produto.getEstoqueMinimo().getValor();
    }

    /** Cada leitura cria um objeto NOVO: mexer nele não altera o arquivo até chamar salvar(). */
    private Produto deLinha(String linha) {
        String[] campos = BancoDeDados.separarCampos(linha, 6, BancoDeDados.TABELA_PRODUTOS);
        try {
            return new Produto(
                    campos[0],
                    campos[1],
                    new Dinheiro(Long.parseLong(campos[2])),
                    new Quantidade(Integer.parseInt(campos[3])),
                    new Quantidade(Integer.parseInt(campos[4])),
                    new Quantidade(Integer.parseInt(campos[5])));
        } catch (RuntimeException e) {
            // Número mal escrito, valor negativo etc.: o arquivo foi alterado ou está corrompido.
            throw new PersistenciaException("Produto inválido no arquivo de dados: \"" + linha + "\"", e);
        }
    }
}
