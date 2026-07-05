package org.kittykat.cat65.settings;

import javafx.util.StringConverter;

public enum ExpansionPort {
    _Disconnected,
    _2A05,
    _Vgc7;

    public static final StringConverter<ExpansionPort> stringConverter = new StringConverter<>() {
        @Override
        public String toString(ExpansionPort object) {
            return object.toString().substring(1);
        }
        @Override
        public ExpansionPort fromString(String string) {
            return ExpansionPort.valueOf("_" + string.strip());
        }
    };

    public static ExpansionPort fromID(byte id) {
        return switch (id) {
            case 0x2 -> _2A05;
            case 0x7 -> _Vgc7;
            default  -> _Disconnected;
        };
    }
}
