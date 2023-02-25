module Cache(
    input wire[ADDR1_BUS_SIZE-1:0] A1,
    inout wire[DATA1_BUS_SIZE-1:0] D1,
    inout wire[CTR1_BUS_SIZE-1:0] C1,
    output wire[ADDR2_BUS_SIZE-1:0] A2,
    inout wire[DATA2_BUS_SIZE-1:0] D2,
    inout wire[CTR2_BUS_SIZE-1:0] C2,
    input wire clk,
    input wire reset,
    input wire C_DUMP
);
    logic[ADDR2_BUS_SIZE-1:0] A2_buffer;

    logic[DATA1_BUS_SIZE-1:0] D1_buffer;
    logic[CTR1_BUS_SIZE-1:0] C1_buffer;

    logic[DATA2_BUS_SIZE-1:0] D2_buffer;
    logic[CTR2_BUS_SIZE-1:0] C2_buffer;

    logic[CACHE_TAG_SIZE-1:0] tag;
    logic[CACHE_SET_SIZE-1:0] set;
    logic[CACHE_OFFSET_SIZE-1:0] offset;
    logic[31:0] data_buffer_LE; // LE - Little Endian
    bit index;
    logic[CTR1_BUS_SIZE-1:0] C1_copy;

    logic[7:0] data[CACHE_SETS_COUNT-1:0][CACHE_WAY-1:0][CACHE_LINE_SIZE-1:0]; 
    logic valid[CACHE_SETS_COUNT-1:0][CACHE_WAY-1:0];
    logic dirty[CACHE_SETS_COUNT-1:0][CACHE_WAY-1:0];
    bit LRU[CACHE_SETS_COUNT-1:0];
    logic[CACHE_TAG_SIZE-1:0] tags[CACHE_SETS_COUNT-1:0][CACHE_WAY-1:0];

    assign (strong1, pull0) A2 = A2_buffer;
    assign (strong1, pull0) D1 = D1_buffer;
    assign (strong1, pull0) D2 = D2_buffer;
    assign (strong1, pull0) C1 = C1_buffer;
    assign (strong1, pull0) C2 = C2_buffer;

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

    task reset_cache;
        begin
            tag = 0; set = 0; offset = 0; data_buffer_LE = 0; C1_copy = 0; index = 0;
            D2_buffer = 0; C2_buffer = 0; A2_buffer = 0;
            C1_buffer = 0; D1_buffer = 0;

            for(int i = 0; i < CACHE_SETS_COUNT; i++) begin
                for(int j = 0; j < CACHE_WAY; j++) begin
                    valid[i][j] = 0;
                    dirty[i][j] = 0;
                    LRU[i] = 0;
                    tags[i][j] = 0;
                    for (int k = 0; k < CACHE_LINE_SIZE; k++) begin
                        data[i][j][k] = 0;
                    end
                end
            end
        end
    endtask

    task dump_to_terminal;
        begin
            $display("----CACHE_DUMP----");
            for(int i = 0; i < CACHE_SETS_COUNT; i++) begin
                for(int j = 0; j < CACHE_WAY; j++) begin
                    $write("Cache_set=%b, idx=%b, CACHE_LINE: valid(b)=%b, dirty(b)=%b, LRU(b)=%b, tag(h)=%h, data(h)=",
                        i[0+:CACHE_SET_SIZE], j[0+:CACHE_WAY_SIZE], valid[i][j], dirty[i][j], LRU[i], tags[i][j]
                    );
                    for (int k = 0; k < CACHE_LINE_SIZE; k++) begin
                        $write("%h", data[i][j][k]);
                    end
                    $display();
                end
            end
        end
    endtask

    task read_from_memory(input [CACHE_TAG_SIZE-1:0] tag, [CACHE_SET_SIZE-1:0] set, idx);
        begin
            C2_buffer = 'b10; A2_buffer = {tag, set}; D2_buffer = 0;
            next_tact();
            C2_buffer = 0; A2_buffer = 0; D2_buffer = 0;
            tags[set][idx] = tag;
            valid[set][idx] = 1;
            dirty[set][idx] = 0;
            LRU[set] = idx;
            next_clk();
            while (C2 != 1) begin
                next_tact();
            end
            for (int i = CACHE_LINE_SIZE - 2; i > -1; i -= 2) begin
                {data[set][idx][i + 1], data[set][idx][i]} = D2;
                next_tact();
            end
            next_clk();
            // log_read_from_memory(set, idx);
        end
    endtask

    task log_read_from_memory(input [CACHE_SET_SIZE-1:0] set, idx);
        begin
            $write("read_from_memory: \t");
            for (int i = 0; i < CACHE_LINE_SIZE; i++) $write("%h", data[set][idx][i]);
            $write("\ttag: %b, set: %b, idx: %b", tags[set][idx], set, idx);
            $display("");
        end
    endtask

    task upload_data_to_memory(input [CACHE_SET_SIZE-1:0] set, idx);
        begin
            D2_buffer = {data[set][idx][CACHE_LINE_SIZE - 1], data[set][idx][CACHE_LINE_SIZE - 2]};
            A2_buffer = {tags[set][idx], set};
            C2_buffer = 3;
            next_tact();
            for (int i = CACHE_LINE_SIZE - 4; i >= 0; i -= 2) begin
                D2_buffer = {data[set][idx][i + 1], data[set][idx][i]};
                next_tact();
                A2_buffer = 0;
            end
            C2_buffer = 0; D2_buffer = 0;
            dirty[set][idx] = 0;
            next_clk();
            while (C2 != 1) begin
                next_tact();
            end
            next_tact(); next_clk();
            // log_upload_data_to_memory(set, idx);
        end
    endtask

    task log_upload_data_to_memory(input [CACHE_SET_SIZE-1:0] set, idx);
        begin
            $write("upload_data_to_memory: \t");
            for (int i = 0; i < CACHE_LINE_SIZE; i++) $write("%h", data[set][idx][i]);
            $display("");
        end
    endtask

    initial begin
        reset_cache();
    end

    always @(posedge C_DUMP) begin
        dump_to_terminal();
    end

    always @(posedge reset) begin
        reset_cache();
    end

    always @(negedge clk) begin
        if (C1 == 4) begin // ===============================C1_INVALIDATE_LINE===============================
            {tag, set} = A1;
            if (tags[set][0] == tag || tags[set][1] == tag) begin
                index = (tags[set][0] == set) ? 0 : 1;
                valid[set][index] = 0;
            end
            while (C1 != 0) begin
                next_tact();
            end
            C1_buffer = 3'b111;
            next_tact();
            C1_buffer = 0;
        end else if (1 <= C1 && C1 <= 3) begin // ===============================C1_READING===============================
            {tag, set} = A1; C1_copy = C1;
            next_tact();
            offset = A1[0+:CACHE_OFFSET_SIZE];
            next_clk();
            for (int i = 0; i < 2; i++) next_tact(); // waiting
            if (tags[set][0] == tag || tags[set][1] == tag) begin
                index = (tags[set][0] == tag) ? 0 : 1;
                if (valid[set][index] == 1) begin
                    for (int i = 0; i < 2; i++) next_tact();
                end else begin
                    read_from_memory(tag, set, index);
                end
            end else if (valid[set][0] == 0 || valid[set][1] == 0) begin
                index = (valid[set][0] == 0) ? 0 : 1;
                read_from_memory(tag, set, index);
            end else begin
                index = (LRU[set] == 1) ? 0 : 1;
                if (dirty[set][index] == 1 && valid[set][index] == 1)
                    upload_data_to_memory(set, index);
                read_from_memory(tag, set, index);
            end
            LRU[set] = index; tags[set][index] = tag; valid[set][index] = 1;
            C1_buffer = 3'b111;
            case (C1_copy)
                3'b001: begin
                    D1_buffer = data[set][index][offset];
                end
                3'b010: begin
                    D1_buffer = {data[set][index][offset + 1], data[set][index][offset]};
                end
                3'b011: begin
                    D1_buffer = {data[set][index][offset + 3], data[set][index][offset + 2]};
                    next_tact();
                    D1_buffer = {data[set][index][offset + 1], data[set][index][offset]};
                end
            endcase
            C1_copy = 0;
            next_tact();
            C1_buffer = 0; D1_buffer = 0;

        end else if (5 <= C1 && C1 <= 7) begin // ===============================C1_WRITE===============================
            {tag, set} = A1; C1_copy = C1; data_buffer_LE[2*DATA1_BUS_SIZE-1:DATA1_BUS_SIZE] = D1;
            next_tact();

            if (C1 == 7) data_buffer_LE[DATA1_BUS_SIZE-1:0] = D1;

            offset = A1[0+:CACHE_OFFSET_SIZE];
            next_clk();

            for (int i = 0; i < 2; i++) next_tact(); // waiting

            if (tags[set][0] == tag || tags[set][1] == tag) begin
                index = (tags[set][0] == tag) ? 0 : 1;
                if (valid[set][index] == 1) begin
                    for (int i = 0; i < 2; i++) next_tact();
                end else begin
                    read_from_memory(tag, set, index);
                end
            end else if (valid[set][0] == 0 || valid[set][1] == 0) begin
                index = (valid[set][0] == 0) ? 0 : 1;
                read_from_memory(tag, set, index);
            end else begin
                index = (LRU[set] == 1) ? 0 : 1;
                if (dirty[set][index] == 1 && valid[set][index] == 1)
                    upload_data_to_memory(set, index);
                read_from_memory(tag, set, index);
            end
            
            LRU[set] = index; tags[set][index] = tag; valid[set][index] = 1; dirty[set][index] = 1;

            case (C1_copy)
                3'b101: begin
                    data[set][index][offset] = data_buffer_LE[23:16];
                end
                3'b110: begin
                    data[set][index][offset] = data_buffer_LE[23:16];
                    data[set][index][offset + 1] = data_buffer_LE[31:24];
                end
                3'b111: begin
                    for (int i = 0; i < 4; i++)
                        data[set][index][offset + i] = data_buffer_LE[i*8+:8];
                end
            endcase

            C1_copy = 0; C1_buffer = 3'b111;
            next_tact();
            C1_buffer = 0;
        end
    end
endmodule