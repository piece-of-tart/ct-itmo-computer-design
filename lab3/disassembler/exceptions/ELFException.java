package disassembler.exceptions;

public class ELFException extends ParserException {
    public ELFException(final String message, final int pos) {
        super(message, pos);
    }
}
