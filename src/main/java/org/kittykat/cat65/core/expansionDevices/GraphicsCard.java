package org.kittykat.cat65.core.expansionDevices;

import javafx.scene.Scene;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.Cat65;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.ui.window.videoCard.ScreenWindow;
import org.kittykat.cat65.ui.window.videoCard.VideoSettingsWindow;

import java.nio.IntBuffer;
import java.util.Arrays;

public class GraphicsCard extends ExpansionDevice{

    private final VideoSettingsWindow settings = new VideoSettingsWindow();

    private static final float CLOCK_MHZ = 25.175f;

    private static final int H_VISIBLE = 640;

    private static final int V_VISIBLE = 480;

    private static final int MAX_RAY_DISTANCE = 255;

    private final int[] frame = new int[H_VISIBLE * V_VISIBLE];
    private final int[] colorBuffer = new int[frame.length];
    private final PixelBuffer<IntBuffer> pixelBuffer =
            new PixelBuffer<>(H_VISIBLE, V_VISIBLE, IntBuffer.wrap(colorBuffer), PixelFormat.getIntArgbPreInstance());

    private int[] pointsX = new int[64];
    private int[] pointsY = new int[64];
    private int[] pointsZ = new int[64];


    private int[] lineStart = new int[64];
    private int[] lineEnd = new int[64];
    private boolean[] lineActive = new boolean[64];

    private int[] pointX2d = new int[64];
    private int[] pointY2d = new int[64];

    private final int SENSOR_DISANCE = 16;

    private boolean[] pointActive = new boolean[64];

    private double[][] MATRIX = new double[4][4];


    private int TIME_ALIVE = 0;


    private volatile double timeAccumulator = 0f;

    public GraphicsCard(int portNum) {
        super(0b1111_1111_1111, portNum);
        Arrays.fill(frame, 0xff_000000);
        makeWindow();
        showWindow();


        MATRIX[0][3] = 0; // MOVE X
        MATRIX[1][3] = 0; // MOVE Y
        MATRIX[2][3] = 4; // MOVE Z
        MATRIX[3][3] = 1;

        MATRIX[0][0] = 1; // SCALE X
        MATRIX[1][1] = 1; // SCALE Y
        MATRIX[2][2] = 1; // SCALE Z


        int p1 = addPoint(-16,-16,0);
        int p2 = addPoint(16, -16, 0);
        int p3 = addPoint(16, 16, 0);
        int p4 = addPoint(-16, 16, 0);

        int p5 = addPoint(-16, -16, 16);
        int p6 = addPoint(16, -16, 16);
        int p7 = addPoint(16, 16, 16);
        int p8 = addPoint(-16, 16, 16);

        addLine(p1,p2);
        addLine(p2, p3);
        addLine(p3, p4);
        addLine(p4, p1);

        addLine(p5, p6);
        addLine(p6, p7);
        addLine(p7, p8);
        addLine(p8, p5);

        addLine(p1, p5);
        addLine(p2, p6);
        addLine(p3, p7);
        addLine(p4, p8);




    }

    @Override
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void clock() {
        timeAccumulator += CLOCK_MHZ;

        while (timeAccumulator >= 1f) {
            //videoClock();
            TIME_ALIVE += 1;
            //System.out.println("edfdsfsdf");
            timeAccumulator -= 1f;
        }
    }

    @Override
    protected int get(int relAddress, boolean cpu) {
        System.out.printf("%03x\n", relAddress);

        return 0xff;
    }

    @Override
    protected void set(int relAddress, int value, boolean cpu) {
        System.out.printf("%03x %02x\n", relAddress, value);
    }

    @Override
    public void updateWindow() {
        if ((!settings.vSync) || true) {
            //System.arraycopy(frame, 0, colorBuffer, 0, frame.length);
            //fillColor(255,0,0);

            MATRIX[0][3] = (int) (Math.sin(Math.toDegrees(TIME_ALIVE * 0.000000005)) * 40);


            fillColor(0,0,0);
            projectPoints();
            renderPoints();
            renderLines();
            //renderRays();
            updateScreen();

            //System.out.println("bleh");
            //fillColor(255,0,0);
        }

    }

    public int addPoint(int x, int y, int z){
        int index = getFreePointIndex();
        System.out.println(index);
        if (index != -1){
            pointsX[index] = x;
            pointsY[index] = y;
            pointsZ[index] = z;
            pointActive[index] = true;

        }
        return index;
    }

    public void addLine(int start, int end) {
        int index = getFreeLineIndex();
        System.out.println(index);
        if (index != -1) {
            lineStart[index] = start;
            lineEnd[index] = end;
            lineActive[index] = true;

        }
    }
    public int getFreePointIndex(){
        for (int i = 0; i < 64; i++) {
            if (!pointActive[i]) {
                return i;
            }
        }
        return -1;
    }

    public int getFreeLineIndex() {
        for (int i = 0; i < 64; i++) {
            if (!lineActive[i]) {
                return i;
            }
        }
        return -1;
    }

    public void renderPoints(){
        for (int i = 0; i < 64; i++) {
            if (pointActive[i]) {
                setPixelRGB(pointX2d[i], pointY2d[i],255,0,0);
            }
        }
    }

    public void plotLineBresenham(int x0, int y0, int x1, int y1){
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;

        while (true){
            setPixelRGB(x0,y0, 0,255,0);
            int e2 = 2*error;
            if (e2 >= dy){
                if (x0 == x1){break;}
                error = error + dy;
                x0 = x0 + sx;
            }
            if (e2 <= dx){
                if (y0 == y1) {break;}
                error = error + dx;
                y0 = y0 + sy;
            }
        }
    }

    public void renderLines(){
        for (int i = 0; i < 64; i++) {
            if (lineActive[i]) {
                plotLineBresenham(pointX2d[lineStart[i]], pointY2d[lineStart[i]], pointX2d[lineEnd[i]], pointY2d[lineEnd[i]]);
            }
        }
    }



    public void projectPoints(){
        for (int i = 0; i < 64; i ++){
            if (pointActive[i]){
                int x3d = pointsX[i];
                int y3d = pointsY[i];
                int z3d = pointsZ[i];

                //System.out.println(Arrays.deepToString(MATRIX));
                //System.out.println(MATRIX[2][2]);

                //System.out.println(((MATRIX[2][0] * x3d) + (MATRIX[2][1] * y3d) + (MATRIX[2][2] * z3d) + (MATRIX[2][3] * 1)));

                //System.out.println((MATRIX[2][0] * x3d));
                //System.out.println((MATRIX[2][1] * y3d));
                //System.out.println((MATRIX[2][2] * z3d));
                //System.out.println((MATRIX[2][3] * 1));
                //System.out.println(z3d);




                int x3d_out = (int) ((MATRIX[0][0] * x3d) + (MATRIX[0][1] * y3d) + (MATRIX[0][2] * z3d) + (MATRIX[0][3] * 1));
                int y3d_out = (int) ((MATRIX[1][0] * x3d) + (MATRIX[1][1] * y3d) + (MATRIX[1][2] * z3d) + (MATRIX[1][3] * 1));
                int z3d_out = (int) ((MATRIX[2][0] * x3d) + (MATRIX[2][1] * y3d) + (MATRIX[2][2] * z3d) + (MATRIX[2][3] * 1));
                int w3d_out = (int) ((MATRIX[3][0] * x3d) + (MATRIX[3][1] * y3d) + (MATRIX[3][2] * z3d) + (MATRIX[3][3] * 1));

                x3d = x3d_out;
                y3d = y3d_out;
                z3d = z3d_out;


                int x2d = (x3d * SENSOR_DISANCE) / z3d ;
                int y2d = (y3d * SENSOR_DISANCE) / z3d;

                pointX2d[i] = x2d + (H_VISIBLE >> 1);
                pointY2d[i] = y2d + (V_VISIBLE >> 1);
            }
        }
    }

    public double invSqr(double x){
        return 1/Math.sqrt(x);
    }

    public int runRay(int x, int y, int z, int dirX, int dirY, int dirZ){
        //MAX_RAY_DISTANCE
        int TRAVELLED_DISTANCE = 0;

        double rayX = x;
        double rayY = y;
        double rayZ = z;

        double rayNormaliseScale = invSqr(dirX* dirX + dirY* dirY + dirZ * dirZ);

        double dx = dirX * rayNormaliseScale;
        double dy = dirY * rayNormaliseScale;
        double dz = dirZ * rayNormaliseScale;


        boolean COLLISION = false;

        while (TRAVELLED_DISTANCE < MAX_RAY_DISTANCE){
            rayX = rayX + dx;
            rayY = rayY + dy;
            rayZ = rayZ + dz;
            TRAVELLED_DISTANCE += 1;

            //if (rayY > 2){
            //    COLLISION = true;
            //    break;

            //}

            if (Math.sqrt(rayX*rayX+ rayY * rayY + (rayZ-8) * (rayZ-8)) - 4 < 0){
                COLLISION = true;
                break;
            }
        }

        if (COLLISION == false){
            return -1;
        }

        return TRAVELLED_DISTANCE;

    }

    public void renderRays() {
        for (int x = 0; x < H_VISIBLE; x++) {
            for (int y = 0; y < V_VISIBLE; y++) {
                int dis = runRay(0,0,0, x-(H_VISIBLE >> 1), y - (V_VISIBLE >> 1), 128);
                if (dis == -1){
                    setPixelRGB(x, y, 0, 255, 0);
                } else {
                    setPixelRGB(x, y, dis, 0, 0);
                }
            }
        }
    }

    public void fillColor(int r, int g, int b){
        for (int x = 0; x<H_VISIBLE; x++){
            for (int y = 0; y < V_VISIBLE; y++) {
                setPixelRGB(x, y, r, g, b);
            }
        }
    }

    public void setPixelRGB(int x, int y, int r, int g, int b){
        setPixel(x,y, (r << 16) | (g << 8) | b);
    }

    public void setPixel(int x, int y, int col){
        colorBuffer[x+(y*H_VISIBLE)] = col | 0xff_000000;
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

        window.setTitle("GC3D Video Card [$%x000] >:3".formatted(portNum));
        window.setScene(scene);
        window.setResizable(false);
        window.setAlwaysOnTop(true);
    }
}
