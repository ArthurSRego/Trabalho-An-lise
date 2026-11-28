package br.ufpb.mercadinho;

import br.ufpb.mercadinho.dominio.valor.*;
import br.ufpb.mercadinho.dominio.usuario.*;
import br.ufpb.mercadinho.dominio.Produto;
import br.ufpb.mercadinho.repositorio.ProdutoRepositorio;
import br.ufpb.mercadinho.repositorio.Transacao;
import br.ufpb.mercadinho.repositorio.UsuarioRepositorio;

public class DadosIniciais {

    private DadosIniciais() {
    }

    public static void carregarSeNecessario(Transacao transacao, UsuarioRepositorio usuarios,
                                            ProdutoRepositorio produtos) {
        if (!usuarios.listarTodos().isEmpty()) {
            return;   
        }

        transacao.iniciar();
        try {
            usuarios.salvar(new Usuario("ana", "Ana Souza", Perfil.ATENDENTE));
            usuarios.salvar(new Usuario("carlos", "Carlos Lima", Perfil.GERENTE));

            
            produtos.salvar(produto("P001", "Arroz Tipo 1 5kg",            2890,          30,    5));
            produtos.salvar(produto("P002", "Feijão Carioca 1kg",           849,          40,    8));
            produtos.salvar(produto("P003", "Açúcar Cristal 1kg",           499,          25,    5));
            produtos.salvar(produto("P004", "Café Torrado 250g",           1290,           4,    2));
            produtos.salvar(produto("P005", "Leite Integral 1L",            549,          48,   12));
            produtos.salvar(produto("P006", "Óleo de Soja 900ml",           799,          24,    6));
            produtos.salvar(produto("P007", "Macarrão Espaguete 500g",      429,          35,    7));
            produtos.salvar(produto("P008", "Sabão em Pó 1kg",             1390,           6,    5));

            transacao.efetivar();
        } catch (RuntimeException erro) {
            transacao.desfazer();
            throw erro;
        }
    }

    private static Produto produto(String codigo, String nome, long precoEmCentavos, int estoque, int minimo) {
        return new Produto(codigo, nome, new Dinheiro(precoEmCentavos), new Quantidade(estoque), new Quantidade(minimo));
    }
}

