package disassembler;

public class SymbolDescription {
    public final String name;
    public final int st_name;
    public final int st_value;
    public final int st_size;
    public final String st_bind;
    public final String st_type;
    public final String st_vis;
    public final String st_shndx;

    public SymbolDescription(String name, int st_name, int st_value, int st_size, String st_bind, String st_type, String st_vis, String st_shndx) {
        this.name = name;
        this.st_name = st_name;
        this.st_value = st_value;
        this.st_size = st_size;
        this.st_bind = st_bind;
        this.st_type = st_type;
        this.st_vis = st_vis;
        this.st_shndx = st_shndx;
    }

    @Override
    public String toString() {
        return "SymbolDescription [name=" + name + ", st_name=" + st_name + ", st_value=" + st_value + ", st_size="
                + st_size + ", st_bind=" + st_bind + ", st_type=" + st_type + ", st_vis=" + st_vis + ", st_shndx="
                + st_shndx + "]";
    }
}