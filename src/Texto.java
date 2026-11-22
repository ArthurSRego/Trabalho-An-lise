 public class Texto {
    // Pequenas funções de apoio para validar textos dos identificadores
    private Texto() {
    }
    public static boolean temApenasLetrasENumeros(String texto) {
        if (texto.isEmpty()) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            char letra = texto.charAt(i);
            if (!Character.isLetterOrDigit(letra)) {
                return false;
            }
        }
        return true;
    }
}
