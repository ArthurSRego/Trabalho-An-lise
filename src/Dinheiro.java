public class Dinheiro {

    public static final Dinheiro ZERO = new Dinheiro(0);

    private final long centavos;

    public Dinheiro(long centavos) {
        if (centavos < 0) {
            throw new RegraDeNegocioException("Valor em dinheiro não pode ser negativo (recebido: "
                    + centavos + " centavos).");
        }
        this.centavos = centavos;
    }

    public long getCentavos() {
        return centavos;
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(this.centavos + outro.centavos);
    }

    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(this.centavos - outro.centavos);
    }

    public Dinheiro multiplicar(int vezes) {
        return new Dinheiro(this.centavos * vezes);
    }

    public Dinheiro percentual(int percentual) {
        return new Dinheiro((this.centavos * percentual + 50) / 100);
    }

    public boolean ehMaiorQue(Dinheiro outro) {
        return this.centavos > outro.centavos;
    }

    public boolean ehZero() {
        return centavos == 0;
    }

    @Override
    public boolean equals(Object outroObjeto) {
        if (this == outroObjeto) {
            return true;
        }
        if (!(outroObjeto instanceof Dinheiro)) {
            return false;
        }
        Dinheiro outro = (Dinheiro) outroObjeto;
        return this.centavos == outro.centavos;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(centavos);
    }

    @Override
    public String toString() {
        long reais = centavos / 100;
        long restoEmCentavos = centavos % 100;
        return String.format("R$ %d,%02d", reais, restoEmCentavos);
    }
}
