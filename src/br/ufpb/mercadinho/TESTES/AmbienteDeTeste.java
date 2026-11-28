package br.ufpb.mercadinho.TESTES;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.dominio.desconto.CatalogoDescontos;
import br.ufpb.mercadinho.dominio.desconto.DescontoPercentual;
import br.ufpb.mercadinho.dominio.desconto.DescontoValorFixo;
import br.ufpb.mercadinho.dominio.desconto.SemDesconto;
import br.ufpb.mercadinho.dominio.financeiro.Cobranca;
import br.ufpb.mercadinho.dominio.pedido.Pedido;
import br.ufpb.mercadinho.dominio.usuario.Perfil;
import br.ufpb.mercadinho.dominio.usuario.Usuario;
import br.ufpb.mercadinho.dominio.valor.Dinheiro;
import br.ufpb.mercadinho.dominio.valor.Quantidade;
import br.ufpb.mercadinho.persistencia.BancoDeDados;
import br.ufpb.mercadinho.persistencia.CobrancaRepositorioArquivo;
import br.ufpb.mercadinho.persistencia.PedidoRepositorioArquivo;
import br.ufpb.mercadinho.persistencia.ProdutoRepositorioArquivo;
import br.ufpb.mercadinho.persistencia.UsuarioRepositorioArquivo;
import br.ufpb.mercadinho.repositorio.CobrancaRepositorio;
import br.ufpb.mercadinho.repositorio.PedidoRepositorio;
import br.ufpb.mercadinho.repositorio.ProdutoRepositorio;
import br.ufpb.mercadinho.repositorio.UsuarioRepositorio;
import br.ufpb.mercadinho.servico.MercadinhoFacade;
import br.ufpb.mercadinho.servico.PedidoService;
import br.ufpb.mercadinho.servico.ProdutoService;
import br.ufpb.mercadinho.servico.observador.CentralDeNotificacoes;


public class AmbienteDeTeste {

    public final Path pasta;
    public final Path arquivo;
    public final BancoDeDados banco;
    public final CatalogoDescontos catalogo;
    public final UsuarioRepositorio usuarios;
    public final ProdutoRepositorio produtos;
    public final PedidoRepositorio pedidos;
    public final CobrancaRepositorio cobrancas;
    public final CentralDeNotificacoes notificacoes;
    public final ObservadorDeTeste observador;
    public final ProdutoService produtoService;
    public final PedidoService pedidoService;
    public final MercadinhoFacade facade;
    public final Usuario atendente;
    public final Usuario gerente;

    public static AmbienteDeTeste novo() throws IOException {
        return new AmbienteDeTeste(Files.createTempDirectory("mercadinho-teste"));
    }

    private AmbienteDeTeste(Path pasta) {
        this.pasta = pasta;
        this.arquivo = pasta.resolve("dados-teste.txt");

        banco = BancoDeDados.getInstancia();
        banco.conectar(arquivo.toString());

        catalogo = new CatalogoDescontos();
        catalogo.registrar(new SemDesconto());
        catalogo.registrar(new DescontoPercentual("PERC10", 10));
        catalogo.registrar(new DescontoValorFixo("FIXO5", new Dinheiro(500)));

        usuarios = new UsuarioRepositorioArquivo(banco);
        produtos = new ProdutoRepositorioArquivo(banco);
        pedidos = new PedidoRepositorioArquivo(banco, catalogo);
        cobrancas = new CobrancaRepositorioArquivo(banco);

        notificacoes = new CentralDeNotificacoes();
        observador = new ObservadorDeTeste();
        notificacoes.inscreverObservadorDePedido(observador);
        notificacoes.inscreverObservadorDeEstoque(observador);

        produtoService = new ProdutoService(produtos, banco, notificacoes);
        pedidoService = new PedidoService(pedidos, produtos, cobrancas, banco, catalogo, notificacoes);
        facade = new MercadinhoFacade(usuarios, produtoService, pedidoService, catalogo);

        if (usuarios.listarTodos().isEmpty()) {
            gravarDadosDeTeste();
        }
        atendente = usuarios.buscarPorLogin("ana");
        gerente = usuarios.buscarPorLogin("carlos");
    }


    public AmbienteDeTeste reiniciar() {
        return new AmbienteDeTeste(pasta);
    }

    public void apagarArquivos() {
        File[] conteudo = pasta.toFile().listFiles();
        if (conteudo != null) {
            for (File arquivoOuPasta : conteudo) {
                arquivoOuPasta.delete();
            }
        }
        pasta.toFile().delete();
    }


    // Atalhos para deixar os testes curtos

    public Pedido criarPedido(String politica, String codigo, int quantidade) {
        Pedido pedido = pedidoService.registrarPedido(atendente, "Maria", politica);
        return pedidoService.adicionarItem(atendente, pedido.getNumero(), codigo, quantidade);
    }

    /** Pedido ABERTO da cliente "Maria" com dois itens. */
    public Pedido criarPedido(String politica, String codigo1, int quantidade1, String codigo2, int quantidade2) {
        Pedido pedido = criarPedido(politica, codigo1, quantidade1);
        return pedidoService.adicionarItem(atendente, pedido.getNumero(), codigo2, quantidade2);
    }

    public Produto produto(String codigo) {
        return produtos.buscarPorCodigo(codigo);
    }

    public int fisico(String codigo) {
        return produto(codigo).getEstoqueFisico().getValor();
    }

    public int reservado(String codigo) {
        return produto(codigo).getEstoqueReservado().getValor();
    }

    public int disponivel(String codigo) {
        return produto(codigo).getEstoqueDisponivel().getValor();
    }

    public int cobrancasDoPedido(int numeroPedido) {
        int quantidade = 0;
        for (Cobranca cobranca : cobrancas.listarTodas()) {
            if (cobranca.getNumeroPedido() == numeroPedido) {
                quantidade++;
            }
        }
        return quantidade;
    }

    private void gravarDadosDeTeste() {
        banco.iniciar();
        usuarios.salvar(new Usuario("ana", "Ana Atendente", Perfil.ATENDENTE));
        usuarios.salvar(new Usuario("carlos", "Carlos Gerente", Perfil.GERENTE));
        produtos.salvar(new Produto("A01", "Arroz 5kg", new Dinheiro(2500), new Quantidade(10), new Quantidade(2)));
        produtos.salvar(new Produto("F01", "Feijão 1kg", new Dinheiro(800), new Quantidade(5), new Quantidade(1)));
        produtos.salvar(new Produto("C01", "Café 250g", new Dinheiro(1000), new Quantidade(6), new Quantidade(5)));
        banco.efetivar();
    }
}
