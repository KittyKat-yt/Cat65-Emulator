package org.kittykat.cat65.core.cpu;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

record OpcodeDef(IntSupplier input, int cycles, int[] opcodes, String args) {}
record OpcodeType(Consumer<OpcodeContext> method, OpcodeDef[] opcodeDefinitions) {}

class OpcodeContext {
    public int input;
    public String args;

    public OpcodeContext(int input, String args) {
        this.input = input;
        this.args = args;
    }
}
