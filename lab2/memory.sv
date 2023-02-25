module Memory(
    input wire[ADDR2_BUS_SIZE - 1:0] A2,
    inout wire[DATA2_BUS_SIZE - 1:0] D2,
    inout wire[CTR2_BUS_SIZE - 1:0] C2,
    input wire clk,
    input wire reset,
    input wire M_DUMP
);

    integer SEED = _SEED;

    logic[7:0] data[MEM_CACHE_LINE_COUNT-1:0][CACHE_LINE_SIZE-1:0];

    logic[DATA2_BUS_SIZE-1:0] D2_buffer;
    logic[CTR2_BUS_SIZE-1:0] C2_buffer;

    logic[CACHE_ADDR_SIZE+CACHE_OFFSET_SIZE-1:0] cache_line_addr;

    assign (strong1, pull0) D2 = D2_buffer;
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


    task reset_memory;
        begin
            cache_line_addr = 0;
            D2_buffer = 0; C2_buffer = 0;
            for (int i = 0; i < MEM_CACHE_LINE_COUNT; i++) begin
                for (int j = 0; j < CACHE_LINE_SIZE; j++) begin
                    data[i][j] = $random(SEED)>>16;
                end
            end
        end
    endtask

    task dump_to_terminal;
        begin
            $display("----MEMORY_DUMP----");
            for(int i = 0; i < MEM_CACHE_LINE_COUNT; i++) begin
                $write("%b: ", i[0+:CACHE_TAG_SIZE+CACHE_SET_SIZE]);
                for (int j = 0; j < CACHE_LINE_SIZE; j++) begin
                    $write("%h", data[i][j]);
                end
                $display();
            end
        end
    endtask


    initial begin
        reset_memory();
    end

    always @(posedge reset) begin
        reset_memory();
    end

    always @(posedge M_DUMP) begin
        dump_to_terminal();
    end

    always @(negedge clk) begin

        if (C2 == 2) begin // ===============================C2_READ===============================

            cache_line_addr = A2;

            for (int i = 0; i < 100; i++) next_tact();

            next_clk();
            D2_buffer = {data[cache_line_addr][CACHE_LINE_SIZE - 1], data[cache_line_addr][CACHE_LINE_SIZE - 2]}; 
            C2_buffer = 1;
            for (int i = CACHE_LINE_SIZE - 4; i > -1; i -= 2) begin
                next_tact();
                D2_buffer = {data[cache_line_addr][i+1], data[cache_line_addr][i]};
            end
            next_tact();
            C2_buffer = 0; D2_buffer = 0;

        end else if (C2 == 3) begin // ===============================C2_WRITE===============================

            cache_line_addr = A2;

            for (int i = CACHE_LINE_SIZE - 2; i > -1; i -= 2) begin
                {data[cache_line_addr][i+1], data[cache_line_addr][i]} = D2;
                next_tact();
            end

            for (int i = 0; i < 100 - (CACHE_LINE_SIZE / 2); i++) next_tact();
            next_clk();
            C2_buffer = 1;
            next_tact();
            C2_buffer = 0; D2_buffer = 0;
        end
    end

endmodule