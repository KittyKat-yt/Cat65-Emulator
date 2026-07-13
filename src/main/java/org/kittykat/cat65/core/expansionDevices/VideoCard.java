package org.kittykat.cat65.core.expansionDevices;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.Cat65;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.CMU;
import org.kittykat.cat65.core.ExpansionData;
import org.kittykat.cat65.ui.window.ConfigWindow;
import org.kittykat.cat65.ui.window.videoCard.ScreenWindow;
import org.kittykat.cat65.ui.window.videoCard.VideoSettingsWindow;

import java.lang.reflect.Array;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.kittykat.cat65.EmuHelper.boolBit;

public class VideoCard extends ExpansionDevice {
    private final VideoSettingsWindow settings = new VideoSettingsWindow();

    private static final float CLOCK_MHZ = 25.175f;
    private static final int   REG_ADDR_MASK = 0b0000_0000_0111;
    private static final int   VRAM_SIZE = 0x800;

    private static final int H_VISIBLE = 640;
    private static final int H_FRONT   = 16;
    private static final int H_SYNC    = 96;
    private static final int H_BACK    = 48;

    private static final int V_VISIBLE = 480;
    private static final int V_FRONT   = 10;
    private static final int V_SYNC    = 2;
    private static final int V_BACK    = 33;

    private final int[] frame       = new int[H_VISIBLE * V_VISIBLE];
    private final int[] colorBuffer = new int[frame.length];
    private final PixelBuffer<IntBuffer> pixelBuffer =
            new PixelBuffer<>(H_VISIBLE, V_VISIBLE, IntBuffer.wrap(colorBuffer), PixelFormat.getIntArgbPreInstance());

    private int nextTile  = 0x00;
    private int nextLineY = 0;
    private final int[] preloadedLayers = new int[4];
    private final int[] layerRegisters  = new int[4];

    private volatile int dotCounter  = 0;
    private volatile int lineCounter = 0;

    private volatile boolean hBlank = true;
    private volatile boolean hSync  = true;
    private volatile boolean vBlank = true;
    private volatile boolean vSync  = true;
    private int outputColor = 0xff_000000;

    private final byte[] vram00 = new byte[VRAM_SIZE];
    private final byte[] vram01 = new byte[VRAM_SIZE];

    private volatile int colormapBank = 0b000;
    private volatile int tilemapBank  = 0b000;
    private volatile int ctrl    = 0b0000_0000;
    private volatile int hScroll = 0x00;

    private volatile double timeAccumulator = 0d;

    public VideoCard(int portNum) {
        super(0b1111_1111_1111, portNum);
        Arrays.fill(frame, 0xff_000000);
        makeWindow();
        showWindow();
    }

    @Override
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void clock() {
        timeAccumulator += CLOCK_MHZ * (settings.syncClocks? 1d : (CMU.getTargetClockPeriod() / 1_000d));
        while (timeAccumulator >= 1f) {
            videoClock();
            timeAccumulator -= 1f;
        }
    }

    @Override
    protected int get(int relAddress, boolean cpu) {
        if (relAddress < 0x800) {
            relAddress &= REG_ADDR_MASK;
            return switch (relAddress) {
                case 0x0 -> 0x07;
                case 0x1 -> (colormapBank << 4) | tilemapBank;
                case 0x2 -> (boolBit(vBlank) << 7) | (boolBit(hBlank) << 6) | (boolBit(isBlanking()) << 4)
                          | (boolBit(vSync)  << 3) | (boolBit(hSync)  << 2);
                case 0x3 -> ctrl;
                case 0x4 -> hScroll;
                case 0x6 -> (lineCounter >> 8);
                case 0x7 -> (lineCounter & 0x0ff);
                default  -> CMU.getMDR();
            };
        } else {
            relAddress -= 0x800;
            return ((ctrl & 0b0000_1000) != 0) ? vram00[relAddress] : vram01[relAddress];
        }
    }
    @Override
    protected void set(int relAddress, int value, boolean cpu) {
        if (relAddress < 0x800) {
            relAddress &= REG_ADDR_MASK;
            switch (relAddress) {
                case 0x1 -> {
                    colormapBank = (value & 0b0111_0000) >> 4;
                    tilemapBank  = (value & 0b0000_0111);
                }
                case 0x3 -> ctrl    = value & 0b1100_1000;
                case 0x4 -> hScroll = value & 0x3f;
            }
        } else {
            relAddress -= 0x800;
            if ((ctrl & 0b0000_1000) != 0) {
                vram00[relAddress] = (byte) value;
            } else {
                vram01[relAddress] = (byte) value;
            }
        }
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private void videoClock() {
        switch (dotCounter & 0x00f) {
            case 0x0, 0x2,
                 0x4, 0x6,
                 0x8, 0xa,
                 0xc, 0xe -> outputPixel();
            case 0x1, 0x5 -> shiftBgRegisters();
            case 0x3 -> {
                shiftBgRegisters();
                preloadTile();
            }
            case 0x7 -> {
                shiftBgRegisters();
                getTileLayer(0);
            }
            case 0x9 -> {
                shiftBgRegisters();
                getTileLayer(1);
            }
            case 0xb -> {
                shiftBgRegisters();
                getTileLayer(2);
            }
            case 0xd -> {
                shiftBgRegisters();
                getTileLayer(3);
            }
            case 0x0f -> transferLayers();
        }
        if (isVisible()) {
            frame[lineCounter * H_VISIBLE + dotCounter] = outputColor;
        }

        dotCounter++;
        if (dotCounter == H_VISIBLE) {
            hBlank = true;
        } else if (dotCounter == (H_VISIBLE + H_FRONT)) {
            hSync = false;
        } else if (dotCounter == (H_VISIBLE + H_FRONT + H_SYNC)) {
            hSync = true;
        } else if (dotCounter == (H_VISIBLE + H_FRONT + H_SYNC + H_BACK)) {
            dotCounter = 0;
            hBlank = false;

            lineCounter++;
            if (lineCounter == V_VISIBLE) {
                vBlank = true;

                if (settings.vSync) {
                    System.arraycopy(frame, 0, colorBuffer, 0, frame.length);
                    Platform.runLater(this::updateScreen);
                }
            } else if (lineCounter == (V_VISIBLE + V_FRONT)) {
                vSync = false;
            } else if (lineCounter == (V_VISIBLE + V_FRONT + V_SYNC)) {
                vSync = true;
            } else if (lineCounter == (V_VISIBLE + V_FRONT + V_SYNC + V_BACK)) {
                lineCounter = 0;
                vBlank = false;
            }
        }
    }

    private void preloadTile() {
        int x = (dotCounter >> 4) + 1;
        int y = lineCounter;

        if (vBlank) {
            x = 0;
            y = 0;
        } else if (hBlank) {
            x = 0;
            y++;
        }
        nextLineY = (y >> 1) & 0b111;
        if (shouldPreload()) {
            x = (x + hScroll) & 0b11_1111;
            y = (y >> 4) & 0b1_1111;
            nextTile = readVRAM(x | (y << 6));
        }
    }
    private void getTileLayer(int layer) {
        int address = (tilemapBank << 13) | (nextTile << 5) | (layer << 3) | nextLineY;
        preloadedLayers[layer] = readTile(address);
    }
    private void transferLayers() {
        if (shouldPreload()) {
            System.arraycopy(preloadedLayers, 0, layerRegisters, 0, 4);
        }
    }
    private void shiftBgRegisters() {
        if (isVisible()) {
            for (int l = 0; l < 4; l++) {
                layerRegisters[l] <<= 1;
            }
        }
    }
    private void outputPixel() {
        if (isVisible()) {
            outputColor = 0xff_000000;
            if (!isForceBlanking()) {
                int paletteAddress = 0x0;
                for (int l = 0; l < 4; l++) {
                    paletteAddress >>= 1;
                    paletteAddress |= layerRegisters[l] & 0x80;
                }
                paletteAddress = (paletteAddress >> 4) | 0x780;
                outputColor = ExpansionData.getColor(readVRAM(paletteAddress) | (colormapBank << 8));
            }
        }
    }

    private int readVRAM(int address) {
        return (((ctrl & 0b0000_1000) == 0) ? vram00[address] : vram01[address]) & 0xff;
    }
    private int readTile(int address) {
        return ExpansionData.tilesetROM[address] & 0xff;
    }

    public boolean shouldPreload() {
        return !(vBlank & hSync);
    }
    public boolean isBlanking() {
        return !isVisible() | isForceBlanking();
    }
    public boolean isForceBlanking() {
        return ((ctrl & 0b0100_0000) == 0);
    }
    public boolean isVisible() {
        return !(hBlank | vBlank);
    }

    @Override
    public boolean getNMI() {
        return !(((ctrl & 0b1000_0000) != 0) && vBlank);
    }

    @Override
    public void updateWindow() {
        if (!settings.vSync) {
            System.arraycopy(frame, 0, colorBuffer, 0, frame.length);
            updateScreen();
        }
    }
    private void updateScreen() {
        pixelBuffer.updateBuffer(buffer -> null);
    }

    @Override
    protected void makeWindow() {
        super.makeWindow();

        ScreenWindow screen = new ScreenWindow(pixelBuffer);

        VBox root = new VBox(Cat65.SPACING, settings, screen);
        root.setId("root");

        Scene scene = new Scene(root);
        EmuHelper.applyCSS(scene);

        window.setTitle("Vgc7 Video Card [$%x000] :3c".formatted(portNum));
        window.setScene(scene);
        window.setResizable(false);
        window.setAlwaysOnTop(true);
    }
}
