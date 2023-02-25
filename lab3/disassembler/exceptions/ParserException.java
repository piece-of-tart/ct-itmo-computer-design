package disassembler.exceptions;

public abstract class ParserException extends Exception {
    private final int pos;

    public ParserException(final String message, final int pos) {
        super(message);
        this.pos = pos;
    }

    public int getPos() {
        return pos;
    }
}
