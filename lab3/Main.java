import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import disassembler.RISCV32Disassembler;
import disassembler.exceptions.*;

public class Main {
    public static void main(String[] args) {
        final String inputFileName;
        final String outputFileName;
        try {
            inputFileName = args[1];
            outputFileName = args[2];
        } catch (IndexOutOfBoundsException e) {
            System.out.println("You entered incorrect number of arguments in command line");
            return;
        }

        try (
            final var input = new FileInputStream(new File(inputFileName));
            final var output = new PrintStream(new File(outputFileName))
        ) {
            new RISCV32Disassembler().parse(input, output);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (FileHeaderException e) {
            System.out.println("FileHeaderException while trying to parse ELF file '" + inputFileName + "': " + e.getMessage());
            System.out.println("Byte position: " + e.getPos());
        } catch (ELFException e) {
            System.out.println("ELFException while trying to parse ELF file '" + inputFileName + "': " + e.getMessage());
            System.out.println("Byte position: " + e.getPos());
        } catch (IndexOutOfBoundsException e) {
            System.out.println("We faced with incorrect information concerned with sizes of sections or size of file entries.");
        }
    }
}