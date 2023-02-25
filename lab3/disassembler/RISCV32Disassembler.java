package disassembler;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import disassembler.exceptions.ELFException;
import disassembler.exceptions.FileHeaderException;

public class RISCV32Disassembler {
    private byte[] data;
    private static Map<String, InstructionTypes> instructionType;
    private static List<String> registers;
    private ELFParser elfParser;

    public RISCV32Disassembler() {
        elfParser = new ELFParser();
    }

    static {
        instructionType = new HashMap<>();

        instructionType.put("lui", InstructionTypes.UPPER_IMMEDIATE);
        instructionType.put("auipc", InstructionTypes.UPPER_IMMEDIATE);
        instructionType.put("addi", InstructionTypes.IMMEDIATE);
        instructionType.put("slti", InstructionTypes.IMMEDIATE);
        instructionType.put("sltiu", InstructionTypes.IMMEDIATE);
        instructionType.put("xori", InstructionTypes.IMMEDIATE);
        instructionType.put("ori", InstructionTypes.IMMEDIATE);
        instructionType.put("andi", InstructionTypes.IMMEDIATE);
        instructionType.put("slli", InstructionTypes.I_SHAMT);
        instructionType.put("srli", InstructionTypes.I_SHAMT);
        instructionType.put("srai", InstructionTypes.I_SHAMT);
        instructionType.put("add", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("sub", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("sll", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("slt", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("sltu", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("xor", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("srl", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("or", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("and", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("csrrw", InstructionTypes.IMMEDIATE_CSRR);
        instructionType.put("csrrs", InstructionTypes.IMMEDIATE_CSRR);
        instructionType.put("scrrc", InstructionTypes.IMMEDIATE_CSRR);
        instructionType.put("csrrwi", InstructionTypes.IMMEDIATE_CSRR);
        instructionType.put("csrrsi", InstructionTypes.IMMEDIATE_CSRR);
        instructionType.put("csrrci", InstructionTypes.IMMEDIATE_CSRR);
        instructionType.put("ecall", InstructionTypes.SPECIAL);
        instructionType.put("ebreak", InstructionTypes.SPECIAL);
        instructionType.put("uret", InstructionTypes.SPECIAL);
        instructionType.put("sret", InstructionTypes.SPECIAL);
        instructionType.put("mret", InstructionTypes.SPECIAL);
        instructionType.put("wfi", InstructionTypes.SPECIAL);
        instructionType.put("lb", InstructionTypes.IMMEDIATE_L);
        instructionType.put("lh", InstructionTypes.IMMEDIATE_L);
        instructionType.put("lw", InstructionTypes.IMMEDIATE_L);
        instructionType.put("lbu", InstructionTypes.IMMEDIATE_L);
        instructionType.put("lhu", InstructionTypes.IMMEDIATE_L);
        instructionType.put("sb", InstructionTypes.STORE);
        instructionType.put("sh", InstructionTypes.STORE);
        instructionType.put("sw", InstructionTypes.STORE);
        instructionType.put("jal", InstructionTypes.JUMP);
        instructionType.put("jalr", InstructionTypes.JUMP_WITH_REG);
        instructionType.put("beq", InstructionTypes.BRANCH);
        instructionType.put("bne", InstructionTypes.BRANCH);
        instructionType.put("blt", InstructionTypes.BRANCH);
        instructionType.put("bge", InstructionTypes.BRANCH);
        instructionType.put("bltu", InstructionTypes.BRANCH);
        instructionType.put("bgeu", InstructionTypes.BRANCH);
        instructionType.put("mul", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("mulh", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("mulhsu", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("mulhu", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("div", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("divu", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("rem", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("remu", InstructionTypes.REGISTER_REGISTER);
        instructionType.put("unknown_instruction", InstructionTypes.UNKNOWN_INSTRUCTION);


        registers = new ArrayList<>();
        registers.add(0, "zero");
        registers.add(1, "ra");
        registers.add(2, "sp");
        registers.add(3, "gp");
        registers.add(4, "tp");
        registers.add(5, "t0");
        registers.add(6, "t1");
        registers.add(7, "t2");
        registers.add(8, "s0");
        registers.add(9,  "s1");
        registers.add(10, "a0");
        registers.add(11, "a1");
        registers.add(12, "a2");
        registers.add(13, "a3");
        registers.add(14, "a4");
        registers.add(15, "a5");
        registers.add(16, "a6");
        registers.add(17, "a7");
        registers.add(18, "s2");
        registers.add(19, "s3");
        registers.add(20, "s4");
        registers.add(21, "s5");
        registers.add(22, "s6");
        registers.add(23, "s7");
        registers.add(24, "s8");
        registers.add(25, "s9");
        registers.add(26, "s10");
        registers.add(27, "s11");
        registers.add(28, "t3");
        registers.add(29, "t4");
        registers.add(30, "t5");
        registers.add(31, "t6");
    }

    public void parse(final InputStream input, final PrintStream out) throws FileHeaderException, ELFException, IOException {
        data = input.readAllBytes();
        elfParser.parse(data);

        printTextSegment(out, elfParser.getELFSection(".text"), elfParser.getSymTabMap());
        out.println();
        elfParser.printSymTab(out);

        System.out.println("Success disassembling.");
    }

    private void printTextSegment(final PrintStream out, final ELFSection text, final Map<Integer, SymbolDescription> symTabMap) {
        int idx = text.sh_offset;
        fillSymTabWithLLabels(text, symTabMap);

        out.println(".text");
        for (int i = 0; i < text.sh_size / 4; i++) {
            final var label = symTabMap.get(text.sh_addr + i * 4);
            if (label != null) {
                out.printf("%08x    <%s>:\n", text.sh_addr + i * 4, label.name);
            }
            final int word = readNBytes(idx, 4);

            final String instruction = getInstruction(word);

            switch (instructionType.get(instruction)) {
                case REGISTER_REGISTER: {
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s, %s\n",
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 7) & 0b11111),
                        registers.get((word >> 15) & 0b11111),
                        registers.get((word >> 20) & 0b11111)
                    );
                    break;
                }
                case UPPER_IMMEDIATE: {
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s\n",
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 7) & 0b11111),
                        Integer.toHexString((word >>> 12))
                    );
                    break;
                }
                case IMMEDIATE: {
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s, %s\n",
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 7) & 0b11111),
                        registers.get((word >> 15) & 0b11111),
                        String.valueOf((word >> 20)) // не менять, иначе отрицательные значения не появляются
                    );
                    break;
                }
                case IMMEDIATE_CSRR: {
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s, %s\n",
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 7) & 0b11111),
                        Integer.toString(word >> 20),
                        registers.get((word >> 15) & 0b11111)
                    );
                    break;
                }
                case IMMEDIATE_L: {
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s(%s)\n",
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 7) & 0b11111),
                        Integer.toString(word >> 20),
                        registers.get((word >> 15) & 0b11111)
                    );
                    break;
                }
                case STORE: {
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s(%s)\n",
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 20) & 0b11111),
                        Integer.toString(
                            ((word >> 25) << 5) |
                            ((word >> (11 - 4)) & 0x0000001f)
                        ),
                        registers.get((word >> 15) & 0b11111)
                    );
                    break;
                }
                case BRANCH: {
                    final int jAddr = ((
                        (((word >>> 25) & 0x0000003f) << 5)  |
                        (((word >>> 7) & 0x00000001) << 11)  |
                        (((word >>> 8) & 0x0000000f) << 1)) + text.sh_addr + i * 4 -
                        (((word >>> 31) == 1) ? ((int) Math.pow(2, 12)) : 0)
                    );
                    var symDesc = symTabMap.get(jAddr);
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s, %s <%s>\n",
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 15) & 0b11111),
                        registers.get((word >> 20) & 0b11111),
                        Integer.toHexString(jAddr),
                        symDesc.name
                    );
                    break;
                }
                case JUMP: {
                    final int jAddr = ((
                        (((word >>> 21) & 0x000003ff) << 1)  |
                        (((word >>> 20) & 0x00000001) << 11)  |
                        (((word >>> 12) & 0x000000ff) << 12)) + text.sh_addr + i * 4 -
                        (((word >>> 31) == 1) ? ((int) Math.pow(2, 20)) : 0)
                    );
                    var symDesc = symTabMap.get(jAddr);
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s <%s>\n", 
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 7) & 0b11111),
                        Integer.toHexString(jAddr), 
                        symDesc.name
                    );
                    break;
                }
                case JUMP_WITH_REG: {
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s(%s)\n", 
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 7) & 0b11111),
                        Integer.toHexString(word >> 20),
                        registers.get((word >> 15) & 0b11111)
                    );
                    break;
                }
                case I_SHAMT: {
                    out.printf(
                        "   %05x:\t%08x\t%7s\t%s, %s, %s\n",
                        text.sh_addr + i * 4,
                        word,
                        instruction,
                        registers.get((word >> 7) & 0b11111),
                        registers.get((word >> 15) & 0b11111),
                        String.valueOf((word >>> 20) & 0b11111)
                    );
                    break;
                }
                case SPECIAL, UNKNOWN_INSTRUCTION: {
                    out.printf(
                        "   %05x:\t%08x\t%7s\n", 
                        text.sh_addr + i * 4,
                        word,
                        instruction
                    );
                    break;
                }
                default:
                    throw new IllegalStateException("IllegalStateException: Token for '0x" + Integer.toHexString(word) + "' wasn't created.");
            }

            idx += 4;
        }
    }

    private void fillSymTabWithLLabels(final ELFSection text, final Map<Integer, SymbolDescription> symTabMap) {
        int idx = text.sh_offset;
        int numOfUndefLabels = 0;
        
        for (int i = 0; i < text.sh_size / 4; i++, idx += 4) {
            final int word = readNBytes(idx, 4);
            final String instruction = getInstruction(word);
            final var instr = instructionType.get(instruction);
            if (instr == null) {
                continue;
            }
            switch (instr) {
                case BRANCH: {
                    final int jAddr = ((
                        (((word >>> 25) & 0x0000003f) << 5)  |
                        (((word >>> 7) & 0x00000001) << 11)  |
                        (((word >>> 8) & 0x0000000f) << 1)) + text.sh_addr + i * 4 -
                        (((word >>> 31) == 1) ? ((int) Math.pow(2, 12)) : 0)
                    );
                    var symDesc = symTabMap.get(jAddr);
                    if (symDesc == null) {
                        symDesc = new SymbolDescription("L" + numOfUndefLabels, -1, jAddr, 0, "LOCAL", "NOTYPE", "DEFAULT", "UNDEF");
                        symTabMap.put(jAddr, symDesc);
                        numOfUndefLabels++;
                    }
                    break;
                }
                case JUMP: {
                    final int jAddr = ((
                        (((word >>> 21) & 0x000003ff) << 1)  |
                        (((word >>> 20) & 0x00000001) << 11)  |
                        (((word >>> 12) & 0x000000ff) << 12)) + text.sh_addr + i * 4 -
                        (((word >>> 31) == 1) ? ((int) Math.pow(2, 20)) : 0)
                    );
                    var symDesc = symTabMap.get(jAddr);
                    if (symDesc == null) {
                        symDesc = new SymbolDescription("L" + numOfUndefLabels, -1, jAddr, 0, "LOCAL", "NOTYPE", "DEFAULT", "UNDEF");
                        symTabMap.put(jAddr, symDesc);
                        numOfUndefLabels++;
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private int readNBytes(int idx, final int n) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            res += Byte.toUnsignedInt(data[idx++]) << (8 * i);
        }
        return res;
    }

    private String getInstruction(final int word) {
        return switch (word & 0b1111111) {
            case 0b0110111 -> "lui";
            case 0b0010111 -> "auipc";
            case 0b0010011 -> switch ((word >> 12) & 0b111) {
                case 0b000 -> "addi";
                case 0b010 -> "slti";
                case 0b011 -> "sltiu";
                case 0b100 -> "xori";
                case 0b110 -> "ori";
                case 0b111 -> "andi";
                case 0b001 -> "slli";
                case 0b101 -> switch ((word >> 25) & 0b1111111) {
                    case 0b0000000 -> "srli";
                    case 0b0100000 -> "srai";
                    default -> "unknown_instruction";
                };
                default -> "unknown_instruction";
            };
            case 0b0110011 -> switch ((word >> 12) & 0b111) {
                case 0b000 -> switch ((word >> 25) & 0b1111111) {
                    case 0b0000000 -> "add";
                    case 0b0100000 -> "sub";
                    case 0b0000001 -> "mul";
                    default -> "unknown_instruction";
                };
                case 0b001 -> switch ((word >> 25) & 0b1111111) {
                    case 0b0000000 -> "sll";
                    case 0b0000001 -> "mulh";
                    default -> "unknown_instruction";
                };
                case 0b010 -> switch ((word >> 25) & 0b1111111) {
                    case 0b0000000 -> "slt";
                    case 0b0000001 -> "mulhsu";
                    default -> "unknown_instruction";
                };
                case 0b011 -> switch ((word >> 25) & 0b1111111) {
                    case 0b0000000 -> "sltu";
                    case 0b0000001 -> "mulhsu";
                    default -> "unknown_instruction";
                };
                case 0b100 -> switch ((word >> 25) & 0b1111111) {
                    case 0b0000000 -> "xor";
                    case 0b0000001 -> "div";
                    default -> "unknown_instruction";
                };
                case 0b101 -> switch ((word >> 25) & 0b1111111) {
                    case 0b0000000 -> "srl";
                    case 0b0100000 -> "sra";
                    case 0b0000001 -> "divu";
                    default -> "unknown_instruction";
                };
                case 0b110 -> switch ((word >> 25) & 0b1111111) {
                    case 0b0000000 -> "or";
                    case 0b0000001 -> "rem";
                    default -> "unknown_instruction";
                };
                case 0b111 -> switch ((word >> 25) & 0b1111111) {
                    case 0b0000000 -> "and";
                    case 0b0000001 -> "remu";
                    default -> "unknown_instruction";
                };
                default -> "unknown_instruction";
            };
            case 0b1110011 -> switch ((word >> 12) & 0b111) {
                case 0b001 -> "csrrw";
                case 0b010 -> "csrrs";
                case 0b011 -> "csrrc";
                case 0b101 -> "csrrwi";
                case 0b110 -> "csrrsi";
                case 0b111 -> "csrrci";
                case 0b000 -> switch ((word >> 20) & 0b111111111111) {
                    case 0b000000000000 -> "ecall";
                    case 0b000000000001 -> "ebreak";
                    case 0b000000000010 -> "uret";
                    case 0b000100000010 -> "sret";
                    case 0b001100000010 -> "mret";
                    case 0b000100000101 -> "wfi";
                    default -> "unknown_instruction";
                };
                default -> "unknown_instruction";
            };
            case 0b0000011 -> switch ((word >> 12) & 0b111) {
                case 0b000 -> "lb";
                case 0b001 -> "lh";
                case 0b010 -> "lw";
                case 0b100 -> "lbu";
                case 0b101 -> "lhu";
                default -> "unknown_instruction";
            };
            case 0b0100011 -> switch ((word >> 12) & 0b111) {
                case 0b000 -> "sb";
                case 0b001 -> "sh";
                case 0b010 -> "sw";
                default -> "unknown_instruction";
            };
            case 0b1101111 -> "jal";
            case 0b1100111 -> "jalr";
            case 0b1100011 -> switch ((word >> 12) & 0b111) {
                case 0b000 -> "beq";
                case 0b001 -> "bne";
                case 0b100 -> "blt";
                case 0b101 -> "bge";
                case 0b110 -> "bltu";
                case 0b111 -> "bgeu";
                default -> "unknown_instruction";
            };
            default -> "unknown_instruction";
        };
    }

}
