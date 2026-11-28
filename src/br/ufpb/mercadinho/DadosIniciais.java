package br.ufpb.mercadinho;

import br.ufpb.mercadinho.dominio.*;
import br.ufpb.mercadinho.dominio.usuario.*;
import br.ufpb.mercadinho.dominio.valor.*;
import br.ufpb.mercadinho.dominio.desconto.*;
import br.ufpb.mercadinho.persistencia.*;
import br.ufpb.mercadinho.repositorio.*;
import br.ufpb.mercadinho.servico.*;
import br.ufpb.mercadinho.servico.observador.CentralDeNotificacoes;
import br.ufpb.mercadinho.visao.AvisosNoConsole;
import br.ufpb.mercadinho.visao.ConsoleView;

public final class DadosIniciais {
    private DadosIniciais() { }

    public static Ambiente inicializar(String caminhoArquivo) {
        BancoDeDados banco = BancoDeDados.getInstancia();
        banco.conectar(caminhoArquivo);

        CatalogoDescontos catalogo = new CatalogoDescontos();
        catalogo.registrar(new SemDesconto());
        catalogo.registrar(new DescontoPercentual("PROMO10", 10));
        catalogo.registrar(new DescontoValorFixo("FIXO500", new Dinheiro(500)));

        UsuarioRepositorio usuarioRepo = new UsuarioRepositorioArquivo(banco);
        ProdutoRepositorio produtoRepo = new ProdutoRepositorioArquivo(banco);
        PedidoRepositorio pedidoRepo = new PedidoRepositorioArquivo(banco, catalogo);
        CobrancaRepositorio cobrancaRepo = new CobrancaRepositorioArquivo(banco);

        cadastrarDadosPadrao(banco, usuarioRepo, produtoRepo);

        CentralDeNotificacoes central = new CentralDeNotificacoes();
        ConsoleView console = new ConsoleView();
        AvisosNoConsole avisos = new AvisosNoConsole(console);
        central.inscreverObservadorDePedido(avisos);
        central.inscreverObservadorDeEstoque(avisos);

        Autorizacao autorizacao = new Autorizacao();
        ProdutoService produtoService = new ProdutoService(produtoRepo, banco, autorizacao, central);
        PedidoService pedidoService = new PedidoService(pedidoRepo, produtoRepo, cobrancaRepo,
                banco, autorizacao, central);
        MercadinhoFacade facade = new MercadinhoFacade(produtoService, pedidoService);

        return new Ambiente(banco, usuarioRepo, produtoRepo, pedidoRepo, cobrancaRepo,
                catalogo, central, console, facade);
    }

    private static void cadastrarDadosPadrao(BancoDeDados banco, UsuarioRepositorio usuarios,
                                               ProdutoRepositorio produtos) {
        boolean criarUsuario = usuarios.listarTodos().isEmpty();
        boolean criarProduto = produtos.listarTodos().isEmpty();
        if (!criarUsuario && !criarProduto) return;
        banco.iniciar();
        try {
            if (criarUsuario) {
                usuarios.salvar(new Usuario("gerente", "Gerente do Mercadinho", Perfil.GERENTE));
                usuarios.salvar(new Usuario("atendente", "Atendente", Perfil.ATENDENTE));
            }
            if (criarProduto) {
                produtos.salvar(new Produto("P001", "Arroz 1kg", new Dinheiro(650),
                        new Quantidade(30), new Quantidade(5)));
                produtos.salvar(new Produto("P002", "Feijão 1kg", new Dinheiro(800),
                        new Quantidade(25), new Quantidade(5)));
                produtos.salvar(new Produto("P003", "Açúcar 1kg", new Dinheiro(450),
                        new Quantidade(20), new Quantidade(4)));
            }
            banco.efetivar();
        } catch (RuntimeException e) {
            banco.desfazer();
            throw e;
        }
    }

    public static class Ambiente {
        private final BancoDeDados banco;
        private final UsuarioRepositorio usuarios;
        private final ProdutoRepositorio produtos;
        private final PedidoRepositorio pedidos;
        private final CobrancaRepositorio cobrancas;
        private final CatalogoDescontos descontos;
        private final CentralDeNotificacoes notificacoes;
        private final ConsoleView console;
        private final MercadinhoFacade facade;

        private Ambiente(BancoDeDados banco, UsuarioRepositorio usuarios, ProdutoRepositorio produtos,
                         PedidoRepositorio pedidos, CobrancaRepositorio cobrancas, CatalogoDescontos descontos,
                         CentralDeNotificacoes notificacoes, ConsoleView console, MercadinhoFacade facade) {
            this.banco = banco; this.usuarios = usuarios; this.produtos = produtos; this.pedidos = pedidos;
            this.cobrancas = cobrancas; this.descontos = descontos; this.notificacoes = notificacoes;
            this.console = console; this.facade = facade;
        }
        public BancoDeDados getBanco() {
            return banco; }
        public UsuarioRepositorio getUsuarios() {
            return usuarios; }
        public ProdutoRepositorio getProdutos() {
            return produtos; }
        public PedidoRepositorio getPedidos() {
            return pedidos; }
        public CobrancaRepositorio getCobrancas() {
            return cobrancas; }
        public CatalogoDescontos getDescontos() {
            return descontos; }
        public CentralDeNotificacoes getNotificacoes() {
            return notificacoes; }
        public ConsoleView getConsole() {
            return console; }
        public MercadinhoFacade getFacade() {
            return facade; }
    }
}
