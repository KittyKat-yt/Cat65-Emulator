package org.kittykat.cat65.core;

import javafx.animation.AnimationTimer;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import org.kittykat.cat65.Cat65;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.cpu.Bus;
import org.kittykat.cat65.core.cpu.CPU;
import org.kittykat.cat65.core.expansionDevices.DisconnectedPort;
import org.kittykat.cat65.core.expansionDevices.ExpansionDevice;
import org.kittykat.cat65.core.expansionDevices.audio.AudioExpansion;
import org.kittykat.cat65.core.extraChips.ACIA;
import org.kittykat.cat65.core.extraChips.LCD;
import org.kittykat.cat65.core.extraChips.VIA;
import org.kittykat.cat65.ui.window.DebugWindow;
import org.kittykat.cat65.ui.window.HexView;
import org.kittykat.cat65.ui.window.SerialTerminal;
import org.kittykat.cat65.ui.window.ConfigWindow;
import org.kittykat.cat65.settings.ExpansionPort;
import org.kittykat.cat65.settings.NewLineVariant;
import org.kittykat.cat65.ui.window.core.CPUWindow;
import org.kittykat.cat65.ui.window.core.LCDWindow;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;

public abstract class CMU {
    public  static final int   READ_BUFFER_SIZE   = 512;
    public  static final int   SAMPLER_BUFFER_SIZE = 4 * READ_BUFFER_SIZE; // probably overkill >w<
    public  static final int   VISUAL_BUFFER_SIZE  = 2 * READ_BUFFER_SIZE;
    private static final float FILTER_ALPHA = .75f;

    private static final int WINDOW_ROWS = 4;
    private static final int WINDOW_COLS = 20;

    private static final int HEADER_SIZE = 16;
    private static final char[] ID_CHARS = {'c', '6', '5', 0x15};

    private static DebugWindow debugWindow;
    private static volatile int  debug_bufferFillSum   = 0;
    private static volatile int  debug_bufferFillCount = 0;
    public  static final float[] debug_audioBuffer = new float[VISUAL_BUFFER_SIZE];
    private static int debug_audioBufferSegment = 0;

    private static volatile int  debug_executedClocks            = 0;
    private static volatile long debug_lastClockSpeedSampleTime  = System.nanoTime();
    private static final ArrayList<Double> debug_lastClockSpeeds = new ArrayList<>();

    private static final  RingBuffer audioSamplerBuffer = new RingBuffer(SAMPLER_BUFFER_SIZE);
    private static double sampleTimeAccumulator = 0L;
    private static float  filteredSample = 0f;

    private static final Bus bus = new Bus() {
        @Override
        public int read(int address) {
            return CMU.get(address, true);
        }
        @Override
        public void write(int address, int value) {
            CMU.set(address, value, true);
        }

        @Override
        public boolean getIRQ() {
            return CMU.getIRQ();
        }
        @Override
        public boolean getNMI() {
            return CMU.getNMI();
        }
    };

    private static final int ROM_SIZE = 0x8000;
    private static final byte[] rom = new byte[ROM_SIZE];
    private static final byte[] ram = new byte[0x4000];

    private static volatile ExpansionDevice exPort04 = new DisconnectedPort(4);
    private static volatile ExpansionDevice exPort07 = new DisconnectedPort(7);
    private static final ACIA acia = new ACIA();
    private static final VIA  via  = new VIA();
    private static final LCD  lcd  = new LCD();
    private static final CPU  cpu  = new CPU(bus);

    private static ConfigWindow   config;
    private static SerialTerminal terminal;
    private static HexView   hexView;
    private static CPUWindow cpuWindow;
    private static LCDWindow lcdWindow;

    private static long timeSinceLastClock = System.nanoTime();

    public  static volatile boolean running = true;
    public  static volatile boolean paused  = true;
    public  static volatile boolean step    = false;
    public  static volatile boolean reset   = true;

    private static boolean showDebug = false;

    public static GridPane makeWindow() {
        debugWindow = new DebugWindow();
        config      = new ConfigWindow();
        terminal    = new SerialTerminal();
        cpuWindow   = new CPUWindow(cpu);
        lcdWindow   = new LCDWindow(lcd);
        hexView     = new HexView();

        GridPane window = EmuHelper.makeFreeGrid(WINDOW_ROWS, WINDOW_COLS, HPos.CENTER);
        window.add(config,     0, 0, 7, 1);
        window.add(lcdWindow,        7, 0, 6, 1);
        window.add(cpuWindow, 13, 0, 7, 1);
        window.add(hexView,   13, 1, 7, 3);
        addBigNode(window, terminal);
        addBigNode(window, debugWindow);
        debugWindow.setVisible(false);
        return window;
    }
    private static void addBigNode(GridPane window, Node node) {
        window.add(node, 0, 1, 13, 3);
    }

    public static void clearTerminal() {
        terminal.clear();
    }
    public static void terminalPrint(char c) {
        if (terminal != null) terminal.print(c);
    }
    public static boolean isSerialInputEmpty() {
        return (terminal == null) || (terminal.isBufferEmpty());
    }
    public static char pollSerialInput() {
        return terminal.pollChar();
    }

    public static ObjectProperty<NewLineVariant> getNewLineVariant() {
        return config.newLineVariant;
    }

    public static void stop() {
        exPort04.hideWindow();
        exPort07.hideWindow();
        running = false;
        System.exit(0);
    }

    public static void toggleDebugMode() {
        showDebug = !showDebug;
        terminal.setVisible(!showDebug);
        debugWindow.setVisible(showDebug);
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public static void startClockThread() {
        Thread clockThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            long timeAccumulator = 0L;
            long currentTime;
            long sleepTime;

            while (running) {
                currentTime = System.nanoTime();
                timeSinceLastClock = currentTime - lastTime;
                lastTime = currentTime;

                timeAccumulator += timeSinceLastClock;

                while (timeAccumulator >= config.clockPeriodNanos) {
                    if (!paused || step) {
                        if (reset) {
                            reset();
                            reset = false;
                        } else {
                            clock();
                        }
                        debug_executedClocks++;
                    }
                    timeAccumulator -= config.clockPeriodNanos;
                }

                sleepTime = config.clockPeriodNanos - timeAccumulator;
                if (sleepTime > 0L) {
                    LockSupport.parkNanos(sleepTime);
                }
            }
        }, "Cat65 Clock");
        clockThread.setDaemon(true);
        clockThread.start();
    }
    @SuppressWarnings({"NonAtomicOperationOnVolatileField", "CallToPrintStackTrace"})
    public static void startAudioThread() {
        Thread audioThread = new Thread(() -> {
            float[] samples;
            byte[] buffer = new byte[READ_BUFFER_SIZE * 2];

            int debug_bufferOffset;

            try {
                AudioFormat format = new AudioFormat(Cat65.SAMPLE_RATE, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format, buffer.length);
                line.start();

                while (running) {
                    int available = audioSamplerBuffer.length();
                    if (available > 0) {
                        samples = audioSamplerBuffer.readBuffer(READ_BUFFER_SIZE);
                    } else {
                        samples = new float[READ_BUFFER_SIZE];
                    }
                    debug_bufferFillSum += available;
                    debug_bufferFillCount++;

                    debug_bufferOffset = debug_audioBufferSegment * READ_BUFFER_SIZE;
                    for (int s = 0; s < READ_BUFFER_SIZE; s++) {
                        float sample = Math.max(-1f, Math.min(1f, samples[s]));
                        debug_audioBuffer[s + debug_bufferOffset] = sample;

                        short pcm = (short) (sample * 0x7fff);
                        buffer[2 * s]     = (byte) (pcm & 0xff);
                        buffer[2 * s + 1] = (byte) ((pcm >> 8) & 0xff);
                    }
                    debug_audioBufferSegment = (debug_audioBufferSegment + 1) % 2;
                    line.write(buffer, 0, buffer.length);
                }
                line.flush();
                line.close();
            } catch (LineUnavailableException e) {
                System.err.println("[!] Could not start audio thread...");
                e.printStackTrace();
            }
        }, "Cat65 Audio");
        audioThread.setDaemon(true);
        audioThread.start();
    }
    public static void startUiThread() {
        AnimationTimer UITimer = new AnimationTimer() {
            @SuppressWarnings("FieldCanBeLocal")
            private final long updateTime = (long) (1_000_000_000L / 93.75);
            private long lastUpdate = 0L;

            @Override
            public void handle(long now) {
                if ((now - lastUpdate) < updateTime) return;
                if (!running) stop();

                lastUpdate = now;
                cpuWindow.updateWindow();
                hexView.updateWindow();
                exPort04.updateWindow();
                exPort07.updateWindow();
                terminal.updateWindow();
                lcdWindow.updateWindow();
                debugWindow.updateWindow();
            }
        };
        UITimer.start();
    }

    public static void clock() {
        via.clock();
        acia.clock();
        exPort04.clock();
        exPort07.clock();
        clockAudio();
        cpu.clock();
        if (cpu.isAtInstructionBoundary()) step = false;
    }
    private static void clockAudio() {
        sampleTimeAccumulator += config.clockPeriodNanos;
        while (sampleTimeAccumulator >= Cat65.SAMPLE_PERIOD_NANOS) {
            float raw = 0f;

            for (int e = 0; e < 2; e++) {
                ExpansionDevice expansion = (e == 0) ? exPort04 : exPort07;
                if (expansion instanceof AudioExpansion audio) {
                    raw += audio.getAudioSample();
                }
            }
            filteredSample += FILTER_ALPHA * (raw - filteredSample);

            audioSamplerBuffer.write(filteredSample);
            sampleTimeAccumulator -= Cat65.SAMPLE_PERIOD_NANOS;
        }
    }

    public static void reset() {
        via.reset();
        acia.reset();
        exPort04.reset();
        exPort07.reset();
        cpu.reset();
        step = false;
    }

    public static void swapExpansion04(ExpansionPort ex) {
        exPort04.hideWindow();
        exPort04 = getDevice(ex, 4);
    }
    public static void swapExpansion07(ExpansionPort ex) {
        exPort07.hideWindow();
        exPort07 = getDevice(ex, 7);
    }
    private static ExpansionDevice getDevice(ExpansionPort io, int port) {
        return ExpansionDevice.fromEnum(io, port);
    }

    public static void openEx04Window() {
        exPort04.showWindow();
    }
    public static void openEx07Window() {
        exPort07.showWindow();
    }
    public static boolean doesEx04HaveNoWindow() {
        return exPort04.doesNotHaveWindow();
    }
    public static boolean doesEx07HaveNoWindow() {
        return exPort07.doesNotHaveWindow();
    }

    public static int getMDR() {
        return cpu.mdr;
    }

    public static int getPA(int pinState) {
        // ToDo: get PortA
        return 0x00;
    }
    public static void setPA(int pinState) {
        // ToDo: set PortA
    }
    /**
     * bits 0-3: Data
     **/
    public static int getPB(int pinState) {
        return lcd.get(pinState);
    }
    /**
     * bits 0-3: Data<br>
     * bit   4:  RS<br>
     * bit   5:  R/W<br>
     * bit   6:  E
     **/
    public static void setPB(int pinState) {
        lcd.set(pinState);
    }

    public static boolean getIRQ() {
        return via.getIRQ() && acia.getIRQ() && exPort04.getIRQ() && exPort07.getIRQ();
    }
    public static boolean getNMI() {
        return exPort04.getNMI() && exPort07.getNMI();
    }

    public static int get(int address, boolean cpu) {
        address &= 0xffff;
        int select = address >> 12;
        return 0xff & (switch (select) {
            case 0x0,0x1,0x2,0x3 -> ram[address];
            case 0x4             -> exPort04.read(address - 0x4000, cpu);
            case 0x5             -> acia.read(address - 0x5000, cpu);
            case 0x6             -> via.read(address - 0x6000, cpu);
            case 0x7             -> exPort07.read(address - 0x7000, cpu);
            default              -> rom[address - 0x8000];
        });
    }
    public static void set(int address, int value, boolean cpu) {
        address &= 0xffff;
        value &= 0xff;
        int select = address >> 12;
        switch (select) {
            case 0x0,0x1,0x2,0x3 -> ram[address] = (byte) value;
            case 0x4             -> exPort04.write(address - 0x4000, value, cpu);
            case 0x5             -> acia.write(address - 0x5000, value, cpu);
            case 0x6             -> via.write(address - 0x6000, value, cpu);
            case 0x7             -> exPort07.write(address - 0x7000, value, cpu);
            default -> {
                 if (!cpu) rom[address - 0x8000] = (byte) value;
            }
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void loadROM(File file) {
        boolean p = paused;
        paused = true;
        try {
            System.out.println();
            byte[] data = Files.readAllBytes(file.toPath());

            boolean c65 = true;
            for (int i = 0; i < 4; i++) {
                if (data[i] != ID_CHARS[i]) {
                    c65 = false;
                    break;
                }
            }
            if (c65) {
                loadC65(data);
            } else {
                loadRaw(data);
            }
        } catch (Exception e) {
            System.err.println("[!] Could not open file...");
            e.printStackTrace();
        } finally {
            reset = true;
            paused = p;
        }
    }
    private static void loadRaw(byte[] file) {
        System.out.println("[#] loading ROM");
        if (file.length < ROM_SIZE) {
            System.err.println("[!] Warning! this ROM has a file size below 32KiB!");
        } else if (file.length > ROM_SIZE) {
            System.err.printf("[!] Warning! This ROM has %d unused bytes left over\n", file.length - ROM_SIZE);
        }

        for (int i = 0; i < rom.length; i++) {
            rom[i] = (i < file.length) ? file[i] : 0x00;
        }
    }
    private static void loadC65(byte[] file) throws IOException {
        System.out.println("[#] loading .c65 file");
        if (file.length < HEADER_SIZE) {
            throw new IOException("A .c65 ROM needs to be at least %d bytes!".formatted(HEADER_SIZE));
        }
        // 0x7 to 0xf are reserved for future use

        System.out.println("[*] loading Expansion...");
        config.exPort04.set(ExpansionPort.fromID(file[0x4]));
        config.exPort07.set(ExpansionPort.fromID(file[0x5]));

        System.out.println("[*] loading ROM...");
        if (file.length < (ROM_SIZE + HEADER_SIZE)) {
            System.err.println("[!] Warning! this .c65 file has a ROM size below 32KiB!");
        }

        int index = HEADER_SIZE;
        for (int a = 0; a < rom.length; a++) {
            rom[a] = (index < file.length) ? file[index] : 0x00;
            index++;
        }
        index = ExpansionData.loadC65(file, index);

        if (index < file.length) {
            System.err.printf("[!] Warning! This file has %d unused bytes left over\n", file.length - index);
        }
        reset = true;
    }

    public static float getAudioBufferFillLevel() {
        float average = debug_bufferFillSum / ((float) debug_bufferFillCount);
        debug_bufferFillSum   = 0;
        debug_bufferFillCount = 0;
        return average;
    }

    public static double getTargetClockSpeed() {
        return 1_000_000_000d / config.clockPeriodNanos;
    }
    public static double getCurrentClockSpeed() {
        long   now         = System.nanoTime();
        long   elapsedTime = now - debug_lastClockSpeedSampleTime;
        double clockSpeed  = debug_executedClocks * 1_000_000_000d / elapsedTime;
        debug_executedClocks           = 0;
        debug_lastClockSpeedSampleTime = now;

        double clockSpeedAverage = clockSpeed;
        for (double pastSpeed : debug_lastClockSpeeds) {
            clockSpeedAverage += pastSpeed;
        }
        debug_lastClockSpeeds.add(clockSpeed);
        int sampleCount = debug_lastClockSpeeds.size();
        clockSpeedAverage /= sampleCount;
        if (sampleCount > 10) debug_lastClockSpeeds.removeFirst();

        return clockSpeedAverage;
    }
}
