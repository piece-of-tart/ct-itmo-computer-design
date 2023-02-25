`include "memory.sv"
`include "cache.sv"
`include "CPU.sv"

// bytes:
parameter MEM_SIZE = (1 << 19);
parameter CACHE_SIZE = (64 * 16);
parameter CACHE_LINE_SIZE = (16);

// integers:
parameter _SEED = (225526);
parameter MEM_CACHE_LINE_COUNT = MEM_SIZE / CACHE_LINE_SIZE;
parameter CACHE_LINE_COUNT = (64);
parameter CACHE_WAY = (2);
parameter CACHE_SETS_COUNT = CACHE_LINE_COUNT / CACHE_WAY;

// bits:
parameter CACHE_TAG_SIZE = (10);
parameter CACHE_SET_SIZE = (5);
parameter CACHE_OFFSET_SIZE = (4);
parameter CACHE_ADDR_SIZE = (19);
parameter CACHE_WAY_SIZE = (1);
parameter ADDR1_BUS_SIZE = (CACHE_TAG_SIZE + CACHE_SET_SIZE);
parameter DATA1_BUS_SIZE = (16);
parameter CTR1_BUS_SIZE = (3);
parameter ADDR2_BUS_SIZE = ADDR1_BUS_SIZE;
parameter DATA2_BUS_SIZE = (16);
parameter CTR2_BUS_SIZE = (2);

module testbench();
    wire[ADDR1_BUS_SIZE-1:0] A1;
    wire[CTR1_BUS_SIZE-1:0] C1;
    wire[DATA1_BUS_SIZE-1:0] D1;
    wire[ADDR2_BUS_SIZE-1:0] A2;
    wire[CTR2_BUS_SIZE-1:0] C2;
    wire[DATA2_BUS_SIZE-1:0] D2;
    bit clk;
    bit reset;
    bit C_DUMP;
    bit M_DUMP;

    CPU cpu (A1, D1, C1, clk);
    Cache cache (A1, D1, C1, A2, D2, C2, clk, reset, C_DUMP);
    Memory memory (A2, D2, C2, clk, reset, M_DUMP);

    task next_clk;
        begin
            if (clk == 0) wait(clk != 0);
            else if (clk == 1) wait(clk != 1);
        end
    endtask

    always #1 clk = ~clk;

    initial begin
        clk = 0;
        reset = 0;
    end

    initial begin
        $dumpfile("dump.vcd");
        $dumpvars(0, testbench);
    end

    // initial begin
    //     next_clk(); next_clk(); next_clk(); next_clk();
    //     M_DUMP = 1;
    // end

endmodule