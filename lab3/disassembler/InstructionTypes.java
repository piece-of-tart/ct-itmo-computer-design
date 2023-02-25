package disassembler;

public enum InstructionTypes {
    UPPER_IMMEDIATE, // lui rd, imm
    STORE, // sb rs2, offset(rs1)
    BRANCH, // beq rs1, rs2, offset
    JUMP, // jal rd, offset
    JUMP_WITH_REG, // jalr rd, rs1, offset
    REGISTER_REGISTER, // add rd, rs1, rs2
    IMMEDIATE_L, // lb rd, offset(rs1)
    IMMEDIATE_CSRR, // csrrw rd, offset, rs1
    IMMEDIATE, // addi rd, rs1, imm
    I_SHAMT, // slli rd, rs1, shamt
    SPECIAL, // ecall
    UNKNOWN_INSTRUCTION;
}