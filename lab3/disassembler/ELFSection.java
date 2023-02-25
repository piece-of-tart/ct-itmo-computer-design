package disassembler;

public class ELFSection {
    public final String s_name;
    public final int sh_name;
    public final int sh_type;
    public final int sh_flags;
    public final int sh_addr;
    public final int sh_offset;
    public final int sh_size;
    public final int sh_link;
    public final int sh_info;
    public final int sh_addralign;
    public final int sh_entsize;

    public ELFSection(String s_name, int sh_name, int sh_type, int sh_flags, int sh_addr, int sh_offset, int sh_size,
    int sh_link, int sh_info, int sh_addralign, int sh_entsize) {
        this.s_name = s_name;
        this.sh_name = sh_name;
        this.sh_type = sh_type;
        this.sh_flags = sh_flags;
        this.sh_addr = sh_addr;
        this.sh_offset = sh_offset;
        this.sh_size = sh_size;
        this.sh_link = sh_link;
        this.sh_info = sh_info;
        this.sh_addralign = sh_addralign;
        this.sh_entsize = sh_entsize;
    }

    @Override
    public String toString() {
        return "ELFSection [s_name=" + s_name + ", sh_name=" + sh_name + ", sh_type=" + sh_type + ", sh_flags="
                + sh_flags + ", sh_addr=" + sh_addr + ", sh_offset=" + sh_offset + ", sh_size=" + sh_size + ", sh_link="
                + sh_link + ", sh_info=" + sh_info + ", sh_addralign=" + sh_addralign + ", sh_entsize=" + sh_entsize
                + "]";
    }
}