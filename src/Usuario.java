public class Usuario {
    private String loginUsuario;
    private String nomeUsuario;
    private Perfil perfil;

    public Usuario(String loginUsuario, String nomeUsuario, Perfil perfil){

        this.loginUsuario = loginUsuario;
        this.nomeUsuario = nomeUsuario;
        this.perfil = perfil;
    }
    public String getLoginUsuario() {
        return loginUsuario;
    }
    public String getNomeUsuario() {
        return nomeUsuario;
    }
    public Perfil getPerfil() {
        return perfil;
    }
    public boolean ehGerente() {
        return perfil == Perfil.GERENTE;
    }
}
