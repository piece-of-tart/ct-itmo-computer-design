package disassembler.exceptions;

public class FileHeaderException extends ParserException {
    public FileHeaderException(final String message, final int pos) {
        super(message, pos);
    }
}
