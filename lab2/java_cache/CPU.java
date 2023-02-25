public class CPU {
    public static void main(String[] args) {
        final long[] tacts = new long[1];
        final Cache cache = new Cache(tacts);

        final int M = 64;
        final int N = 60;
        final int K = 32;

        final int aStart = 0;
        final int bStart = aStart + M * K;
        final int cStart = bStart + 2 * K * N;

        int pa = aStart;
        int pb = bStart;
        int pc = cStart;
        int cacheHits = 0;
        int cacheMisses = 0;

        double tactsBefore;

        tacts[0] = 1;
        tacts[0] += 5;
        for (int y = 0; y < M; y++) {
            tacts[0] += 1; // new iteration
            for (int x = 0; x < N; x++) {
                tacts[0] += 1; // new iteration
                pb = bStart; tacts[0] += 1;
                tacts[0] += 1; // initialization s
                for (int k = 0; k < K; k++) {
                    tacts[0] += 1; // new iteration
                    tactsBefore = tacts[0];
                    cache.readByte(pa + k);
                    if (tacts[0] - tactsBefore > 100) {
                        cacheMisses += 1;
                    } else {
                        cacheHits += 1;
                    }
                    tactsBefore = tacts[0];
                    cache.readTwoBytes(pb + 2 * x);
                    if (tacts[0] - tactsBefore > 100) {
                        cacheMisses += 1;
                    } else {
                        cacheHits += 1;
                    }
                    pb += 2 * N;
                    tacts[0] += 2; // two times sum
                    tacts[0] += 5; // multiplication
                }
                tactsBefore = tacts[0];

                cache.write(pc + 4 * x);
                if (tacts[0] - tactsBefore > 100) {
                    cacheMisses += 1;
                } else {
                    cacheHits += 1;
                }
            }
            pa += K;
            pc += 4 * N;
            tacts[0] += 7; // two times sum and multiplication 
        }
        tacts[0] += 1; // exit of the function
        System.out.println("Cache hits: " + cacheHits);
        System.out.println("Cache requests: " + (cacheHits + cacheMisses));
        System.out.println("Percent of cache hits: " + (((double) cacheHits) / (cacheHits + cacheMisses)) * 100 + "%");
        System.out.println("Number of tacts: " + (int) tacts[0]);
    }
}