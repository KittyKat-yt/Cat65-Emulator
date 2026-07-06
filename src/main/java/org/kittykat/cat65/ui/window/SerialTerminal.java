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
    private final ConcurrentLinkedQueue<Character> inputQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Character> printQueue = new ConcurrentLinkedQueue<>();

    public SerialTerminal() {
        super("Serial Terminal");

        terminal.setId("terminal");
        terminal.getStyleClass().add("screen");
        terminal.setEditable(false);
        terminal.setWrapText(true);
        terminal.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            for (char chr : e.getCharacter().toCharArray()) {
                handleTypedChar(chr);
            }
            e.consume();
        });
        VBox.setVgrow(terminal, Priority.ALWAYS);
        getChildren().add(terminal);
    }
    private void handleTypedChar(char c) {
        if (c == 0x0d) {
            bufferChar(switch (CMU.getNewLineVariant().get()) {
                case CR, CRLF -> '\r';
                case LF       -> '\n';
            });
            if (CMU.getNewLineVariant().get() == NewLineVariant.CRLF) {
                bufferChar('\n');
            }
        } else {
            bufferChar(c);
        }
    }
    private void bufferChar(char c) {
        inputQueue.add(c);
    }

    public boolean isBufferEmpty() {
        return inputQueue.isEmpty();
    }
    @SuppressWarnings("DataFlowIssue")
    public char pollChar() {
        return inputQueue.poll();
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
