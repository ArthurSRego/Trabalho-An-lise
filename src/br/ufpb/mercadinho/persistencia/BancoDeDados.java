package br.ufpb.mercadinho.persistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.ufpb.mercadinho.excecao.PersistenciaException;
import br.ufpb.mercadinho.repositorio.Transacao;


public class BancoDeDados implements Transacao {

    // ------------------------------------------------------------------
    // Nomes das tabelas (usados pelos repositórios)
    // ------------------------------------------------------------------
    public static final String TABELA_USUARIOS = "usuarios";
    public static final String TABELA_PRODUTOS = "produtos";
    public static final String TABELA_PEDIDOS = "pedidos";
    public static final String TABELA_ITENS_PEDIDO = "itens_pedido";
    public static final String TABELA_COBRANCAS = "cobrancas";

    /** Ordem das tabelas no arquivo. */
    private static final String[] NOMES_DAS_TABELAS = {
            TABELA_USUARIOS, TABELA_PRODUTOS, TABELA_PEDIDOS, TABELA_ITENS_PEDIDO, TABELA_COBRANCAS
    };

    /** Descrição das colunas de cada tabela (mesma ordem de NOMES_DAS_TABELAS), escrita como comentário. */
    private static final String[] COLUNAS_DAS_TABELAS = {
            "login;nome;perfil",
            "codigo;nome;preco_centavos;estoque_fisico;estoque_reservado;estoque_minimo",
            "numero;cliente;login_atendente;status;codigo_politica_desconto",
            "numero_pedido;codigo_produto;nome_produto;quantidade;preco_unitario_centavos",
            "numero;numero_pedido;valor_centavos;status"
    };

    // ------------------------------------------------------------------
    // SINGLETON
    // ------------------------------------------------------------------

    /** A única instância (começa null e é criada no primeiro getInstancia()). */
    private static BancoDeDados instancia;

    /** Construtor PRIVADO: impede "new BancoDeDados()" fora desta classe. */
    private BancoDeDados() {
    }

    /** Ponto de acesso global: devolve sempre o MESMO objeto. */
    public static BancoDeDados getInstancia() {
        if (instancia == null) {
            instancia = new BancoDeDados();
        }
        return instancia;
    }

    // ------------------------------------------------------------------
    // Estado da conexão
    // ------------------------------------------------------------------

    private Path arquivo;                                // arquivo oficial de dados
    private Map<String, List<String>> tabelas;           // nome da tabela -> linhas
    private Map<String, List<String>> copiaDeSeguranca;  // usada pelo desfazer()
    private boolean emTransacao;

    /**
     * Abre (ou cria) o arquivo de dados e carrega tudo para a memória.
     *
     * Chamar de novo com o mesmo caminho equivale a "fechar e abrir o programa":
     * o que estava na memória é descartado e tudo é lido do disco outra vez.
     */
    public void conectar(String caminhoDoArquivo) {
        this.arquivo = Paths.get(caminhoDoArquivo);
        this.tabelas = criarTabelasVazias();
        this.copiaDeSeguranca = null;
        this.emTransacao = false;

        // Sobrou um .tmp de uma gravação interrompida? Ele pode estar pela metade,
        // então é descartado. O arquivo oficial tem a última versão COMPLETA.
        Path temporario = caminhoDoTemporario();
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível apagar o arquivo temporário "
                    + temporario.toAbsolutePath() + ".", e);
        }

        if (Files.exists(arquivo)) {
            carregarDoDisco();
        }
        // Se o arquivo ainda não existe, começamos com as tabelas vazias.
        // Ele será criado na primeira vez que uma transação for efetivada.
    }

    /** Caminho do arquivo oficial (útil para mensagens na tela). */
    public Path getArquivo() {
        return arquivo;
    }

    // ------------------------------------------------------------------
    // Operações usadas pelos repositórios
    // ------------------------------------------------------------------

    /** Devolve uma CÓPIA das linhas da tabela (quem chamou pode alterar a lista à vontade). */
    public List<String> lerTabela(String nomeTabela) {
        return new ArrayList<>(tabela(nomeTabela));
    }

    /**
     * Procura a linha cuja CHAVE (primeiro campo) é igual a "chave".
     *
     * @return a linha encontrada, ou null se não existir
     */
    public String buscarLinha(String nomeTabela, String chave) {
        for (String linha : tabela(nomeTabela)) {
            if (primeiroCampo(linha).equals(chave)) {
                return linha;
            }
        }
        return null;
    }

    /**
     * Insere a linha ou, se já existe uma linha com a mesma chave (primeiro campo),
     * substitui essa linha. Só funciona dentro de uma transação.
     */
    public void salvarLinha(String nomeTabela, String linha) {
        exigirTransacaoAtiva();
        List<String> linhas = tabela(nomeTabela);
        String chave = primeiroCampo(linha);
        for (int i = 0; i < linhas.size(); i++) {
            if (primeiroCampo(linhas.get(i)).equals(chave)) {
                linhas.set(i, linha);   // já existia: substitui
                return;
            }
        }
        linhas.add(linha);              // não existia: insere no final
    }

    /** Troca TODAS as linhas de uma tabela. Só funciona dentro de uma transação. */
    public void gravarTabela(String nomeTabela, List<String> novasLinhas) {
        exigirTransacaoAtiva();
        tabela(nomeTabela);   // só para conferir se a tabela existe
        tabelas.put(nomeTabela, new ArrayList<>(novasLinhas));
    }

    // ------------------------------------------------------------------
    // Transação (implementa o contrato repositorio.Transacao)
    // ------------------------------------------------------------------

    @Override
    public void iniciar() {
        exigirConexao();
        if (emTransacao) {
            throw new IllegalStateException("Já existe uma transação em andamento. "
                    + "Efetive ou desfaça a transação atual antes de iniciar outra.");
        }
        copiaDeSeguranca = copiar(tabelas);
        emTransacao = true;
    }

    @Override
    public void efetivar() {
        exigirTransacaoAtiva();
        try {
            gravarNoDisco();
        } catch (PersistenciaException e) {
            // Não conseguiu gravar: a memória volta a ficar igual ao que está no disco.
            desfazer();
            throw e;
        }
        copiaDeSeguranca = null;
        emTransacao = false;
    }

    @Override
    public void desfazer() {
        if (!emTransacao) {
            return;   // nada a desfazer (ex.: a falha aconteceu antes de iniciar a transação)
        }
        tabelas = copiaDeSeguranca;
        copiaDeSeguranca = null;
        emTransacao = false;
    }

    // ------------------------------------------------------------------
    // Funções de apoio para os repositórios montarem/lerem linhas
    // ------------------------------------------------------------------

    /**
     * Separa uma linha nos seus campos e confere a quantidade de campos.
     * Se o arquivo estiver estragado, a mensagem diz qual tabela e qual linha.
     */
    public static String[] separarCampos(String linha, int quantidadeEsperada, String nomeTabela) {
        String[] campos = linha.split(";");
        if (campos.length != quantidadeEsperada) {
            throw new PersistenciaException("Linha inválida na tabela \"" + nomeTabela + "\": esperava "
                    + quantidadeEsperada + " campos e encontrou " + campos.length + ". Linha: \"" + linha + "\"");
        }
        return campos;
    }

    /**
     * Prepara um texto livre (nome de cliente, nome de produto) para ir ao arquivo:
     * troca ';' (o nosso separador) por ',' e tira quebras de linha.
     */
    public static String limparTexto(String texto) {
        return texto.replace(';', ',').replace('\n', ' ').replace('\r', ' ').trim();
    }

    // ------------------------------------------------------------------
    // Partes internas
    // ------------------------------------------------------------------

    private List<String> tabela(String nomeTabela) {
        exigirConexao();
        List<String> linhas = tabelas.get(nomeTabela);
        if (linhas == null) {
            throw new PersistenciaException("Tabela desconhecida: \"" + nomeTabela + "\".");
        }
        return linhas;
    }

    /** O texto antes do primeiro ';' (ou a linha toda, se não houver ';'). */
    private static String primeiroCampo(String linha) {
        int posicao = linha.indexOf(';');
        if (posicao < 0) {
            return linha;
        }
        return linha.substring(0, posicao);
    }

    private Map<String, List<String>> criarTabelasVazias() {
        Map<String, List<String>> novas = new LinkedHashMap<>();
        for (String nome : NOMES_DAS_TABELAS) {
            novas.put(nome, new ArrayList<>());
        }
        return novas;
    }

    /** Cópia das tabelas. Basta copiar as listas: Strings não mudam (são imutáveis). */
    private Map<String, List<String>> copiar(Map<String, List<String>> origem) {
        Map<String, List<String>> copia = new LinkedHashMap<>();
        for (String nome : origem.keySet()) {
            copia.put(nome, new ArrayList<>(origem.get(nome)));
        }
        return copia;
    }

    private void carregarDoDisco() {
        List<String> linhasDoArquivo;
        try {
            linhasDoArquivo = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível ler o arquivo de dados " + arquivo.toAbsolutePath() + ".", e);
        }

        String tabelaAtual = null;
        int numeroDaLinha = 0;
        for (String linhaOriginal : linhasDoArquivo) {
            numeroDaLinha++;
            String linha = linhaOriginal.trim();
            if (linha.startsWith("﻿")) {
                linha = linha.substring(1);   // marca invisível (BOM) que alguns editores colocam no início
            }
            if (linha.isEmpty() || linha.startsWith("#")) {
                continue;   // linha em branco ou comentário
            }
            if (linha.startsWith("[") && linha.endsWith("]")) {
                tabelaAtual = linha.substring(1, linha.length() - 1);
                if (!tabelas.containsKey(tabelaAtual)) {
                    throw new PersistenciaException("Tabela desconhecida \"" + tabelaAtual + "\" na linha "
                            + numeroDaLinha + " de " + arquivo.toAbsolutePath() + ".");
                }
            } else if (tabelaAtual == null) {
                throw new PersistenciaException("A linha " + numeroDaLinha + " de " + arquivo.toAbsolutePath()
                        + " está fora de qualquer tabela: \"" + linha + "\"");
            } else {
                tabelas.get(tabelaAtual).add(linha);
            }
        }
    }

    private void gravarNoDisco() {
        // Monta o conteúdo completo do arquivo na memória.
        List<String> conteudo = new ArrayList<>();
        conteudo.add("# Mercadinho Bom Jesus - arquivo de dados (gerado pelo sistema).");
        conteudo.add("# Feche o programa antes de editar. Para voltar aos dados iniciais, rode com --resetar.");
        for (int i = 0; i < NOMES_DAS_TABELAS.length; i++) {
            String nome = NOMES_DAS_TABELAS[i];
            conteudo.add("");
            conteudo.add("[" + nome + "]");
            conteudo.add("# " + COLUNAS_DAS_TABELAS[i]);
            conteudo.addAll(tabelas.get(nome));
        }

        Path temporario = caminhoDoTemporario();
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            // Passo 1: escreve TUDO no arquivo temporário.
            Files.write(temporario, conteudo, StandardCharsets.UTF_8);
            // Passo 2: troca o arquivo oficial pelo temporário, de uma vez só.
            substituirArquivo(temporario, arquivo);
        } catch (IOException e) {
            throw new PersistenciaException("Falha ao gravar os dados em " + arquivo.toAbsolutePath()
                    + ". Nenhuma alteração desta operação foi salva.", e);
        }
    }

    private void substituirArquivo(Path origem, Path destino) throws IOException {
        try {
            // Renomear é atômico no sistema operacional: o arquivo nunca fica "pela metade".
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Alguns sistemas de arquivos não oferecem troca atômica; nesse caso usamos a troca comum,
            // que continua protegida pelo arquivo temporário completo.
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path caminhoDoTemporario() {
        return Paths.get(arquivo.toString() + ".tmp");
    }

    private void exigirConexao() {
        if (tabelas == null) {
            throw new IllegalStateException("O banco de dados não foi conectado. Chame conectar(caminho) antes.");
        }
    }

    private void exigirTransacaoAtiva() {
        exigirConexao();
        if (!emTransacao) {
            throw new IllegalStateException("Alterações só podem ser feitas dentro de uma transação "
                    + "(chame iniciar() antes).");
        }
    }
}
