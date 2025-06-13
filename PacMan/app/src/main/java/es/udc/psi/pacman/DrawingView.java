package es.udc.psi.pacman;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private Thread thread;
    private SurfaceHolder holder;
    private boolean canDraw = true;
    private boolean useButtonControls;
    protected GameSession gameSession;

    protected Paint paint;
    private Bitmap[] pacmanRight, pacmanDown, pacmanLeft, pacmanUp;
    private Bitmap[] arrowRight, arrowDown, arrowLeft, arrowUp;
    private Bitmap ghostBitmap;
    private int totalFrame = 4;             // Total amount of frames fo each direction
    protected int currentPacmanFrame = 0;     // Current Pacman frame to draw
    private int currentArrowFrame = 0;      // Current arrow frame to draw
    private long frameTicker;               // Current time since last frame has been drawn
    protected int xPosPacman;                 // x-axis position of pacman
    protected int yPosPacman;                 // y-axis position of pacman
    protected int xPosGhost;                  // x-axis position of ghost
    protected int yPosGhost;                  // y-axis position of ghost
    int xDistance;
    int yDistance;
    private float x1, x2, y1, y2;           // Initial/Final positions of swipe
    protected int direction = 4;              // Direction of the swipe, initial direction is right
    protected int nextDirection = 4;          // Buffer for the next direction you choose
    protected int viewDirection = 2;          // Direction that pacman is facing
    private int ghostDirection;
    private int arrowDirection = 4;
    private int screenWidth;                // Width of the phone screen
    protected int blockSize;                  // Size of a block on the map
    public static int LONG_PRESS_TIME=750;  // Time in milliseconds
    protected int currentScore = 0;           //Current game score
    protected static final int GHOST_DAMAGE   = 200;
    protected static final int RESPAWN_X      = 8;
    protected static final int RESPAWN_Y      = 13;
    protected static final int MAX_LIVES    = 1;
    protected int   lives[]   = { MAX_LIVES };
    protected Paint textPaint = new Paint();
    private final short[][] originalLevel =
            new short[18][17];
    private int pelletsLeft;
    private static final int[] DX = { 0,  1, 0, -1 };   // up, right, down, left
    private static final int[] DY = {-1,  0, 1,  0 };
    private int lastGhostDir = 4;
    final Handler handler = new Handler();
    List<Integer> freeDirs = new ArrayList<>(4);



    public DrawingView(Context context) {
        this(context, null);
        // Inicializar sesion de juego
        gameSession = new GameSession("clasico", 1);
    }


    public DrawingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public DrawingView(Context context,
                       AttributeSet attrs,
                       int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }


    public DrawingView(Context context,
                       AttributeSet attrs,
                       int defStyleAttr,
                       int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    private void init(Context context) {
        holder = getHolder();
        holder.addCallback(this);

        frameTicker = 1000 / totalFrame;

        paint = new Paint();
        paint.setColor(Color.WHITE);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        blockSize   = screenWidth / 17;
        blockSize   = (blockSize / 5) * 5;

        xPosGhost      = 8 * blockSize;
        ghostDirection = 4;
        yPosGhost      = 4 * blockSize;

        xPosPacman = 8 * blockSize;
        yPosPacman = 13 * blockSize;

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(blockSize);
        textPaint.setAntiAlias(true);

        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 17; j++) {
                originalLevel[i][j] = leveldata1[i][j];
                if ((leveldata1[i][j] & 16) != 0) pelletsLeft++;   // cuenta pellets
            }
        }


        SharedPreferences prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE);
        useButtonControls = prefs.getBoolean("use_button_controls", false);

        loadBitmapImages();

        Log.i("info", "DrawingView initialised");
    }

    @Override
    public void run() {
        Log.i("info", "Run");
        while (canDraw) {
            if (!holder.getSurface().isValid()) {
                continue;
            }
            Canvas canvas = holder.lockCanvas();
            // Set background color to Transparent
            if (canvas != null) {
                canvas.drawColor(Color.BLACK);
                drawMap(canvas);
                drawArrowIndicators(canvas);

                updateFrame(System.currentTimeMillis());

                moveGhost(canvas);

                // Moves the pacman based on his direction
                movePacman(canvas);

                checkCollisionSingle();

                // Draw the pellets
                drawPellets(canvas);

                //Update current and high scores
                updateScores(canvas);
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    public void updateScores(Canvas canvas) {
        paint.setTextSize(blockSize);

        Globals g = Globals.getInstance();
        int highScore = g.getHighScore();
        if (currentScore > highScore) {
            g.setHighScore(currentScore);
        }

        String formattedHighScore = String.format("%05d", highScore);
        String hScore = "High Score : " + formattedHighScore;
        canvas.drawText(hScore, 0, 2*blockSize - 10, paint);

        String formattedScore = String.format("%05d", currentScore);
        String score = "Score : " + formattedScore;
        canvas.drawText(score, 11 * blockSize, 2 * blockSize - 10, paint);
        canvas.drawText("Lives : " + lives[0], 0, blockSize * 3 - 10, textPaint);
    }

    @SuppressWarnings("ConstantConditions")
    public void moveGhost(Canvas canvas) {

        if (xPosGhost >= blockSize * 17) xPosGhost = 0;
        if (xPosGhost < 0)               xPosGhost = blockSize * 17;

        if (xPosGhost % blockSize == 0 && yPosGhost % blockSize == 0) {

            short ch = leveldata1[yPosGhost / blockSize][xPosGhost / blockSize];

            freeDirs.clear();
            if ((ch & 2) == 0) freeDirs.add(0); // up
            if ((ch & 4) == 0) freeDirs.add(1); // right
            if ((ch & 8) == 0) freeDirs.add(2); // down
            if ((ch & 1) == 0) freeDirs.add(3); // left

            int best   = -1;
            int bestD  = Integer.MAX_VALUE;

            for (int d : freeDirs) {

                if (freeDirs.size() > 1 && (d + 2) % 4 == lastGhostDir) continue;

                int nx = xPosGhost + DX[d]*blockSize;
                int ny = yPosGhost + DY[d]*blockSize;
                int dist = Math.abs(nx - xPosPacman) + Math.abs(ny - yPosPacman);

                if (dist < bestD) { bestD = dist; best = d; }
            }
            if (best == -1) best = freeDirs.get(0);   // sólo quedaba la inversa

            ghostDirection = best;
            lastGhostDir   = best;
        }

        xPosGhost += DX[ghostDirection] * blockSize / 20;
        yPosGhost += DY[ghostDirection] * blockSize / 20;

        canvas.drawBitmap(ghostBitmap, xPosGhost, yPosGhost, paint);
    }


    // Updates the character sprite and handles collisions
    protected void moveSinglePacman(Canvas canvas,
                                    int idx,
                                    int[] xPos, int[] yPos,
                                    int[] dir, int[] nextDir,
                                    int[] viewDir) {

        // -------- acceso a los datos del jugador ----------
        int x  = xPos[idx];
        int y  = yPos[idx];
        int d  = dir[idx];
        int nd = nextDir[idx];
        int vd = viewDir[idx];
        // ---------------------------------------------------

        short ch;

        // ¿está alineado con la cuadrícula?
        if ((x % blockSize == 0) && (y % blockSize == 0)) {

            // túnel derecha→izquierda
            if (x >= blockSize * 17) x = 0;

            // celda actual del mapa
            ch = leveldata1[y / blockSize][x / blockSize];

            // ¿pellet?
            if ((ch & 16) != 0) {
                leveldata1[y / blockSize][x / blockSize] = (short) (ch ^ 16);
                currentScore += 10;
                if (--pelletsLeft == 0) {
                    // Nivel completado - llamar al método levelCompleted en lugar de resetPellets
                    levelCompleted();
                    return;
                }
            }

            // *buffer* de dirección
            if (!((nd == 3 && (ch & 1) != 0) ||
                    (nd == 1 && (ch & 4) != 0) ||
                    (nd == 0 && (ch & 2) != 0) ||
                    (nd == 2 && (ch & 8) != 0))) {
                vd = d = nd;
            }

            // colisión con pared
            if ((d == 3 && (ch & 1) != 0) ||
                    (d == 1 && (ch & 4) != 0) ||
                    (d == 0 && (ch & 2) != 0) ||
                    (d == 2 && (ch & 8) != 0)) {
                d = 4;   // parar
            }
        }

        // túnel izquierda→derecha
        if (x < 0) x = blockSize * 17;

        // dibujar
        drawSinglePacman(canvas, vd, x, y);

        // avanzar según dirección
        if (d == 0)        y += -blockSize / 15;
        else if (d == 1)   x +=  blockSize / 15;
        else if (d == 2)   y +=  blockSize / 15;
        else if (d == 3)   x += -blockSize / 15;

        // -------- volcar cambios -------------
        xPos[idx]     = x;
        yPos[idx]     = y;
        dir[idx]      = d;
        nextDir[idx]  = nd;
        viewDir[idx]  = vd;
        // -------------------------------------
    }

    public void movePacman(Canvas canvas) {

        int[] x  = { xPosPacman };
        int[] y  = { yPosPacman };
        int[] d  = { direction };
        int[] nd = { nextDirection };
        int[] vd = { viewDirection };

        moveSinglePacman(canvas, 0, x, y, d, nd, vd);

        xPosPacman   = x[0];
        yPosPacman   = y[0];
        direction    = d[0];
        nextDirection= nd[0];
        viewDirection= vd[0];
    }

    protected boolean intersects(int x1,int y1,int x2,int y2){
        int s = blockSize;               // el sprite es cuadrado
        return Math.abs(x1-x2) < 0.6*s && Math.abs(y1-y2) < 0.6*s;
    }

    private void checkCollisionSingle() {
        if (intersects(xPosPacman, yPosPacman, xPosGhost, yPosGhost)) {


            currentScore = Math.max(0, currentScore - GHOST_DAMAGE);


            // paint.setAlpha(120);
            // drawSinglePacman(...)

            if (--lives[0] == 0) {
                gameOver();
                return;
            }


            xPosPacman = RESPAWN_X * blockSize;
            yPosPacman = RESPAWN_Y * blockSize;
            direction      = 4;
            nextDirection  = 4;
            viewDirection  = 2;
        }
    }

    protected void gameOver() {
        // Finalizar sesión de juego
        if (gameSession != null) {
            gameSession.updateMainPlayerScore(currentScore, lives[0]);
            gameSession.endGame(false); // false porque se acabaron las vidas
        }
        
        /* muestra Toast en el hilo de UI y vuelve a MainActivity */
        post(() -> {
            Context ctx = getContext();
            Toast.makeText(ctx,
                    "Game Over. Puntuación obtenida: " + currentScore,
                    Toast.LENGTH_LONG).show();

            if (ctx instanceof Activity) ((Activity) ctx).finish();
            ctx.startActivity(new Intent(ctx, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK));
        });
    }

    protected void levelCompleted() {
        if (gameSession != null) {
            gameSession.updateMainPlayerScore(currentScore, lives[0]);
            gameSession.endGame(true); // true porque se completó el nivel
        }
        
        post(() -> {
            Context ctx = getContext();
            Toast.makeText(ctx,
                    "¡Nivel completado! Puntuación: " + currentScore,
                    Toast.LENGTH_LONG).show();

            if (ctx instanceof Activity) ((Activity) ctx).finish();
            ctx.startActivity(new Intent(ctx, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK));
        });
    }

    // Actualizar puntuación periódicamente
    protected void updateGameSession() {
        if (gameSession != null) {
            gameSession.updateMainPlayerScore(currentScore, lives[0]);
        }
    }

    private void drawArrowIndicators(Canvas canvas) {
        switch(nextDirection) {
            case(0):
                canvas.drawBitmap(arrowUp[currentArrowFrame],5*blockSize , 20*blockSize, paint);
                break;
            case(1):
                canvas.drawBitmap(arrowRight[currentArrowFrame],5*blockSize , 20*blockSize, paint);
                break;
            case(2):
                canvas.drawBitmap(arrowDown[currentArrowFrame],5*blockSize , 20*blockSize, paint);
                break;
            case(3):
                canvas.drawBitmap(arrowLeft[currentArrowFrame],5*blockSize , 20*blockSize, paint);
                break;
            default:
                break;
        }

    }

    protected void drawSinglePacman(Canvas canvas,
                                    int viewDir, int x, int y) {
        switch (viewDir) {
            case 0: canvas.drawBitmap(pacmanUp[currentPacmanFrame],    x, y, paint); break;
            case 1: canvas.drawBitmap(pacmanRight[currentPacmanFrame], x, y, paint); break;
            case 3: canvas.drawBitmap(pacmanLeft[currentPacmanFrame],  x, y, paint); break;
            default: canvas.drawBitmap(pacmanDown[currentPacmanFrame], x, y, paint);
        }
    }

    // Method that draws pacman based on his viewDirection
    public void drawPacman(Canvas canvas) {
        drawSinglePacman(canvas, viewDirection, xPosPacman, yPosPacman);
    }


    // Method that draws pellets and updates them when eaten
    public void drawPellets(Canvas canvas) {
        float x;
        float y;
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 17; j++) {
                x = j * blockSize;
                y = i * blockSize;
                // Draws pellet in the middle of a block
                if ((leveldata1[i][j] & 16) != 0)
                    canvas.drawCircle(x + blockSize / 2, y + blockSize / 2, blockSize / 10, paint);
            }
        }
    }

    // Method to draw map layout
    public void drawMap(Canvas canvas) {
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(2.5f);
        int x;
        int y;
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 17; j++) {
                x = j * blockSize;
                y = i * blockSize;
                if ((leveldata1[i][j] & 1) != 0) // draws left
                    canvas.drawLine(x, y, x, y + blockSize - 1, paint);

                if ((leveldata1[i][j] & 2) != 0) // draws top
                    canvas.drawLine(x, y, x + blockSize - 1, y, paint);

                if ((leveldata1[i][j] & 4) != 0) // draws right
                    canvas.drawLine(
                            x + blockSize, y, x + blockSize, y + blockSize - 1, paint);
                if ((leveldata1[i][j] & 8) != 0) // draws bottom
                    canvas.drawLine(
                            x, y + blockSize, x + blockSize - 1, y + blockSize , paint);
            }
        }
        paint.setColor(Color.WHITE);
    }

    Runnable longPressed = new Runnable() {
        @Override public void run() {

            canDraw = false;
            if (thread != null) thread = null;

            Context ctx = getContext();
            if (ctx instanceof Activity) {
                ((Activity) ctx).finish();
            }

            Intent i = new Intent(ctx, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        }
    };

    // Method to get touch events
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!useButtonControls) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    y1 = event.getY();
                    handler.postDelayed(longPressed, LONG_PRESS_TIME);
                    break;
                case MotionEvent.ACTION_UP:
                    x2 = event.getX();
                    y2 = event.getY();
                    calculateSwipeDirection();
                    handler.removeCallbacks(longPressed);
                    break;
            }
        }
        return true;
    }

    public void setNextDirection(int direction) {
        this.nextDirection = direction;
    }


    // Calculates which direction the user swipes
    // based on calculating the differences in
    // initial position vs final position of the swipe
    private void calculateSwipeDirection() {
        float xDiff = (x2 - x1);
        float yDiff = (y2 - y1);

        // Directions
        // 0 means going up
        // 1 means going right
        // 2 means going down
        // 3 means going left
        // 4 means stop moving, look at move function

        // Checks which axis has the greater distance
        // in order to see which direction the swipe is
        // going to be (buffering of direction)
        if (Math.abs(yDiff) > Math.abs(xDiff)) {
            if (yDiff < 0) {
                nextDirection = 0;
            } else if (yDiff > 0) {
                nextDirection = 2;
            }
        } else {
            if (xDiff < 0) {
                nextDirection = 3;
            } else if (xDiff > 0) {
                nextDirection = 1;
            }
        }
    }

    // Check to see if we should update the current frame
    // based on time passed so the animation won't be too
    // quick and look bad
    private void updateFrame(long gameTime) {

        // If enough time has passed go to next frame
        if (gameTime > frameTicker + (totalFrame * 30)) {
            frameTicker = gameTime;

            // Increment the frame
            currentPacmanFrame++;
            // Loop back the frame when you have gone through all the frames
            if (currentPacmanFrame >= totalFrame) {
                currentPacmanFrame = 0;
            }
        }
        if (gameTime > frameTicker + (50)) {
            currentArrowFrame++;
            if (currentArrowFrame >= 7) {
                currentArrowFrame = 0;
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("info", "Surface Created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("info", "Surface Changed");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("info", "Surface Destroyed");
    }

    public void pause() {
        Log.i("info", "pause");
        canDraw = false;
        if (thread != null) {
            try { thread.join(); }
            catch (InterruptedException ignored) {}
            thread = null;
        }
    }

    public void resume() {
        Log.i("info", "resume");

        if (thread == null || !thread.isAlive()) {
            thread = new Thread(this);
            thread.start();
            Log.i("info", "resume thread");
        }
        canDraw = true;
    }

    private void loadBitmapImages() {
        // Scales the sprites based on screen
        int spriteSize = screenWidth/17;        // Size of Pacman & Ghost
        spriteSize = (spriteSize / 5) * 5;      // Keep it a multiple of 5
        int arrowSize = 7*blockSize;            // Size of arrow indicators

        // Add bitmap images of right arrow indicators
        arrowRight = new Bitmap[7]; // 7 image frames for right direction
        arrowRight[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.right_arrow_frame1), arrowSize, arrowSize, false);
        arrowRight[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.right_arrow_frame2), arrowSize, arrowSize, false);
        arrowRight[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.right_arrow_frame3), arrowSize, arrowSize, false);
        arrowRight[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.right_arrow_frame4), arrowSize, arrowSize, false);
        arrowRight[4] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.right_arrow_frame5), arrowSize, arrowSize, false);
        arrowRight[5] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.right_arrow_frame6), arrowSize, arrowSize, false);
        arrowRight[6] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.right_arrow_frame7), arrowSize, arrowSize, false);

        arrowDown = new Bitmap[7]; // 7 images frames for down direction
        arrowDown[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.down_arrow_frame1), arrowSize, arrowSize, false);
        arrowDown[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.down_arrow_frame2), arrowSize, arrowSize, false);
        arrowDown[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.down_arrow_frame3), arrowSize, arrowSize, false);
        arrowDown[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.down_arrow_frame4), arrowSize, arrowSize, false);
        arrowDown[4] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.down_arrow_frame5), arrowSize, arrowSize, false);
        arrowDown[5] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.down_arrow_frame6), arrowSize, arrowSize, false);
        arrowDown[6] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.down_arrow_frame7), arrowSize, arrowSize, false);

        arrowUp = new Bitmap[7]; // 7 frames for each direction
        arrowUp[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.up_arrow_frame1), arrowSize, arrowSize, false);
        arrowUp[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.up_arrow_frame2), arrowSize, arrowSize, false);
        arrowUp[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.up_arrow_frame3), arrowSize, arrowSize, false);
        arrowUp[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.up_arrow_frame4), arrowSize, arrowSize, false);
        arrowUp[4] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.up_arrow_frame5), arrowSize, arrowSize, false);
        arrowUp[5] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.up_arrow_frame6), arrowSize, arrowSize, false);
        arrowUp[6] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.up_arrow_frame7), arrowSize, arrowSize, false);

        arrowLeft = new Bitmap[7]; // 7 images frames for left direction
        arrowLeft[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.left_arrow_frame1), arrowSize, arrowSize, false);
        arrowLeft[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.left_arrow_frame2), arrowSize, arrowSize, false);
        arrowLeft[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.left_arrow_frame3), arrowSize, arrowSize, false);
        arrowLeft[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.left_arrow_frame4), arrowSize, arrowSize, false);
        arrowLeft[4] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.left_arrow_frame5), arrowSize, arrowSize, false);
        arrowLeft[5] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.left_arrow_frame6), arrowSize, arrowSize, false);
        arrowLeft[6] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.left_arrow_frame7), arrowSize, arrowSize, false);



        // Add bitmap images of pacman facing right
        pacmanRight = new Bitmap[totalFrame];
        pacmanRight[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(),R.drawable.pacman_right1), spriteSize, spriteSize, false);
        pacmanRight[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_right2), spriteSize, spriteSize, false);
        pacmanRight[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_right3), spriteSize, spriteSize, false);
        pacmanRight[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_right), spriteSize, spriteSize, false);
        // Add bitmap images of pacman facing down
        pacmanDown = new Bitmap[totalFrame];
        pacmanDown[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_down1), spriteSize, spriteSize, false);
        pacmanDown[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_down2), spriteSize, spriteSize, false);
        pacmanDown[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_down3), spriteSize, spriteSize, false);
        pacmanDown[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_down), spriteSize, spriteSize, false);
        // Add bitmap images of pacman facing left
        pacmanLeft = new Bitmap[totalFrame];
        pacmanLeft[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_left1), spriteSize, spriteSize, false);
        pacmanLeft[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_left2), spriteSize, spriteSize, false);
        pacmanLeft[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_left3), spriteSize, spriteSize, false);
        pacmanLeft[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_left), spriteSize, spriteSize, false);
        // Add bitmap images of pacman facing up
        pacmanUp = new Bitmap[totalFrame];
        pacmanUp[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_up1), spriteSize, spriteSize, false);
        pacmanUp[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_up2), spriteSize, spriteSize, false);
        pacmanUp[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_up3), spriteSize, spriteSize, false);
        pacmanUp[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_up), spriteSize, spriteSize, false);

        ghostBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.ghost), spriteSize, spriteSize, false);
    }


    final short leveldata1[][] = new short[][]{
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {19, 26, 26, 18, 26, 26, 26, 22, 0, 19, 26, 26, 26, 18, 26, 26, 22},
            {21, 0, 0, 21, 0, 0, 0, 21, 0, 21, 0, 0, 0, 21, 0, 0, 21},
            {17, 26, 26, 16, 26, 18, 26, 24, 26, 24, 26, 18, 26, 16, 26, 26, 20},
            {25, 26, 26, 20, 0, 25, 26, 22, 0, 19, 26, 28, 0, 17, 26, 26, 28},
            {0, 0, 0, 21, 0, 0, 0, 21, 0, 21, 0, 0, 0, 21, 0, 0, 0},
            {0, 0, 0, 21, 0, 19, 26, 24, 26, 24, 26, 22, 0, 21, 0, 0, 0},
            {26, 26, 26, 16, 26, 20, 0, 0, 0, 0, 0, 17, 26, 16, 26, 26, 26},
            {0, 0, 0, 21, 0, 17, 26, 26, 26, 26, 26, 20, 0, 21, 0, 0, 0},
            {0, 0, 0, 21, 0, 21, 0, 0, 0, 0, 0, 21, 0, 21, 0, 0, 0},
            {19, 26, 26, 16, 26, 24, 26, 22, 0, 19, 26, 24, 26, 16, 26, 26, 22},
            {21, 0, 0, 21, 0, 0, 0, 21, 0, 21, 0, 0, 0, 21, 0, 0, 21},
            {25, 22, 0, 21, 0, 0, 0, 17, 2, 20, 0, 0, 0, 21, 0, 19, 28}, // "2" in this line is for
            {0, 21, 0, 17, 26, 26, 18, 24, 24, 24, 18, 26, 26, 20, 0, 21, 0}, // pacman's spawn
            {19, 24, 26, 28, 0, 0, 25, 18, 26, 18, 28, 0, 0, 25, 26, 24, 22},
            {21, 0, 0, 0, 0, 0, 0, 21, 0, 21, 0, 0, 0, 0, 0, 0, 21},
            {25, 26, 26, 26, 26, 26, 26, 24, 26, 24, 26, 26, 26, 26, 26, 26, 28},
    };

    private void resetPellets() {
        pelletsLeft = 0;
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 17; j++) {
                leveldata1[i][j] = originalLevel[i][j];  // restaura bit 16
                if ((originalLevel[i][j] & 16) != 0) pelletsLeft++;
            }
        }
        Log.i("info", "¡Pellets repuestos!");
    }


}
