public class Cache {
    public static final int CACHE_SETS_COUNT = 32;
    public static final int CACHE_WAY = 2;
    public static final int CACHE_TAG_SIZE = 10;
    public static final int CACHE_SET_SIZE = 5;
    public static final int CACHE_OFFSET_SIZE = 4;

    private final long[] tacts; // array with length = 1
    private final int tags[][];
    private final boolean valid[][];
    private final boolean dirty[][];
    private final int LRU[];

    public Cache(final long[] tacts) {
        this.tacts = tacts;

        this.tags = new int[CACHE_SETS_COUNT][CACHE_WAY];
        this.valid = new boolean[CACHE_SETS_COUNT][CACHE_WAY];
        this.dirty = new boolean[CACHE_SETS_COUNT][CACHE_WAY];
        this.LRU = new int[CACHE_SETS_COUNT];

        for (int i = 0; i < CACHE_SETS_COUNT; i++) {
            for (int j = 0; j < CACHE_WAY; j++) {
                tags[i][j] = 0;
                valid[i][j] = false;
                dirty[i][j] = false;
                LRU[i] = 0;
            }
        }
    }

    public void readByte(final int address) {
        tacts[0] += 2;
        final int tag = address >>> (CACHE_SET_SIZE + CACHE_OFFSET_SIZE);
        final int set = (address << (32 - CACHE_SET_SIZE - CACHE_OFFSET_SIZE)) >>> (32 - CACHE_TAG_SIZE + CACHE_SET_SIZE);
        final int index;
        tacts[0] += 2;
        if (tags[set][0] == tag || tags[set][1] == tag)  {
            index = (tags[set][0] == tag) ? 0 : 1;
            if (valid[set][index]) {
                tacts[0] += 2;
            } else {
                readFromMemory(tag, set, index);
            }
        } else if (!valid[set][0] || !valid[set][1]) {
            index = valid[set][0] ? 1 : 0;
            readFromMemory(tag, set, index);
        } else {
            index = (LRU[set] == 1) ? 0 : 1;
            if (dirty[set][index] && valid[set][index]) {
                uploadDataToMemory(set, index);
            }
            readFromMemory(tag, set, index);
        }
        // sending data to the CPU in one tact
        tacts[0] += 2;
        LRU[set] = index;
        tags[set][index] = tag;
        valid[set][index] = true;
        return;
    }

    public void readTwoBytes(final int address) {
        readByte(address);
    }

    public void readFourBytes(final int address) {
        readTwoBytes(address);
        tacts[0] += 1;
    }

    public void write(final int address) {
        tacts[0] += 2;
        final int tag = address >>> (CACHE_SET_SIZE + CACHE_OFFSET_SIZE);
        final int set = (address << (32 - CACHE_SET_SIZE - CACHE_OFFSET_SIZE)) >>> (32 - CACHE_TAG_SIZE + CACHE_SET_SIZE);
        // System.out.println("W " + Integer.toBinaryString(tag) + " " + Integer.toBinaryString(set));
        final int index;
        tacts[0] += 2;
        if (tags[set][0] == tag || tags[set][1] == tag)  {
            index = (tags[set][0] == tag) ? 0 : 1;
            if (valid[set][index]) {
                tacts[0] += 2;
            } else {
                readFromMemory(tag, set, index);
            }
        } else if (!valid[set][0] || !valid[set][1]) {
            index = valid[set][0] ? 1 : 0;
            readFromMemory(tag, set, index);
        } else {
            index = (LRU[set] == 1) ? 0 : 1;
            if (dirty[set][index] && valid[set][index]) {
                uploadDataToMemory(set, index);
            }
            readFromMemory(tag, set, index);
        }
        // sending response to the CPU
        tacts[0] += 2;
        LRU[set] = index;
        dirty[set][index] = true;
        tags[set][index] = tag;
        valid[set][index] = true;
    }

    private void uploadDataToMemory(final int set, final int index) {
        tacts[0] += 1;
        tacts[0] += 8; // sending data to memory
        tacts[0] += 100 - 8; // delay
        // sending response on the C2 wire
        tacts[0] += 2;
        dirty[set][index] = false;
    }

    private void readFromMemory(final int tag, final int set, final int index) {
        tacts[0] += 1;
        tacts[0] += 100; // delay
        tacts[0] += 7; // sending data to the cache
        tacts[0] += 2;
        tags[set][index] = tag;
        valid[set][index] = true;
        dirty[set][index] = false;
        LRU[set] = index;
    }
}
