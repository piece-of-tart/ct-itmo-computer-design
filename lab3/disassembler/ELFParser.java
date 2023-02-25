package disassembler;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import disassembler.exceptions.ELFException;
import disassembler.exceptions.FileHeaderException;

public final class ELFParser {
    private byte[] elf;
    private final FileHeaderParser fileHeaderParser;
    private List<SymbolDescription> symtab;
    private Map<String, ELFSection> sectionsMap;
    private List<ELFSection> sectionsList;

    public ELFParser() {
        this.fileHeaderParser = new FileHeaderParser();
    }

    public void parse(final byte[] elf) throws FileHeaderException, ELFException {
        this.elf = elf;
        fileHeaderParser.parse(elf);
        parseAndFillSections();
        parseSymTab();
    }

    public void printSymTab(final PrintStream out) {
        out.println(".symtab");
        out.println("Symbol Value         	  Size Type     Bind 	 Vis   	  Index Name");
        int i = 0;
        for (SymbolDescription e : symtab) {
            out.printf("[%4d] 0x%-15X %5d %-8s %-8s %-8s %6s %s\n", i, e.st_value, e.st_size, e.st_type, e.st_bind, e.st_vis, e.st_shndx, e.name);
            i++;
        }
    } 

    public Map<Integer, SymbolDescription> getSymTabMap() {
        final Map<Integer, SymbolDescription> symTabMap = new HashMap<>(symtab.size() * 2);
        for (SymbolDescription symDesc : symtab) {
            symTabMap.put(symDesc.st_value, symDesc);
        }
        return symTabMap;
    }

    public ELFSection getELFSection(final String name) {
        return sectionsMap.get(name);
    }

    private void parseSymTab() throws ELFException {
        this.symtab = new ArrayList<>();
        final ELFSection symTabSec = sectionsMap.get(".symtab");
        final ELFSection strTabSec = sectionsMap.get(".strtab");
        int idx = symTabSec.sh_offset;

        if (symTabSec == null || strTabSec == null) {
            throw new ELFException("The ELF file does not contain sections .symtab or .strtab", idx);
        }
        
        for (int i = 0; i < symTabSec.sh_size / symTabSec.sh_entsize; i++) {
            final String name;
            if ((readNBytes(idx + 12, 1) & 0xf) == 0x3) { // SECTION type
                name = sectionsList.get(readNBytes(idx + 14, 2)).s_name;
            } else { // other types
                name = readName(readNBytes(idx, 4) + strTabSec.sh_offset);
            }
            symtab.add(
                new SymbolDescription(
                    name,
                    readNBytes(idx, 4), 
                    readNBytes(idx + 4, 4), 
                    readNBytes(idx + 8, 4),
                    switch (readNBytes(idx + 12, 1) >> 4) {
                        case 0x0 -> "LOCAL";
                        case 0x1 -> "GLOBAL";
                        case 0x2 -> "WEAK";
                        case 0xd, 0xe, 0xf -> "<processor specific>: " + (readNBytes(idx + 12, 1) >> 4);
                        default -> throw new ELFException("Unexpected Symbol Binding while parsing .symtab section (name='" + name + "'):" + (readNBytes(idx + 12, 1) >> 4), idx + 12);
                    },
                    switch (readNBytes(idx + 12, 1) & 0xf) {
                        case 0x0 -> "NOTYPE";
                        case 0x1 -> "OBJECT";
                        case 0x2 -> "FUNC";
                        case 0x3 -> "SECTION";
                        case 0x4 -> "FILE";
                        case 0xd, 0xe, 0xf -> "<processor specific>: " + (readNBytes(idx + 12, 1) & 0xf);
                        default -> throw new ELFException("Unexpected Symbol Types while parsing .symtab section (name='" + name + "'):" + (readNBytes(idx + 12, 1) & 0xf), idx + 12);
                    },
                    switch (readNBytes(idx + 13, 1)) {
                        case 0x0 -> "DEFAULT";
                        case 0x1 -> "INTERNAL";
                        case 0x2 -> "HIDDEN";
                        case 0x3 -> "PROTECTED";
                        default -> throw new ELFException("Unexpected Symbol Visibilty while parsing .symtab section (name='" + name + "'):" + (readNBytes(idx + 13, 1) & 0xf), idx + 13);
                    },
                    switch (readNBytes(idx + 14, 2)) {
                        case 0x0 -> "UNDEF";
                        case 0xff00 -> "LORESERVE";
                        case 0xfff1 -> "ABS";
                        case 0xfff2 -> "COMMON";
                        case 0xffff -> "HIRESERVE";
                        default -> {
                            final int specialSectionIdx = readNBytes(idx + 14, 2);
                            if (0xff00 <= specialSectionIdx && specialSectionIdx <= 0xff1f) {
                                yield "<processor specific>:" + specialSectionIdx;
                            } else {
                                yield String.valueOf(specialSectionIdx);
                            }
                        }
                    }
                )
            );
            idx += symTabSec.sh_entsize;
        }
    }

    private void parseAndFillSections() {
        this.sectionsMap = new HashMap<>();
        this.sectionsList = new ArrayList<>();

        final int shstrtabIdx = readNBytes(
            fileHeaderParser.e_shoff +
                fileHeaderParser.e_shstrndx * fileHeaderParser.e_shentsize + 4 * 4,
            2);

        int shIdx = fileHeaderParser.e_shoff;
        for (int i = 0; i < fileHeaderParser.e_shnum; i++) {
            int sh_name = readNBytes(shIdx, 4);

            final String name = readName(shstrtabIdx + sh_name);
            sectionsList.add(
                new ELFSection(name,
                    sh_name,
                    readNBytes(shIdx + 4, 4),
                    readNBytes(shIdx + 8, 4),
                    readNBytes(shIdx + 12, 4),
                    readNBytes(shIdx + 16, 4),
                    readNBytes(shIdx + 20, 4),
                    readNBytes(shIdx + 24, 4),
                    readNBytes(shIdx + 28, 4),
                    readNBytes(shIdx + 32, 4),
                    readNBytes(shIdx + 36, 4)
                )
            );
            sectionsMap.put(name, sectionsList.get(i));
            shIdx += fileHeaderParser.e_shentsize;
        }
    }

    private String readName(int idx) {
        final StringBuilder sb = new StringBuilder();
        while (readNBytes(idx, 1) != 0x00) {
            sb.append((char) readNBytes(idx++, 1));
        }
        return sb.toString();
    }

    private int readNBytes(int idx, final int n) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            res += Byte.toUnsignedInt(elf[idx++]) << (8 * i);
        }
        return res;
    }

    final class FileHeaderParser {
        private byte[] elf;
        private int pointer;
    
        public int e_entry;
        public int e_phoff;
        public int e_shoff;
        public int e_flags;
        public int e_ehsize;
        public int e_phentsize;
        public int e_phnum;
        public int e_shentsize;
        public int e_shnum;
        public int e_shstrndx;
    
        public void parse(final byte[] elf) throws FileHeaderException {
            this.elf = elf;
            checkFileHeader();
            setHeaderFilds();
        }
    
        private void setHeaderFilds() {
            e_entry = readNBytes(4);
            e_phoff = readNBytes(4);
            e_shoff = readNBytes(4);
            e_flags = readNBytes(4);
            e_ehsize = readNBytes(2);
            e_phentsize = readNBytes(2);
            e_phnum = readNBytes(2);
            e_shentsize = readNBytes(2);
            e_shnum = readNBytes(2);
            e_shstrndx = readNBytes(2);
        }
    
        private void checkFileHeader() throws FileHeaderException {
            // e_ident[16]
            if (readByte() != 0x7f || readByte() != 0x45 || readByte() != 0x4c || readByte() != 0x46) {
                throw new FileHeaderException("Illegal file signature. Magic number must be 0x7f 0x45 0x4c 0x46.", pointer);
            }
            if (readByte() != 0x01) {
                throw new FileHeaderException("Illegal EI_CLASS. Supported only 32-bit.", pointer);
            }
            if (readByte() != 0x01) {
                throw new FileHeaderException("Illegal endianess(EI_DATA). Supported only Little Endian.", pointer);
            }
            if (readByte() != 0x01) {
                throw new FileHeaderException("Illegal EI_VERSION.", pointer);
            }
            pointer = 16;
            // After this part data will be represented in little endian 
    
            // e_type
            pointer += 2;
            // e_machine
            if (readByte() != 0xf3 || readByte() != 0x00) {
                throw new FileHeaderException("Illegal ISA. Supported only RISC-V", pointer);
            }
            // e_version 
            pointer += 4;
        }
    
        private int readByte() {
            return Byte.toUnsignedInt(elf[pointer++]);
        }
    
        private int readNBytes(final int n) {
            int res = 0;
            for (int i = 0; i < n; i++) {
                res += Byte.toUnsignedInt(elf[pointer++]) << (8 * i);
            }
            return res;
        }
    }
}