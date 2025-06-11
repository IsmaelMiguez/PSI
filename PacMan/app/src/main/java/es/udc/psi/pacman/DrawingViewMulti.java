package es.udc.psi.pacman;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import java.util.Arrays;

public class DrawingViewMulti extends DrawingView {

    private static final String TAG = "PACMAN_NET";

    private int[] xPosPacman = new int[]{ 8 * blockSize };
    private int[] yPosPacman = new int[]{13 * blockSize };
    private int[] direction  = new int[]{ 4 };
    private int[] nextDir    = new int[]{ 4 };
    private int[] viewDir    = new int[]{ 2 };

    public DrawingViewMulti(Context ctx) { super(ctx); }


    public void resizePlayers(int count) {

        int old = xPosPacman.length;                 // tamaño anterior

        xPosPacman = Arrays.copyOf(xPosPacman, count);
        yPosPacman = Arrays.copyOf(yPosPacman, count);
        direction  = Arrays.copyOf(direction , count);
        nextDir    = Arrays.copyOf(nextDir   , count);
        viewDir    = Arrays.copyOf(viewDir   , count);

        for (int i = old; i < count; i++) {          // sólo las nuevas
            xPosPacman[i] = 8 * blockSize;           // misma casilla “spawn”
            yPosPacman[i] = 13 * blockSize;
            direction [i] = 4;                       // parado
            nextDir  [i] = 4;
            viewDir  [i] = 2;                        // mirando abajo
        }
        lives = Arrays.copyOf(lives, count);
        for (int i = old; i < count; i++) lives[i] = MAX_LIVES;
    }


    public void setNextDirection(int player, int dir) {
        if (player >= 0 && player < nextDir.length) {
            nextDir[player] = dir;
            Log.i(TAG, "[VIEW] nextDir[" + player + "] = " + dir);
        }
    }


    @Override
    public void drawPacman(Canvas c) {
        for (int i = 0; i < xPosPacman.length; i++) {
            super.drawSinglePacman(c, viewDir[i], xPosPacman[i], yPosPacman[i]);
            c.drawText("♥"+lives[i], xPosPacman[i], yPosPacman[i] - 4, textPaint);
        }
    }

    @Override
    public void movePacman(Canvas c) {
        for (int i = 0; i < xPosPacman.length; i++) {
            super.moveSinglePacman(c, i,
                    xPosPacman, yPosPacman,
                    direction,  nextDir,
                    viewDir);

            if (intersects(xPosPacman[i], yPosPacman[i], xPosGhost, yPosGhost)) {


                currentScore = Math.max(0, currentScore - GHOST_DAMAGE);

                if (--lives[i] == 0) {

                    lives[i]       = 0;
                    xPosPacman[i]  = -9999;
                    yPosPacman[i]  = -9999;
                    direction [i]  = 4;
                    nextDir   [i]  = 4;


                    boolean alguienVivo = false;
                    for (int v : lives) if (v > 0) { alguienVivo = true; break; }
                    if (!alguienVivo) { gameOver(); return; }
                    continue;
                }



                xPosPacman[i] = RESPAWN_X * blockSize;
                yPosPacman[i] = RESPAWN_Y * blockSize;
                direction [i] = 4;
                nextDir  [i] = 4;
                viewDir  [i] = 2;
        }}
    }
}
