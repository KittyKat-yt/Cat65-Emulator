package org.kittykat.cat65.ui.window;

import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.core.CMU;
import org.kittykat.cat65.settings.NewLineVariant;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SerialTerminal extends WindowWithTitle {
    private final TextArea terminal = new TextArea();
    private final ConcurrentLinkedQueue<Character> printQueue = new ConcurrentLinkedQueue<>();

    public SerialTerminal() {
        super("Serial Terminal");

        terminal.setId("terminal");
        terminal.getStyleClass().add("screen");
        terminal.setEditable(false);
        terminal.setWrapText(true);
        terminal.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            char chr = e.getCharacter().charAt(0);
            if (chr == 0x0d) {
                CMU.receiveChar(switch (CMU.getNewLineVariant().get()) {
                    case CR, CRLF -> '\r';
                    case LF       -> '\n';
                }, false);
                if (CMU.getNewLineVariant().get() == NewLineVariant.CRLF) {
                    CMU.receiveChar('\n', true);
                }
            } else {
                CMU.receiveChar(chr, false);
            }
            e.consume();
        });
        VBox.setVgrow(terminal, Priority.ALWAYS);
        this.getChildren().add(terminal);
    }

    public void print(char chr) {
        printQueue.add(chr);
    }
    @Override
    public void updateWindow() {
        while (!printQueue.isEmpty()) {
            char c = printQueue.poll();

            if ((c == '\r') || (c == '\n')) {
                if (CMU.getNewLineVariant().get() == NewLineVariant.CRLF) {
                    terminal.appendText(String.valueOf(c));
                } else {
                    terminal.appendText("\r\n");
                }
            } else if (c == '\f') {
                terminal.clear();
            } else if ((c == 0x08) || (c == 0x7f)) {
                int len = terminal.getLength();
                if (len > 0) {
                    terminal.deleteText(len - 1, len);
                }
            } else {
                terminal.appendText(String.valueOf(c));
            }
        }
    }
    public void clear() {
        terminal.clear();
    }
}
