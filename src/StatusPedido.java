public enum StatusPedido {
    ABERTO,
    CONFIRMADO,
    FINALIZADO,
    CANCELADO;


    public boolean podeMudarPara(StatusPedido destino) {
        switch (this) {
            case ABERTO:
                return destino == CONFIRMADO || destino == CANCELADO;

            case CONFIRMADO:
                return destino == FINALIZADO || destino == CANCELADO;
            default:
                return false;
        }
    }
}
