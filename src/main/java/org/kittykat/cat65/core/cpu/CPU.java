package org.kittykat.cat65.core.cpu;

import org.kittykat.cat65.EmuHelper;

public class CPU {
    private final OpcodeType[] opcodeTypes = {
            new OpcodeType(this::ADC, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm,  2, new int[]{0x69}, null),
                    new OpcodeDef(this::value_zp,   3, new int[]{0x65}, null),
                    new OpcodeDef(this::value_zpX,  4, new int[]{0x75}, null),
                    new OpcodeDef(this::value_abs,  4, new int[]{0x6d}, null),
                    new OpcodeDef(this::value_absX, 4, new int[]{0x7d}, null),
                    new OpcodeDef(this::value_absY, 4, new int[]{0x79}, null),
                    new OpcodeDef(this::value_xInd, 6, new int[]{0x61}, null),
                    new OpcodeDef(this::value_indY, 5, new int[]{0x71}, null),

                    new OpcodeDef(this::value_zpInd, 5, new int[]{0x72}, null),
            }),
            new OpcodeType(this::AND, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm,  2, new int[]{0x29}, null),
                    new OpcodeDef(this::value_zp,   3, new int[]{0x25}, null),
                    new OpcodeDef(this::value_zpX,  4, new int[]{0x35}, null),
                    new OpcodeDef(this::value_abs,  4, new int[]{0x2d}, null),
                    new OpcodeDef(this::value_absX, 4, new int[]{0x3d}, null),
                    new OpcodeDef(this::value_absY, 4, new int[]{0x39}, null),
                    new OpcodeDef(this::value_xInd, 6, new int[]{0x21}, null),
                    new OpcodeDef(this::value_indY, 5, new int[]{0x31}, null),

                    new OpcodeDef(this::value_zpInd, 5, new int[]{0x32}, null),
            }),
            new OpcodeType(this::ASL, new OpcodeDef[] {
                    new OpcodeDef(null,         2, new int[]{0x0a}, null),
                    new OpcodeDef(this::address_zp,   5, new int[]{0x06}, null),
                    new OpcodeDef(this::address_zpX,  6, new int[]{0x16}, null),
                    new OpcodeDef(this::address_abs,  6, new int[]{0x0e}, null),
                    new OpcodeDef(this::address_absX, 6, new int[]{0x1e}, null),
            }),
            new OpcodeType(this::B__, new OpcodeDef[] {
                    new OpcodeDef(null, 2, new int[]{0x10}, "NC"),  // BPL
                    new OpcodeDef(null, 2, new int[]{0x30}, "NS"),  // BMI
                    new OpcodeDef(null, 2, new int[]{0x50}, "VC"),  // BVC
                    new OpcodeDef(null, 2, new int[]{0x70}, "VS"),  // BVS
                    new OpcodeDef(null, 2, new int[]{0x90}, "CC"),  // BCC
                    new OpcodeDef(null, 2, new int[]{0xb0}, "CS"),  // BCS
                    new OpcodeDef(null, 2, new int[]{0xd0}, "ZC"),  // BNE
                    new OpcodeDef(null, 2, new int[]{0xf0}, "ZS"),  // BEQ
            }),
            new OpcodeType(this::BIT, new OpcodeDef[] {
                    new OpcodeDef(this::value_zp,  3, new int[]{0x24}, null),
                    new OpcodeDef(this::value_abs, 4, new int[]{0x2c}, null),

                    new OpcodeDef(this::value_imm,  2, new int[]{0x89}, "#"),
                    new OpcodeDef(this::value_absX, 4, new int[]{0x3c}, null),
                    new OpcodeDef(this::value_zpX,  4, new int[]{0x34}, null),
            }),
            new OpcodeType(this::BRK, new OpcodeDef[] {
                    new OpcodeDef(null, 7, new int[]{0x00}, null),
            }),
            new OpcodeType(this::CMP, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm,  2, new int[]{0xc9}, null),
                    new OpcodeDef(this::value_zp,   3, new int[]{0xc5}, null),
                    new OpcodeDef(this::value_zpX,  4, new int[]{0xd5}, null),
                    new OpcodeDef(this::value_abs,  4, new int[]{0xcd}, null),
                    new OpcodeDef(this::value_absX, 4, new int[]{0xdd}, null),
                    new OpcodeDef(this::value_absY, 4, new int[]{0xd9}, null),
                    new OpcodeDef(this::value_xInd, 6, new int[]{0xc1}, null),
                    new OpcodeDef(this::value_indY, 5, new int[]{0xd1}, null),

                    new OpcodeDef(this::value_zpInd, 5, new int[]{0xd2}, null),
            }),
            new OpcodeType(this::CPX, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm, 2, new int[]{0xe0}, null),
                    new OpcodeDef(this::value_zp,  3, new int[]{0xe4}, null),
                    new OpcodeDef(this::value_abs, 4, new int[]{0xec}, null),
            }),
            new OpcodeType(this::CPY, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm, 2, new int[]{0xc0}, null),
                    new OpcodeDef(this::value_zp,  3, new int[]{0xc4}, null),
                    new OpcodeDef(this::value_abs, 4, new int[]{0xcc}, null),
            }),
            new OpcodeType(this::DEC, new OpcodeDef[] {
                    new OpcodeDef(this::address_zp,         5, new int[]{0xc6}, null),
                    new OpcodeDef(this::address_zpX,        6, new int[]{0xd6}, null),
                    new OpcodeDef(this::address_abs,        6, new int[]{0xce}, null),
                    new OpcodeDef(this::address_absX_fixed, 7, new int[]{0xde}, null),
                    // for some reason DEC abs,x isn't like the other RMW instructions and has a fixed cycle count

                    new OpcodeDef(null, 2, new int[]{0x3a}, null),
            }),
            new OpcodeType(this::DEX, new OpcodeDef[] {
                    new OpcodeDef(null, 2, new int[]{0xca}, null),
            }),
            new OpcodeType(this::DEY, new OpcodeDef[] {
                    new OpcodeDef(null, 2, new int[]{0x88}, null),
            }),
            new OpcodeType(this::EOR, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm,  2, new int[]{0x49}, null),
                    new OpcodeDef(this::value_zp,   3, new int[]{0x45}, null),
                    new OpcodeDef(this::value_zpX,  4, new int[]{0x55}, null),
                    new OpcodeDef(this::value_abs,  4, new int[]{0x4d}, null),
                    new OpcodeDef(this::value_absX, 4, new int[]{0x5d}, null),
                    new OpcodeDef(this::value_absY, 4, new int[]{0x59}, null),
                    new OpcodeDef(this::value_xInd, 6, new int[]{0x41}, null),
                    new OpcodeDef(this::value_indY, 5, new int[]{0x51}, null),

                    new OpcodeDef(this::value_zpInd, 5, new int[]{0x52}, null),
            }),
            new OpcodeType(this::SE_, new OpcodeDef[] {
                    new OpcodeDef(null, 2, new int[]{0x38}, "C"),  // SEC
                    new OpcodeDef(null, 2, new int[]{0x78}, "I"),  // SEI
                    new OpcodeDef(null, 2, new int[]{0xf8}, "D"),  // SED
            }),
            new OpcodeType(this::CL_, new OpcodeDef[] {
                    new OpcodeDef(null, 2, new int[]{0x18}, "C"),  // CLC
                    new OpcodeDef(null, 2, new int[]{0x58}, "I"),  // CLI
                    new OpcodeDef(null, 2, new int[]{0xb8}, "V"),  // CLV
                    new OpcodeDef(null, 2, new int[]{0xd8}, "D"),  // CLD
            }),
            new OpcodeType(this::INC, new OpcodeDef[] {
                    new OpcodeDef(this::address_zp,         5, new int[]{0xe6}, null),
                    new OpcodeDef(this::address_zpX,        6, new int[]{0xf6}, null),
                    new OpcodeDef(this::address_abs,        6, new int[]{0xee}, null),
                    new OpcodeDef(this::address_absX_fixed, 7, new int[]{0xfe}, null),
                    // for some reason INC abs,x isn't like the other RMW instructions and has a fixed cycle count

                    new OpcodeDef(null, 2, new int[]{0x1a}, null),
            }),
            new OpcodeType(this::INX, new OpcodeDef[] {
                    new OpcodeDef(null, 2, new int[]{0xe8}, null),
            }),
            new OpcodeType(this::INY, new OpcodeDef[] {
                    new OpcodeDef(null, 2, new int[]{0xc8}, null),
            }),
            new OpcodeType(this::JMP, new OpcodeDef[] {
                    new OpcodeDef(this::address_abs, 3, new int[]{0x4c}, null),
                    new OpcodeDef(this::address_ind, 6, new int[]{0x6c}, null),

                    new OpcodeDef(this::address_absXInd, 6, new int[]{0x7c}, null),
            }),
            new OpcodeType(this::JSR, new OpcodeDef[] {
                    new OpcodeDef(this::address_abs, 6, new int[]{0x20}, null),
            }),
            new OpcodeType(this::LDA, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm,  2, new int[]{0xa9}, null),
                    new OpcodeDef(this::value_zp,   3, new int[]{0xa5}, null),
                    new OpcodeDef(this::value_zpX,  4, new int[]{0xb5}, null),
                    new OpcodeDef(this::value_abs,  4, new int[]{0xad}, null),
                    new OpcodeDef(this::value_absX, 4, new int[]{0xbd}, null),
                    new OpcodeDef(this::value_absY, 4, new int[]{0xb9}, null),
                    new OpcodeDef(this::value_xInd, 6, new int[]{0xa1}, null),
                    new OpcodeDef(this::value_indY, 5, new int[]{0xb1}, null),

                    new OpcodeDef(this::value_zpInd, 5, new int[]{0xb2}, null),
            }),
            new OpcodeType(this::LDX, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm,  2, new int[]{0xa2}, null),
                    new OpcodeDef(this::value_zp,   3, new int[]{0xa6}, null),
                    new OpcodeDef(this::value_zpY,  4, new int[]{0xb6}, null),
                    new OpcodeDef(this::value_abs,  4, new int[]{0xae}, null),
                    new OpcodeDef(this::value_absY, 4, new int[]{0xbe}, null),
            }),
            new OpcodeType(this::LDY, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm,  2, new int[]{0xa0}, null),
                    new OpcodeDef(this::value_zp,   3, new int[]{0xa4}, null),
                    new OpcodeDef(this::value_zpX,  4, new int[]{0xb4}, null),
                    new OpcodeDef(this::value_abs,  4, new int[]{0xac}, null),
                    new OpcodeDef(this::value_absX, 4, new int[]{0xbc}, null),
            }),
            new OpcodeType(this::LSR, new OpcodeDef[] {
                    new OpcodeDef(null,         2, new int[]{0x4a}, null),
                    new OpcodeDef(this::address_zp,   5, new int[]{0x46}, null),
                    new OpcodeDef(this::address_zpX,  6, new int[]{0x56}, null),
                    new OpcodeDef(this::address_abs,  6, new int[]{0x4e}, null),
                    new OpcodeDef(this::address_absX, 6, new int[]{0x5e}, null),
            }),
            new OpcodeType(this::NOP, new OpcodeDef[] {
                    new OpcodeDef(null,     1, new int[]{0x03, 0x13, 0x23, 0x33, 0x43, 0x53, 0x63, 0x73,
                                                                        0x83, 0x93, 0xa3, 0xb3, 0xc3, 0xd3, 0xe3, 0xf3,
                                                                        0x0b, 0x1b, 0x2b, 0x3b, 0x4b, 0x5b, 0x6b, 0x7b,
                                                                        0x8b, 0x9b, 0xab, 0xbb, 0xeb, 0xfb}, "-"),
                    new OpcodeDef(null,     2, new int[]{0xea}, "-"),
                    new OpcodeDef(this::nextByte, 2, new int[]{0x02, 0x22, 0x42, 0x62, 0x82, 0xc2, 0xe2}, null),
                    new OpcodeDef(this::nextByte, 3, new int[]{0x44}, null),
                    new OpcodeDef(this::nextByte, 4, new int[]{0x54, 0xd4, 0xf4}, null),
                    new OpcodeDef(this::nextWord, 4, new int[]{0xdc, 0xfc}, null),
                    new OpcodeDef(this::nextWord, 8, new int[]{0x5c}, null),
            }),
            new OpcodeType(this::ORA, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm,  2, new int[]{0x09}, null),
                    new OpcodeDef(this::value_zp,   3, new int[]{0x05}, null),
                    new OpcodeDef(this::value_zpX,  4, new int[]{0x15}, null),
                    new OpcodeDef(this::value_abs,  4, new int[]{0x0d}, null),
                    new OpcodeDef(this::value_absX, 4, new int[]{0x1d}, null),
                    new OpcodeDef(this::value_absY, 4, new int[]{0x19}, null),
                    new OpcodeDef(this::value_xInd, 6, new int[]{0x01}, null),
                    new OpcodeDef(this::value_indY, 5, new int[]{0x11}, null),

                    new OpcodeDef(this::value_zpInd, 5, new int[]{0x12}, null),
            }),
            new OpcodeType(this::PH_, new OpcodeDef[] {
                    new OpcodeDef(null, 3, new int[]{0x48}, "A"),  // PHA
                    new OpcodeDef(null, 3, new int[]{0x08}, "P"),  // PHP

                    new OpcodeDef(null, 3, new int[]{0xda}, "X"),  // PHX
                    new OpcodeDef(null, 3, new int[]{0x5a}, "Y"),  // PHY
            }),
            new OpcodeType(this::PL_, new OpcodeDef[] {
                    new OpcodeDef(null, 4, new int[]{0x68}, "A"),  // PLA
                    new OpcodeDef(null, 4, new int[]{0x28}, "P"),  // PLP

                    new OpcodeDef(null, 4, new int[]{0xfa}, "X"),  // PLX
                    new OpcodeDef(null, 4, new int[]{0x7a}, "Y"),  // PLY
            }),
            new OpcodeType(this::T__, new OpcodeDef[] {
                    new OpcodeDef(null, 2, new int[]{0xaa}, "AX"),  // TAX
                    new OpcodeDef(null, 2, new int[]{0x8a}, "XA"),  // TXA
                    new OpcodeDef(null, 2, new int[]{0xa8}, "AY"),  // TAY
                    new OpcodeDef(null, 2, new int[]{0x98}, "YA"),  // TYA
                    new OpcodeDef(null, 2, new int[]{0x9a}, "XS"),  // TXS
                    new OpcodeDef(null, 2, new int[]{0xba}, "SX"),  // TSX
            }),
            new OpcodeType(this::ROL, new OpcodeDef[] {
                    new OpcodeDef(null,         2, new int[]{0x2a}, null),
                    new OpcodeDef(this::address_zp,   5, new int[]{0x26}, null),
                    new OpcodeDef(this::address_zpX,  6, new int[]{0x36}, null),
                    new OpcodeDef(this::address_abs,  6, new int[]{0x2e}, null),
                    new OpcodeDef(this::address_absX, 6, new int[]{0x3e}, null),
            }),
            new OpcodeType(this::ROR, new OpcodeDef[] {
                    new OpcodeDef(null,         2, new int[]{0x6a}, null),
                    new OpcodeDef(this::address_zp,   5, new int[]{0x66}, null),
                    new OpcodeDef(this::address_zpX,  6, new int[]{0x76}, null),
                    new OpcodeDef(this::address_abs,  6, new int[]{0x6e}, null),
                    new OpcodeDef(this::address_absX, 6, new int[]{0x7e}, null),
            }),
            new OpcodeType(this::RTI, new OpcodeDef[] {
                    new OpcodeDef(null, 6, new int[]{0x40}, null),
            }),
            new OpcodeType(this::RTS, new OpcodeDef[] {
                    new OpcodeDef(null, 6, new int[]{0x60}, null),
            }),
            new OpcodeType(this::SBC, new OpcodeDef[] {
                    new OpcodeDef(this::value_imm,  2, new int[]{0xe9}, null),
                    new OpcodeDef(this::value_zp,   3, new int[]{0xe5}, null),
                    new OpcodeDef(this::value_zpX,  4, new int[]{0xf5}, null),
                    new OpcodeDef(this::value_abs,  4, new int[]{0xed}, null),
                    new OpcodeDef(this::value_absX, 4, new int[]{0xfd}, null),
                    new OpcodeDef(this::value_absY, 4, new int[]{0xf9}, null),
                    new OpcodeDef(this::value_xInd, 6, new int[]{0xe1}, null),
                    new OpcodeDef(this::value_indY, 5, new int[]{0xf1}, null),

                    new OpcodeDef(this::value_zpInd, 5, new int[]{0xf2}, null),
            }),
            new OpcodeType(this::STA, new OpcodeDef[] {
                    new OpcodeDef(this::address_zp,         3, new int[]{0x85}, null),
                    new OpcodeDef(this::address_zpX,        4, new int[]{0x95}, null),
                    new OpcodeDef(this::address_abs,        4, new int[]{0x8d}, null),
                    new OpcodeDef(this::address_absX_fixed, 5, new int[]{0x9d}, null),
                    new OpcodeDef(this::address_absY_fixed, 5, new int[]{0x99}, null),
                    new OpcodeDef(this::address_xInd,       6, new int[]{0x81}, null),
                    new OpcodeDef(this::address_indY_fixed, 6, new int[]{0x91}, null),

                    new OpcodeDef(this::address_zpInd, 5, new int[]{0x92}, null),
            }),
            new OpcodeType(this::STX, new OpcodeDef[] {
                    new OpcodeDef(this::address_zp,  3, new int[]{0x86}, null),
                    new OpcodeDef(this::address_zpY, 4, new int[]{0x96}, null),
                    new OpcodeDef(this::address_abs, 4, new int[]{0x8e}, null),
            }),
            new OpcodeType(this::STY, new OpcodeDef[] {
                    new OpcodeDef(this::address_zp,  3, new int[]{0x84}, null),
                    new OpcodeDef(this::address_zpX, 4, new int[]{0x94}, null),
                    new OpcodeDef(this::address_abs, 4, new int[]{0x8c}, null),
            }),

            // 65c02 exclusive instructions
            new OpcodeType(this::BBR_, new OpcodeDef[] {
                    new OpcodeDef(this::value_zp, 5, new int[]{0x0f}, "0"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0x1f}, "1"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0x2f}, "2"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0x3f}, "3"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0x4f}, "4"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0x5f}, "5"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0x6f}, "6"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0x7f}, "7"),
            }),
            new OpcodeType(this::BBS_, new OpcodeDef[] {
                    new OpcodeDef(this::value_zp, 5, new int[]{0x8f}, "0"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0x9f}, "1"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0xaf}, "2"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0xbf}, "3"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0xcf}, "4"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0xdf}, "5"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0xef}, "6"),
                    new OpcodeDef(this::value_zp, 5, new int[]{0xff}, "7"),
            }),
            new OpcodeType(this::BRA, new OpcodeDef[] {
                    new OpcodeDef(null, 2, new int[]{0x80}, null),
            }),
            new OpcodeType(this::RMB_, new OpcodeDef[] {
                    new OpcodeDef(this::address_zp, 5, new int[]{0x07}, "0"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0x17}, "1"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0x27}, "2"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0x37}, "3"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0x47}, "4"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0x57}, "5"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0x67}, "6"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0x77}, "7"),
            }),
            new OpcodeType(this::SMB_, new OpcodeDef[] {
                    new OpcodeDef(this::address_zp, 5, new int[]{0x87}, "0"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0x97}, "1"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0xa7}, "2"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0xb7}, "3"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0xc7}, "4"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0xd7}, "5"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0xe7}, "6"),
                    new OpcodeDef(this::address_zp, 5, new int[]{0xf7}, "7"),
            }),
            new OpcodeType(this::STP, new OpcodeDef[] {
                    new OpcodeDef(null, 3, new int[]{0xdb}, null),
            }),
            new OpcodeType(this::STZ, new OpcodeDef[]{
                    new OpcodeDef(this::address_zp,         3, new int[]{0x64}, null),
                    new OpcodeDef(this::address_zpX,        4, new int[]{0x74}, null),
                    new OpcodeDef(this::address_abs,        4, new int[]{0x9c}, null),
                    new OpcodeDef(this::address_absX_fixed, 5, new int[]{0x9e}, null),
            }),
            new OpcodeType(this::TRB, new OpcodeDef[]{
                    new OpcodeDef(this::address_zp,   5, new int[]{0x14}, null),
                    new OpcodeDef(this::address_abs,  6, new int[]{0x1c}, null),
            }),
            new OpcodeType(this::TSB, new OpcodeDef[]{
                    new OpcodeDef(this::address_zp,   5, new int[]{0x04}, null),
                    new OpcodeDef(this::address_abs,  6, new int[]{0x0c}, null),
            }),
            new OpcodeType(this::WAI, new OpcodeDef[] {
                    new OpcodeDef(null, 3, new int[]{0xcb}, null),
            }),
    };
    private final Opcode[] opcodes = new Opcode[0x100];

    public static final int[] VECTOR_ADDRESSES = {
            0xfffc,  // Reset
            0xfffa,  // NMI
            0xfffe,  // IRQ/BRK
    };
    private static final int MAGIC = 0xee;

    private int A  = 0x00;
    private int X  = 0x00;
    private int Y  = 0x00;
    private int S  = 0x00;
    private int P  = 0x00;
    private int PC = 0x0000;

    private int     cycles = 0;
    private boolean wait   = false;
    private boolean stop   = false;
    private boolean jam    = false;  // left over from the original 6502 emulator

    private final Bus bus;

    public CPU(Bus bus) {
        makeOpcodes();
        this.bus = bus;
    }
    private void makeOpcodes() {
        for (OpcodeType type : opcodeTypes) {
            for (OpcodeDef def : type.opcodeDefinitions()) {
                Opcode opcode = new Opcode(type.method(), def.input(), def.args(), def.cycles());
                for (int b : def.opcodes()) {
                    if (opcodes[b] != null) throw new OpcodeException(b, opcodes[b], opcode);
                    opcodes[b] = opcode;
                }
            }
        }
        for (int o = 0; o < opcodes.length; o++) {
            if (opcodes[o] == null) throw new OpcodeException(o);
        }
    }

    public void clock() {
        if (cycles <= 0) {
            if (!stop) {
                boolean irq = !bus.pollIRQ();
                //           ^^^ IRQs are active-low
                if (irq) {
                    wait = false;
                }

                if (bus.pollNMI()) {
                    // NMIs have priority
                    wait = false;
                    hardwareInterrupt(1);
                } else if (irq & (!getFlag('I'))) {
                    hardwareInterrupt(2);
                } else if (!wait) {
                    int opcode = nextByte();
                    Opcode op = opcodes[opcode];
                    cycles = op.cycles;
                    op.execute();
                }
            }
        }
        if (cycles > 0) {
            cycles--;
        }
    }
    private void hardwareInterrupt(int i) {
        cycles = 7;
        stackPushWord(PC);
        stackPush(P | 0b0010_0000);  // bit 5 gets pushed as 1 by IRQs and NMIs

        // IRQs and NMIs on the 65c02 set the interrupt disable flag and clear the decimal flag
        setFlag('I');
        clearFlag('D');

        PC = vectorAddress(i);
    }
    public void reset() {
        wait = false;
        stop = false;
        jam  = false;
        cycles = 7;

        nextByte();  // left over BRK read (would read the opcode)
        nextByte();  // dummy read from BRK

        // suppressed stack writes (converted to reads)
        read(0x0100 | S);
        S = (S - 1) & 0xff;
        read(0x0100 | S);
        S = (S - 1) & 0xff;
        read(0x0100 | S);
        S = (S - 1) & 0xff;

        // Status Flags get reset too
        P = (P & 0b1100_0011) | 0b0000_0100;

        // read $fffc and $fffd to get the PC
        PC = vectorAddress(0);
    }

    public int getFlagMask(char flag) {
        return switch (flag) {
            case 'N' -> 0x80;
            case 'V' -> 0x40;
            case 'D' -> 0x08;
            case 'I' -> 0x04;
            case 'Z' -> 0x02;
            case 'C' -> 0x01;
            default  -> 0x00;
        };
    }
    public boolean getFlag(char flag) {
        int mask = getFlagMask(flag);
        return (P & mask) != 0;
    }
    private void clearFlag(char flag) {
        int mask = getFlagMask(flag);
        P &= (mask ^ 0xff);
    }
    private void setFlag(char flag) {
        int mask = getFlagMask(flag);
        P |= mask;
    }
    private void writeFlag(char flag, boolean v) {
        if (v) {
            setFlag(flag);
        } else {
            clearFlag(flag);
        }
    }
    private void flagsZN(int v) {
        writeFlag('Z', v == 0x00);
        writeFlag('N', (v & 0x80) != 0);
    }

    private int bitMask(int i) {
        return 1 << i;
    }

    private int  read(int address) {
        return bus.read(address);
    }
    private void write(int address, int value) {
        bus.write(address, value);
    }

    private void stackPush(int v) {
        write(0x0100 | S, v);
        S = (S - 1) & 0xff;
    }
    private void stackPushWord(int v) {
        stackPush(v >> 8);
        stackPush(v & 0x00ff);
    }
    private int stackPull() {
        S = (S + 1) & 0xff;
        return read(0x0100 | S);
    }
    private int stackPullWord() {
        return stackPull() | (stackPull() << 8);
    }

    private void dummyRead() {
        read(PC);
    }
    private int nextByte() {
        int v = read(PC);
        PC = (PC + 1) & 0xffff;
        return v;
    }
    private int nextWord() {
        int low = nextByte();
        int high = nextByte();
        return ((high << 8) | low);
    }
    private int vectorAddress(int i) {
        int low  = read(VECTOR_ADDRESSES[i]);
        int high = read(VECTOR_ADDRESSES[i] + 1);
        return ((high << 8) | low);
    }
    private int indexedAddress(int address, int i, boolean fixedCycles) {
        int a = (address + i) & 0xffff;

        boolean pageCrossed = (address & 0xff00) != (a & 0xff00);  // check if a page-boundary is crossed
        if (pageCrossed || fixedCycles) {  // the dummy read also happens on instructions that always have this extra cycle
            // on the 65c02, indexed addressing across a page boundary causes it to read
            // the last instruction byte again instead of reading from an invalid address
            read((PC - 1) & 0xffff);
        }
        if (pageCrossed && !fixedCycles) cycles++;

        return a;
    }

    private int address_zp() {
        return nextByte();
    }
    private int address_zpXY(int i) {
        return (nextByte() + i) & 0xff;
    }
    private int address_zpX() {
        return address_zpXY(X);
    }
    private int address_zpY() {
        return address_zpXY(Y);
    }
    private int address_zpInd() {
        int a = nextByte();
        return read(a) | (read((a + 1) & 0xff) << 8);
    }
    private int address_abs() {
        return nextWord();
    }
    private int address_absXY(int i) {
        return indexedAddress(nextWord(), i, false);
    }
    private int address_absX() {
        return address_absXY(X);
    }
    private int address_absX_fixed() {
        return indexedAddress(nextWord(), X, true);
    }
    private int address_absY() {
        return address_absXY(Y);
    }
    private int address_absY_fixed() {
        return indexedAddress(nextWord(), Y, true);
    }
    private int address_ind() {
        int a = nextWord();
        return (read(a) | (read((a + 1) & 0xffff) << 8));
    }
    private int address_xInd() {
        int a = (nextByte() + X) & 0xff;
        return (read(a) | (read((a + 1) & 0xff) << 8));
    }
    private int address_indY() {
        int a = nextByte();
        return indexedAddress(read(a) | (read((a + 1) & 0xff) << 8), Y, false);
    }
    private int address_indY_fixed() {
        int a = nextByte();
        return indexedAddress(read(a) | (read((a + 1) & 0xff) << 8), Y, true);
    }
    private int address_absXInd() {
        int a = (nextWord() + X) & 0xffff;
        return read(a) | (read((a + 1) & 0xffff) << 8);
    }

    private int value_imm() {
        return nextByte();
    }
    private int value_zp() {
        return read(address_zp());
    }
    private int value_zpX() {
        return read(address_zpXY(X));
    }
    private int value_zpY() {
        return read(address_zpXY(Y));
    }
    private int value_zpInd() {
        return read(address_zpInd());
    }
    private int value_abs() {
        return read(address_abs());
    }
    private int value_absX() {
        return read(address_absXY(X));
    }
    private int value_absY() {
        return read(address_absXY(Y));
    }
    private int value_ind() {
        return read(address_ind());
    }
    private int value_xInd() {
        return read(address_xInd());
    }
    private int value_indY() {
        return read(address_indY());
    }
    private int value_absXInd() {
        return read(address_absXInd());
    }

    private boolean bitBranchValue(int v, OpcodeContext c) {
        return (v & bitMask(c.args.charAt(0) - '0')) != 0;
    }
    private void branch(int offsetByte) {
        dummyRead();  // the processor always fetches the byte following a branch instruction even if the branch is taken
        cycles++;
        int original = PC;
        PC = (PC + EmuHelper.fromTwosComp(offsetByte)) & 0xffff;
        if ((PC & 0xff00) != (original & 0xff00)) {  // check if a page-boundary is crossed
            // it first reads the byte at the address with the correct low-byte but incorrect high-byte before fixing the high-byte
            read((original & 0xff00) | (PC & 0x00ff));
            cycles++;
        }
    }
    private void compare(int reg, int v) {
        int r = (reg - v) & 0xff;
        flagsZN(r);
        writeFlag('C', v <= reg);
    }

    private void ADC(OpcodeContext c) {
        int carry = EmuHelper.boolBit(getFlag('C'));
        int v1 = A;
        int b = v1 + c.input + carry;
        if (getFlag('D')) {  // decimal mode
            int d1 = EmuHelper.fromBCD(v1);
            int d2 = EmuHelper.fromBCD(c.input);
            int r = d1 + d2 + carry;
            A = EmuHelper.toBCD(r % 100) & 0xff;
            writeFlag('C', r > 99);
            
            cycles++;  // on the 65c02 BCD math takes an extra cycle
        } else {
            A = b & 0xff;
            writeFlag('C', b > 0xff);
        }
        flagsZN(A);
        writeFlag('V', (~(v1 ^ c.input) & (v1 ^ b) & 0x80) != 0);
    }
    private void AND(OpcodeContext c) {
        A &= c.input;
        flagsZN(A);
    }
    private void ASL(OpcodeContext c) {
        int v;
        if (c.hasInput) {
            v = read(c.input) << 1;
            read(c.input);  // RMW dummy read
            write(c.input, v);
        } else {
            dummyRead();  // dummy read (implied instruction)
            v = A << 1;
            A = v & 0xff;
        }
        writeFlag('C', v > 0xff);
        flagsZN(v & 0xff);
    }
    private void B__(OpcodeContext c) {
        // branch instructions
        int d = value_imm();
        if (getFlag(c.args.charAt(0)) == (c.args.charAt(1) == 'S')) branch(d);
    }
    private void BIT(OpcodeContext c) {
        writeFlag('Z', (c.input & A)    == 0);
        if (c.args == null) {
            // non-immediate variants only
            writeFlag('N', (c.input & 0x80) != 0);
            writeFlag('V', (c.input & 0x40) != 0);
        }
    }
    private void BRK(OpcodeContext c) {
        nextByte();  // dummy read (implied instruction)

        stackPushWord(PC);
        stackPush(P | 0b0011_0000);  // bit 5 and the B flag both get pushed as 1 by BRK

        // BRKs on the 65c02 set the interrupt disable flag and clear the decimal flag
        setFlag('I');
        clearFlag('D');

        PC = vectorAddress(2);
    }
    private void CL_(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)
        
        // clear flag instructions
        clearFlag(c.args.charAt(0));
    }
    private void CMP(OpcodeContext c) {
        compare(A, c.input);
    }
    private void CPX(OpcodeContext c) {
        compare(X, c.input);
    }
    private void CPY(OpcodeContext c) {
        compare(Y, c.input);
    }
    private void DEC(OpcodeContext c) {
        int v;
        if (c.hasInput) {
            v = (read(c.input) - 1) & 0xff;
            read(c.input);  // RMW dummy read
            write(c.input, v);
        } else {
            dummyRead();  // dummy read (implied instruction)
            A = v = (A - 1) & 0xff;
        }
        flagsZN(v);
    }
    private void DEX(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        X = (X - 1) & 0xff;
        flagsZN(X);
    }
    private void DEY(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        Y = (Y - 1) & 0xff;
        flagsZN(Y);
    }
    private void EOR(OpcodeContext c) {
        A ^= c.input;
        flagsZN(A);
    }
    private void INC(OpcodeContext c) {
        int v;
        if (c.hasInput) {
            v = (read(c.input) + 1) & 0xff;
            read(c.input);  // RMW dummy read
            write(c.input, v);
        } else {
            dummyRead();  // dummy read (implied instruction)
            A = v = (A + 1) & 0xff;
        }
        flagsZN(v);
    }
    private void INX(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        X = (X + 1) & 0xff;
        flagsZN(X);
    }
    private void INY(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        Y = (Y + 1) & 0xff;
        flagsZN(Y);
    }
    private void JMP(OpcodeContext c) {
        PC = c.input;
    }
    private void JSR(OpcodeContext c) {
        stackPushWord((PC - 1) & 0xffff);
        PC = c.input;
    }
    private void LDA(OpcodeContext c) {
        A = c.input;
        flagsZN(A);
    }
    private void LDX(OpcodeContext c) {
        X = c.input;
        flagsZN(X);
    }
    private void LDY(OpcodeContext c) {
        Y = c.input;
        flagsZN(Y);
    }
    private void LSR(OpcodeContext c) {
        int v;
        if (c.hasInput) {
            v = read(c.input);
            read(c.input);  // RMW dummy read
            writeFlag('C', (v & 0x01) != 0);
            v >>= 1;
            write(c.input, v);
        } else {
            dummyRead();  // dummy read (implied instruction)
            writeFlag('C', (A & 0x01) != 0);
            A >>= 1;
            v = A;
        }
        flagsZN(v);
    }
    private void NOP(OpcodeContext c) {
        if (c.args != null) {
            dummyRead();  // dummy read (implied instruction)
        }

        // wow :3c
        // such empty >w<
    }
    private void ORA(OpcodeContext c) {
        A |= c.input;
        flagsZN(A);
    }
    private void PH_(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        // stack push instructions
        char r = c.args.charAt(0);
        switch (r) {
            case 'A' -> stackPush(A);
            case 'X' -> stackPush(X);
            case 'Y' -> stackPush(Y);
            case 'P' -> stackPush(P | 0b0011_0000);  // bit 5 and the B flag both get pushed as 1 by PHP
        }
    }
    private void PL_(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        // stack pull instructions
        char r = c.args.charAt(0);
        switch (r) {
            case 'A' -> {
                A = stackPull();
                flagsZN(A);
            }
            case 'X' -> {
                X = stackPull();
                flagsZN(X);
            }
            case 'Y' -> {
                Y = stackPull();
                flagsZN(Y);
            }
            case 'P' -> P = (stackPull() & 0b1100_1111);  // bit 5 and the B flag get ignored by PLP
        }
    }
    private void T__(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        // register transfer instructions
        char s = c.args.charAt(0);
        char d = c.args.charAt(1);
        int v = switch (s) {
            case 'A' -> A;
            case 'X' -> X;
            case 'Y' -> Y;
            case 'S' -> S;
            default  -> 0x00;
        };
        switch (d) {
            case 'A' -> A = v;
            case 'X' -> X = v;
            case 'Y' -> Y = v;
            case 'S' -> S = v;
        }
        if (d != 'S') {
            flagsZN(v);
        }
    }
    private void ROL(OpcodeContext c) {
        int carry = (getFlag('C') ? 0x01 : 0x00);
        int o;
        int v;
        if (c.hasInput) {
            o = read(c.input);
            read(c.input);  // RMW dummy read
            v = ((o << 1) | carry) & 0xff;
            write(c.input, v);
        } else {
            dummyRead();  // dummy read (implied instruction)
            o = A;
            A = v = ((o << 1) | carry) & 0xff;
        }
        writeFlag('C', (o & 0x80) != 0);
        flagsZN(v);
    }
    private void ROR(OpcodeContext c) {
        int carry = (getFlag('C') ? 0x80 : 0x00);
        int o;
        int v;
        if (c.hasInput) {
            o = read(c.input);
            read(c.input);  // RMW dummy read
            v = ((o >> 1) | carry) & 0xff;
            write(c.input, v);
        } else {
            dummyRead();  // dummy read (implied instruction)
            o = A;
            A = v = ((o >> 1) | carry) & 0xff;
        }
        writeFlag('C', (o & 0x01) != 0);
        flagsZN(v);
    }
    private void RTI(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        P  = (stackPull() & 0b1100_1111);  // bit 5 and the B flag get ignored by RTI
        PC = stackPullWord();
    }
    private void RTS(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        PC = (stackPullWord() + 1) & 0xffff;
    }
    private void SE_(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)

        // set flag instructions
        setFlag(c.args.charAt(0));
    }
    private void SBC(OpcodeContext c) {
        int carry = EmuHelper.boolBit(!getFlag('C'));
        int v1 = A;
        int b = v1 - c.input - carry;
        if (getFlag('D')) {  // decimal mode
            int d1 = EmuHelper.fromBCD(v1);
            int d2 = EmuHelper.fromBCD(c.input);
            int r = d1 - d2 - carry;
            A = EmuHelper.toBCD(Math.floorMod(100 + r, 100)) & 0xff;

            cycles++;  // on the 65c02 BCD math takes an extra cycle
        } else {
            A = b & 0xff;
        }
        writeFlag('C', b >= 0x00);
        flagsZN(A);
        writeFlag('V', ((v1 ^ c.input) & (v1 ^ b) & 0x80) != 0);
    }
    private void STA(OpcodeContext c) {
        write(c.input, A);
    }
    private void STX(OpcodeContext c) {
        write(c.input, X);
    }
    private void STY(OpcodeContext c) {
        write(c.input, Y);
    }

    // WDC 65c02 exclusive instructions
    private void BBR_(OpcodeContext c) {
        // branch-on-bit-reset instructions
        int d = nextByte();
        if (!bitBranchValue(c.input, c)) branch(d);
    }
    private void BBS_(OpcodeContext c) {
        // branch-on-bit-set instructions
        int d = nextByte();
        if (bitBranchValue(c.input, c)) branch(d);
    }
    private void BRA(OpcodeContext c) {
        branch(value_imm());
    }
    private void RMB_(OpcodeContext c) {
        // reset-memory-bit instructions
        int v = read(c.input);
        v &= ~bitMask(c.args.charAt(0) - '0');
        write(c.input, v);
    }
    private void SMB_(OpcodeContext c) {
        // set-memory-bit instructions
        int v = read(c.input);
        v |= bitMask(c.args.charAt(0) - '0');
        write(c.input, v);
    }
    private void STP(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)
        stop = true;
    }
    private void STZ(OpcodeContext c) {
        write(c.input, 0x00);
    }
    private void TRB(OpcodeContext c) {
        int v = read(c.input);
        writeFlag('Z', (A & v) == 0);
        write(c.input, ~A & v);
    }
    private void TSB(OpcodeContext c) {
        int v = read(c.input);
        writeFlag('Z', (A & v) == 0);
        write(c.input, A | v);
    }
    private void WAI(OpcodeContext c) {
        dummyRead();  // dummy read (implied instruction)
        wait = true;
    }

    // original 6502 unintended instructions
    private void ALR(OpcodeContext c) {  // (ASR)
        AND(c);
        // to effectively LSR A
        c.hasInput = false;
        LSR(c);
    }
    private void ANC(OpcodeContext c) {  // (AAC)
        AND(c);
        writeFlag('C', getFlag('N'));
    }
    private void ANE(OpcodeContext c) {  // (XAA)
        A = (A | MAGIC) & X & c.input;
        flagsZN(A);
    }
    private void ARR(OpcodeContext c) {
        AND(c);
        // to effectively ROR A
        c.hasInput = false;
        ROR(c);
        boolean b = (A & 0x40) != 0;
        writeFlag('C', b);
        writeFlag('V', b ^ ((A & 0x20) != 0));
    }
    private void DCP(OpcodeContext c) {  // (DCM)
        DEC(c);
        c.input = read(c.input);
        CMP(c);
    }
    private void ISC(OpcodeContext c) {  // (ISB, INS)
        DEC(c);
        c.input = read(c.input);
        SBC(c);
    }
    private void LAS(OpcodeContext c) {  // (LAR, LAE)
        A = X = S = S & c.input;
        flagsZN(A);
    }
    private void LAX(OpcodeContext c) {
        A = X = c.input;
        flagsZN(A);
    }
    private void LXA(OpcodeContext c) {  // (ATX, OAL)
        AND(c);
        c.args = "AX";
        T__(c);
    }
    private void RLA(OpcodeContext c) {
        ROL(c);
        c.input = read(c.input);
        AND(c);
    }
    private void RRA(OpcodeContext c) {
        ROR(c);
        c.input = read(c.input);
        AND(c);
    }
    private void SAX(OpcodeContext c) {  // (AXS, AAX)
        int r = A & X;
        write(c.input, r);
    }
    private void SBX(OpcodeContext c) {  // (AXS, SAX)
        int o = A & X;
        X = (o - c.input) & 0xff;
        writeFlag('C', c.input <= o);
        flagsZN(X);
    }
    private void SHA(OpcodeContext c) {  // (AXA)
        int o = (c.input - Y) & 0xffff;
        int low = o & 0xff;
        int high = o >> 8;
        int a;
        if ((low + Y) > 0xff) {  // crossed page
            a = ((high & X) << 8) | low + Y;
        } else {
            a = (high << 8) | low + Y;
        }
        int v = A & X & (high + 1);
        write(a, v);
    }
    private void SHX(OpcodeContext c) {  // (SXA, XAS)
        int o = (c.input - Y) & 0xffff;
        int low = o & 0xff;
        int high = o >> 8;
        int a;
        if ((low + Y) > 0xff) {  // crossed page
            a = ((high & X) << 8) | low + Y;
        } else {
            a = (high << 8) | low + Y;
        }
        int v = X & (high + 1);
        write(a, v);
        // see SHA
    }
    private void SHY(OpcodeContext c) {  // (SYA, SAY)
        int o = (c.input - X) & 0xffff;
        int low = o & 0xff;
        int high = o >> 8;
        int a;
        if ((low + X) > 0xff) {  // crossed page
            a = ((high & Y) << 8) | low + X;
        } else {
            a = (high << 8) | low + X;
        }
        int v = Y & (high + 1);
        write(a, v);
        // see SHA
    }
    private void SLO(OpcodeContext c) {  // (ASO)
        ASL(c);
        c.input = read(c.input);
        ORA(c);
    }
    private void SRE(OpcodeContext c) {  // (LSE)
        LSR(c);
        c.input = read(c.input);
        EOR(c);
    }
    private void TAS(OpcodeContext c) {  // (XAS, SHS)
        S = A & X;  // first set the stack pointer
        // then write to memory using the new value of the stack pointer
        int o = (c.input - Y) & 0xffff;
        int low = o & 0xff;
        int high = o >> 8;
        int a;
        if ((low + Y) > 0xff) {  // crossed page
            a = ((high & S) << 8) | low + Y;
        } else {
            a = (high << 8) | low + Y;
        }
        int v = S & (high + 1);
        write(a, v);
    }
    private void JAM(OpcodeContext c) {  // (KIL, HLT)
        jam = true;
    }

    public int getA() {
        return A;
    }
    public int getX() {
        return X;
    }
    public int getY() {
        return Y;
    }
    public int getS() {
        return S;
    }
    public int getP() {
        return P;
    }
    public int getPC() {
        return PC;
    }

    public boolean isAtInstructionBoundary() {
        return cycles <= 0;
    }
}
