import java.util.ArrayList;
import java.util.List;

public class Carrinho {
    private int idCarrinho;
    private List<Item> itens;
    public void adicionarItem(Item item){
        itens.add(item);
    }

    //reimplementar utilizando IDs
    public void removerItem(Item item){
        for (int k = 0;k<itens.size();k++){
            if(itens.get(k) == item){
                itens.remove(k);
            }
        }
    }

    //implementar
    public String listarCarrinho(){}
}
