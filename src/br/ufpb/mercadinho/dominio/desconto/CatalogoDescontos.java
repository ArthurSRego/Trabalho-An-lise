package br.ufpb.mercadinho.dominio.desconto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.ufpb.mercadinho.excecao.RegraDeNegocioException;


public class CatalogoDescontos {

    // LinkedHashMap: um mapa (código -> política) que mantém a ordem de inserção,
    // assim o menu mostra as opções sempre na mesma ordem.
    private final Map<String, PoliticaDesconto> politicas = new LinkedHashMap<>();

    /** Adiciona uma política. Códigos repetidos não são permitidos. */
    public void registrar(PoliticaDesconto politica) {
        String codigo = politica.getCodigo().toUpperCase();
        if (politicas.containsKey(codigo)) {
            throw new RegraDeNegocioException("Já existe uma política de desconto com o código " + codigo + ".");
        }
        politicas.put(codigo, politica);
    }

    /** Busca pelo código (maiúsculas ou minúsculas). Lança exceção se não existir. */
    public PoliticaDesconto buscar(String codigo) {
        PoliticaDesconto politica = null;
        if (codigo != null) {
            politica = politicas.get(codigo.trim().toUpperCase());
        }
        if (politica == null) {
            throw new RegraDeNegocioException("Política de desconto \"" + codigo + "\" não existe. Opções: "
                    + politicas.keySet() + ".");
        }
        return politica;
    }

    public List<PoliticaDesconto> listar() {
        return new ArrayList<>(politicas.values());
    }
}
