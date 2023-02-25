module CPU(
    output wire[ADDR1_BUS_SIZE-1:0] A1,
    inout wire[DATA1_BUS_SIZE-1:0] D1,
    inout wire[CTR1_BUS_SIZE-1:0] C1,
    input clk
);

    localparam M = 64;
    localparam N = 60;
    localparam K = 32;

    localparam a_start_idx = 0;
    localparam b_start_idx = a_start_idx + M*K;
    localparam c_start_idx = b_start_idx + 2*K*N;

    int pa;
    int pb;
    int pc;
    int s;
    int tacts_cnt;
    int cache_hit_cnt;
    int cache_miss_cnt;
    int tacts_cnt_prev;

    logic[31:0] data32;
    logic[15:0] data16;
    logic [7:0] data8;

    logic[CTR1_BUS_SIZE-1:0] C1_buffer;
    logic[DATA1_BUS_SIZE-1:0] D1_buffer;
    logic[ADDR1_BUS_SIZE-1:0] A1_buffer;

    assign (strong1, pull0) A1 = A1_buffer;
    assign (strong1, pull0) D1 = D1_buffer;
    assign (strong1, pull0) C1 = C1_buffer;

    task next_clk;
        begin
            if (clk == 0) wait(clk != 0);
            else if (clk == 1) wait(clk != 1);
        end
    endtask

    task next_tact;
        begin 
            next_clk();
            next_clk();
        end
    endtask

    task write32_by_abs_addr(input [CACHE_ADDR_SIZE] addr, [31:0] data);
        begin
            C1_buffer = 7; D1_buffer = {data[7:0], data[15:8]}; A1_buffer = addr[CACHE_OFFSET_SIZE+:CACHE_SET_SIZE+CACHE_TAG_SIZE];
            next_tact();
            A1_buffer = addr[0+:CACHE_OFFSET_SIZE]; D1_buffer = {data[23:16], data[31:24]};
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_tact();
            end
            next_tact(); next_clk();
        end
    endtask

    task read32_by_abs_addr(input [CACHE_ADDR_SIZE] addr, output [31:0] data);
        begin
            C1_buffer = 3; A1_buffer = addr[CACHE_OFFSET_SIZE+:CACHE_SET_SIZE+CACHE_TAG_SIZE];
            next_tact();
            A1_buffer = addr[0+:CACHE_OFFSET_SIZE];
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_clk();
            end
            data[16:0] = {D1[7:0], D1[15:8]};
            next_tact();
            data[31:16] = {D1[7:0], D1[15:8]}; C1_buffer = 0; D1_buffer = 0; A1_buffer = 0; 
            next_tact(); next_clk();
        end
    endtask

    task read8_by_abs_addr(input [CACHE_ADDR_SIZE] addr, output [7:0] data);
        begin
            C1_buffer = 1; A1_buffer = addr[CACHE_OFFSET_SIZE+:CACHE_SET_SIZE+CACHE_TAG_SIZE];
            next_tact();
            A1_buffer = addr[0+:CACHE_OFFSET_SIZE];
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_tact();
            end
            data = D1; C1_buffer = 0; D1_buffer = 0; A1_buffer = 0; 
            next_tact(); next_clk();
        end
    endtask;

    task read16_by_abs_add(input [CACHE_ADDR_SIZE] addr, output [15:0] data);
        begin
            C1_buffer = 2; A1_buffer = addr[CACHE_OFFSET_SIZE+:CACHE_SET_SIZE+CACHE_TAG_SIZE];
            next_tact();
            A1_buffer = addr[0+:CACHE_OFFSET_SIZE];
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_tact();
            end
            data = {D1[7:0], D1[15:8]}; C1_buffer = 0; D1_buffer = 0; A1_buffer = 0; 
            next_tact(); next_clk();
        end
    endtask;

    task write8(input [7:0] data, [CACHE_TAG_SIZE-1:0] tag, [CACHE_SET_SIZE-1:0] set, [CACHE_OFFSET_SIZE-1:0] offset);
        begin
            C1_buffer = 5; D1_buffer = data; A1_buffer = {tag, set};
            next_tact();
            A1_buffer = offset;
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_tact();
            end
            next_tact(); next_clk();
        end
    endtask

    task write16(input [15:0] data, [CACHE_TAG_SIZE-1:0] tag, [CACHE_SET_SIZE-1:0] set, [CACHE_OFFSET_SIZE-1:0] offset);
        begin
            C1_buffer = 6;
            D1_buffer = {data[7:0], data[15:8]}; A1_buffer = {tag, set};
            next_tact();
            A1_buffer = offset;
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_tact();
            end
            next_tact(); next_clk();
        end
    endtask

    task write32(input [31:0] data, [CACHE_TAG_SIZE-1:0] tag, [CACHE_SET_SIZE-1:0] set, [CACHE_OFFSET_SIZE-1:0] offset);
        begin
            C1_buffer = 7; D1_buffer = {data[7:0], data[15:8]}; A1_buffer = {tag, set};
            next_tact();
            A1_buffer = offset; D1_buffer = {data[23:16], data[31:24]};
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_tact();
            end
            next_tact(); next_clk();
        end
    endtask

    task read8(input [CACHE_TAG_SIZE-1:0] tag, [CACHE_SET_SIZE-1:0] set, [CACHE_OFFSET_SIZE-1:0] offset, output [7:0] data);
        begin
            C1_buffer = 1; A1_buffer = {tag, set};
            next_tact();
            A1_buffer = offset;
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_tact();
            end
            data = D1; C1_buffer = 0; D1_buffer = 0; A1_buffer = 0; 
            next_tact(); next_clk();
        end
    endtask

    task read16(input [CACHE_TAG_SIZE-1:0] tag, [CACHE_SET_SIZE-1:0] set, [CACHE_OFFSET_SIZE-1:0] offset, output [16:0] data);
        begin
            C1_buffer = 2; A1_buffer = {tag, set};
            next_tact();
            A1_buffer = offset;
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_tact();
            end
            data = {D1[7:0], D1[15:8]}; C1_buffer = 0; D1_buffer = 0; A1_buffer = 0; 
            next_tact(); next_clk();
        end
    endtask

    task read32(input [CACHE_TAG_SIZE-1:0] tag, [CACHE_SET_SIZE-1:0] set, [CACHE_OFFSET_SIZE-1:0] offset, output [31:0] data);
        begin
            C1_buffer = 3; A1_buffer = {tag, set};
            next_tact();
            A1_buffer = offset;
            next_tact();
            A1_buffer = 0; D1_buffer = 0; C1_buffer = 0;
            next_clk(); 
            while (C1 != 7) begin
                next_tact();
            end
            data[16:0] = {D1[7:0], D1[15:8]};
            next_tact();
            data[31:16] = {D1[7:0], D1[15:8]}; C1_buffer = 0; D1_buffer = 0; A1_buffer = 0; 
            next_tact(); next_clk();
        end
    endtask

    task invalidate_line(input [CACHE_TAG_SIZE-1:0] tag, [CACHE_SET_SIZE-1:0] set);
        begin 
            C1_buffer = 4; A1_buffer = {tag, set};
            next_tact();
            C1_buffer = 0;
            next_clk();
            while (C1 != 7) begin
                next_tact();
            end
            next_tact(); next_clk();
        end
    endtask

    initial begin
        A1_buffer = 0;
        C1_buffer = 0;
        D1_buffer = 0;
    end

    initial begin
        pa = a_start_idx; pb = b_start_idx; pc = c_start_idx;
        data8 = 0; data16 = 0; data32 = 0;
        cache_hit_cnt = 0; cache_miss_cnt = 0; tacts_cnt_prev = 0; tacts_cnt = 0;
    end

    always @(posedge clk) begin
        tacts_cnt += 1;
    end

    always @(posedge clk) begin
        for(int i = 0; i < 3; i++) begin
            next_tact(); // init a, b, c arrays
        end
        pa = a_start_idx; next_tact(); // init pa
        pc = c_start_idx; next_tact(); // init pc

        for (int y = 0; y < M; y++) begin

            next_tact(); // new iteration
            // $display("%d out of %d", y, M);

            for (int x = 0; x < N; x++) begin
                next_tact(); // new iteration
                pb = b_start_idx; next_tact(); // init
                s = 0; next_tact(); // init
                for (int k = 0; k < K; k++) begin
                    next_tact(); // new iteration

                    tacts_cnt_prev = tacts_cnt;
                    read8_by_abs_addr(pa + k, data8);

                    if (tacts_cnt - tacts_cnt_prev < 100) 
                        cache_hit_cnt += 1;
                    else
                        cache_miss_cnt += 1;

                    tacts_cnt_prev = tacts_cnt;
                    read16_by_abs_add(pb + 2*x, data16);

                    if (tacts_cnt - tacts_cnt_prev < 100)
                        cache_hit_cnt += 1;
                    else
                        cache_miss_cnt += 1;

                    s += data8 * data16; // sum +1 and multiplication +5
                    pb += 2 * N; // sum +1         => 7 tacts
                    for (int i = 0; i < 7; i++) next_tact();
                end
                tacts_cnt_prev = tacts_cnt;
                write32_by_abs_addr(pc + 4 * x, s);

                if (tacts_cnt - tacts_cnt_prev < 100)
                    cache_hit_cnt += 1;
                else
                    cache_miss_cnt += 1;
            end
            pa += K; // sum +1
            pc += 4 * N; // sum +1, multiplication +5   => 7 tacts
            for (int i = 0; i < 7; i++) next_tact();
        end
        next_tact(); // exit of the function
        $display("Cache hits:%d", cache_hit_cnt);
        $display("Cache requests:%d", (cache_hit_cnt + cache_miss_cnt));
        $display("Number of tacts:%d", tacts_cnt);
        $finish();
    end // CacheHits:228080, cacheMisses:21520 tacts:5495053

    // always @(posedge clk) begin
    //     write32('h12345678, 'b1010101010, 'b00101, 5);
    //     write16('h1389, 'b1010101010, 'b00101, 1);
    //     write8('h09, 'b1010111110, 'b01000, 15);
    //     read32('b1010101010, 'b00101, 5, data32);
    //     read16('b1010101010, 'b00101, 1, data16);
    //     read8('b1010111110, 'b01000, 'b1111, data8);
    //     $display("CPU: t=%0t, data32=%h", $time, data32);
    //     $display("CPU: t=%0t, data16=%h", $time, data16);
    //     $display("CPU: t=%0t, data8=%h", $time, data8);
    //     $finish();
    // end
endmodule