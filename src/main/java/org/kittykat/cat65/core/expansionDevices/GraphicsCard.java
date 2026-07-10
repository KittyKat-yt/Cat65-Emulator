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
    private final int[] depthBuffer = new int[frame.length];
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
    private int[] pointZ2d = new int[64];
    private boolean[] pointActive = new boolean[64];


    private int[] trianglePoint1 = new int[64];
    private int[] trianglePoint2 = new int[64];
    private int[] trianglePoint3 = new int[64];
    private boolean[] triangleActive = new boolean[64];

    private final int SENSOR_DISANCE = 32;



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
        MATRIX[2][3] = 32; // MOVE Z
        MATRIX[3][3] = 1;

        MATRIX[0][0] = 1; // SCALE X
        MATRIX[1][1] = 1; // SCALE Y
        MATRIX[2][2] = 1; // SCALE Z


        int p1 = addPoint(-16,-16,-16);
        int p2 = addPoint(16, -16, -16);
        int p3 = addPoint(16, 16, -16);
        int p4 = addPoint(-16, 16, -16);

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


        addTriangle(p1, p2, p3);
        addTriangle(p1, p3, p4);
        addTriangle(p5, p6, p7);
        addTriangle(p5, p7, p8);
        addTriangle(p1, p4, p8);
        addTriangle(p1, p8, p5);
        addTriangle(p2, p3, p7);
        addTriangle(p2, p7, p6);
        addTriangle(p1, p2, p6);
        addTriangle(p1, p6, p5);
        addTriangle(p4, p3, p7);
        addTriangle(p4, p7, p8);





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

            //MATRIX[0][3] = (int) (Math.sin(Math.toDegrees(TIME_ALIVE * 0.000000005)) * 40);

            // Calculate your rotation angle over time (in radians)
// 1. Calculate angles for both axes (using slightly different speeds so it looks organic)
            double angleY = TIME_ALIVE * 0.00000005;
            double cosY = Math.cos(angleY);
            double sinY = Math.sin(angleY);

            double angleX = TIME_ALIVE * 0.00000003; // Slightly slower or faster looks best
            double cosX = Math.cos(angleX);
            double sinX = Math.sin(angleX);

// 2. Combine Y-Axis and X-Axis rotations mathematically into a single matrix
// This represents: Final Matrix = Y_Rotation * X_Rotation
            MATRIX[0][0] = cosY;
            MATRIX[0][1] = sinY * sinX;
            MATRIX[0][2] = sinY * cosX;

            MATRIX[1][0] = 0;
            MATRIX[1][1] = cosX;
            MATRIX[1][2] = -sinX;

            MATRIX[2][0] = -sinY;
            MATRIX[2][1] = cosY * sinX;
            MATRIX[2][2] = cosY * cosX;


            //MATRIX[0][0] *= 2; // SCALE X
            //MATRIX[1][1] *= 2; // SCALE Y
            //MATRIX[2][2] *= 2; // SCALE Z

// 3. Keep the identity components for the unrotated parts if it's a 4x4 matrix
// If your matrix is 4x4, ensure MATRIX[3][3] = 1.0 and the rest of row/col 3 are 0.
            fillDepth(255);
            fillColor(0,0,0);
            projectPoints();
            //renderPoints();
            //renderLines();
            renderTriangles();
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


    public int addTriangle(int tri1, int tri2, int tri3) {
        int index = getFreeTriIndex();
        System.out.println(index);
        if (index != -1) {
            trianglePoint1[index] = tri1;
            trianglePoint2[index] = tri2;
            trianglePoint3[index] = tri3;
            triangleActive[index] = true;

        }
        return index;
    }

    public int getFreePointIndex(){
        for (int i = 0; i < 64; i++) {
            if (!pointActive[i]) {
                return i;
            }
        }
        return -1;
    }

    public int getFreeTriIndex() {
        for (int i = 0; i < 64; i++) {
            if (!triangleActive[i]) {
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
        plotLineBresenham(x0,y0,0,x1,y1,0);
    }

    public void plotLineBresenham(int x0, int y0, int z0, int x1, int y1, int z1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;

        int red = 0;
        int green = 255;
        int blue = 255;

        // Driving axis is X
        if (dx >= dy && dx >= dz) {
            int err_1 = 2 * dy - dx;
            int err_2 = 2 * dz - dx;
            while (true) {
                setPixelRGB(x0, y0, 0, 255, 0); // Assuming setPixelRGB takes z now
                setDepth(x0, y0, z0);
                if (x0 == x1) break;

                if (err_1 >= 0) {
                    y0 += sy;
                    err_1 -= 2 * dx;
                }
                if (err_2 >= 0) {
                    z0 += sz;
                    err_2 -= 2 * dx;
                }
                err_1 += 2 * dy;
                err_2 += 2 * dz;
                x0 += sx;
            }
        }
        // Driving axis is Y
        else if (dy >= dx && dy >= dz) {
            int err_1 = 2 * dx - dy;
            int err_2 = 2 * dz - dy;
            while (true) {
                setPixelRGB(x0, y0, 0, 255, 0);
                setDepth(x0, y0, z0);
                if (y0 == y1) break;

                if (err_1 >= 0) {
                    x0 += sx;
                    err_1 -= 2 * dy;
                }
                if (err_2 >= 0) {
                    z0 += sz;
                    err_2 -= 2 * dy;
                }
                err_1 += 2 * dx;
                err_2 += 2 * dz;
                y0 += sy;
            }
        }
        // Driving axis is Z
        else {
            int err_1 = 2 * dx - dz;
            int err_2 = 2 * dy - dz;
            while (true) {
                setPixelRGB(x0, y0, 0, 255, 0);
                setDepth(x0,y0,z0);
                if (z0 == z1) break;

                if (err_1 >= 0) {
                    x0 += sx;
                    err_1 -= 2 * dz;
                }
                if (err_2 >= 0) {
                    y0 += sy;
                    err_2 -= 2 * dz;
                }
                err_1 += 2 * dx;
                err_2 += 2 * dy;
                z0 += sz;
            }
        }
    }

    public void renderLines(){
        for (int i = 0; i < 64; i++) {
            if (lineActive[i]) {
                plotLineBresenham(pointX2d[lineStart[i]], pointY2d[lineStart[i]], pointZ2d[lineStart[i]], pointX2d[lineEnd[i]], pointY2d[lineEnd[i]], pointZ2d[lineEnd[i]]);
            }
        }
    }

    public void renderTriangles() {
        for (int i = 0; i < 64; i++) {
            if (triangleActive[i]) {
                int x0 = pointX2d[trianglePoint1[i]];
                int y0 = pointY2d[trianglePoint1[i]];
                int z0 = pointZ2d[trianglePoint1[i]];

                int x1 = pointX2d[trianglePoint2[i]];
                int y1 = pointY2d[trianglePoint2[i]];
                int z1 = pointZ2d[trianglePoint2[i]];

                int x2 = pointX2d[trianglePoint3[i]];
                int y2 = pointY2d[trianglePoint3[i]];
                int z2 = pointZ2d[trianglePoint3[i]];
                fillTriangle(x0,y0,z0, x1, y1, z1, x2, y2, z2);
                //plotLineBresenham(pointX2d[lineStart[i]], pointY2d[lineStart[i]], pointZ2d[lineStart[i]], pointX2d[lineEnd[i]], pointY2d[lineEnd[i]], pointZ2d[lineEnd[i]]);
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

                if (z3d > 0) {


                    int x2d = (x3d * SENSOR_DISANCE) / z3d;
                    int y2d = (y3d * SENSOR_DISANCE) / z3d;

                    pointX2d[i] = x2d + (H_VISIBLE >> 1);
                    pointY2d[i] = y2d + (V_VISIBLE >> 1);
                    pointZ2d[i] = z3d;

                }
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

    public void fillDepth(int depth) {
        for (int x = 0; x < H_VISIBLE; x++) {
            for (int y = 0; y < V_VISIBLE; y++) {
                setDepth(x,y, depth);
            }
        }
    }

    public void setPixelRGB(int x, int y, int r, int g, int b){
        setPixel(x,y, (r << 16) | (g << 8) | b);
    }

    public void setPixel(int x, int y, int col){
        if (x < H_VISIBLE && y < V_VISIBLE && x > 0 && y > 0) {
            int index = x + (y * H_VISIBLE);
            if (index < colorBuffer.length) {
                colorBuffer[x + (y * H_VISIBLE)] = col | 0xff_000000;
            }
        }
    }

    public void fillTriangle(int x0, int y0, int z0, int x1, int y1, int z1, int x2, int y2, int z2) {
        // 1. Find the bounding box of the triangle
        int minX = Math.min(x0, Math.min(x1, x2));
        int maxX = Math.max(x0, Math.max(x1, x2));
        int minY = Math.min(y0, Math.min(y1, y2));
        int maxY = Math.max(y0, Math.max(y1, y2));

        // 2. Pre-calculate the denominator for barycentric coordinates
        // This represents twice the area of the triangle
        float area = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
        if (Math.abs(area) < 0.000001f) return; // Degenerate triangle (flat line)

        // 3. Loop over every pixel in the bounding box
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {

                // Calculate barycentric weights (w0, w1, w2) for the pixel center
                float w0 = ((y1 - y2) * (x - x2) + (x2 - x1) * (y - y2)) / area;
                float w1 = ((y2 - y0) * (x - x2) + (x0 - x2) * (y - y2)) / area;
                float w2 = 1.0f - w0 - w1;

                // If all weights are >= 0, the pixel is inside the triangle
                if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
                    // Interpolate the Z value perfectly across the surface
                    int interpolatedZ = Math.round(w0 * z0 + w1 * z1 + w2 * z2);

                    // Plot the pixel directly instead of drawing lines
                    setPixelRGBWithDepth(x,y,0, interpolatedZ*8,0,interpolatedZ);
                    //setDepth(x, y, interpolatedZ);
                }
            }
        }
    }

    public void setDepth(int x, int y, int depth) {
        if (x < H_VISIBLE && y < V_VISIBLE && x > 0 && y > 0) {
            int index = x + (y * H_VISIBLE);
            if (index < colorBuffer.length) {
                depthBuffer[x + (y * H_VISIBLE)] = depth;
                //setPixelRGB(x,y,depth, depth,255);
            }
        }
    }

    public int getDepth(int x, int y){
        if (x < H_VISIBLE && y < V_VISIBLE && x > 0 && y > 0) {
            int index = x + (y * H_VISIBLE);
            if (index < colorBuffer.length) {
                return depthBuffer[x + (y * H_VISIBLE)];
            }
        }
        return 0;
    }

    public void setPixelRGBWithDepth(int x, int y, int r, int g, int b, int depth){
        if (getDepth(x,y) > depth){
            setPixelRGB(x,y,r,g,b);
            setDepth(x,y,depth);
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

        window.setTitle("GC3D Video Card [$%x000] >:3".formatted(portNum));
        window.setScene(scene);
        window.setResizable(false);
        window.setAlwaysOnTop(true);
    }
}
