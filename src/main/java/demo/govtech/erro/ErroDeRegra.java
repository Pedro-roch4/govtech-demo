package demo.govtech.erro;

/** Erro previsto de regra de negocio. O tipo define o status HTTP no ApiExceptionHandler. */
public class ErroDeRegra extends RuntimeException {

    public enum Tipo { NAO_ENCONTRADO, CONFLITO, REGRA_DE_NEGOCIO }

    private final Tipo tipo;

    private ErroDeRegra(Tipo tipo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
    }

    public Tipo tipo() {
        return tipo;
    }

    public static ErroDeRegra regra(String mensagem) {
        return new ErroDeRegra(Tipo.REGRA_DE_NEGOCIO, mensagem);
    }

    public static ErroDeRegra conflito(String mensagem) {
        return new ErroDeRegra(Tipo.CONFLITO, mensagem);
    }

    public static ErroDeRegra naoEncontrado(String mensagem) {
        return new ErroDeRegra(Tipo.NAO_ENCONTRADO, mensagem);
    }
}
