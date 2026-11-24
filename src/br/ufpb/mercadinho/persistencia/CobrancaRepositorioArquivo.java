package br.ufpb.mercadinho.persistencia;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.mercadinho.dominio.Cobranca;
import br.ufpb.mercadinho.dominio.Dinheiro;
import br.ufpb.mercadinho.dominio.StatusCobranca;
import br.ufpb.mercadinho.excecao.PersistenciaException;


public class CobrancaRepositorioArquivo {

    private final BancoDeDados banco;

    public CobrancaRepositorioArquivo(BancoDeDados banco) {
        this.banco = banco;
    }

    @Override
    public int proximoNumero() {
        int maior = 0;
        for (Cobranca cobranca : listarTodas()) {
            if (cobranca.getNumero() > maior) {
                maior = cobranca.getNumero();
            }
        }
        return maior + 1;
    }

    @Override
    public void salvar(Cobranca cobranca) {
        banco.salvarLinha(BancoDeDados.TABELA_COBRANCAS, paraLinha(cobranca));
    }

    @Override
    public Cobranca buscarPorPedido(int numeroPedido) {
        for (Cobranca cobranca : listarTodas()) {
            if (cobranca.getNumeroPedido() == numeroPedido) {
                return cobranca;
            }
        }
        return null;
    }

    @Override
    public List<Cobranca> listarTodas() {
        List<Cobranca> cobrancas = new ArrayList<>();
        for (String linha : banco.lerTabela(BancoDeDados.TABELA_COBRANCAS)) {
            cobrancas.add(deLinha(linha));
        }
        return cobrancas;
    }

    // ---------------- conversão objeto <-> linha ----------------

    private String paraLinha(Cobranca cobranca) {
        return cobranca.getNumero()
                + ";" + cobranca.getNumeroPedido()
                + ";" + cobranca.getValor().getCentavos()
                + ";" + cobranca.getStatus().name();
    }

    private Cobranca deLinha(String linha) {
        String[] campos = BancoDeDados.separarCampos(linha, 4, BancoDeDados.TABELA_COBRANCAS);
        try {
            return new Cobranca(
                    Integer.parseInt(campos[0]),
                    Integer.parseInt(campos[1]),
                    new Dinheiro(Long.parseLong(campos[2])),
                    StatusCobranca.valueOf(campos[3]));
        } catch (RuntimeException e) {
            throw new PersistenciaException("Cobrança inválida no arquivo de dados: \"" + linha + "\"", e);
        }
    }
}
