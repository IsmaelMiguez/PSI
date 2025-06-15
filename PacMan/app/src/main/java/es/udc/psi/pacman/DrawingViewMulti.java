package es.udc.psi.pacman;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import java.util.Arrays;

public class DrawingViewMulti extends DrawingView {

    private static final String TAG = "PACMAN_NET";

    private int[] xPosPacman = new int[]{8 * blockSize};
    private int[] yPosPacman = new int[]{13 * blockSize};
    private int[] direction = new int[]{4};
    private int[] nextDir = new int[]{4};
    private int[] viewDir = new int[]{2};

    public DrawingViewMulti(Context context) {
        super(context);
        // Cambiar a modo cooperativo
        gameSession = new GameSession("cooperativo", 1);
    }


    public void resizePlayers(int count) {

        int old = xPosPacman.length;                 // tamaño anterior

        xPosPacman = Arrays.copyOf(xPosPacman, count);
        yPosPacman = Arrays.copyOf(yPosPacman, count);
        direction = Arrays.copyOf(direction, count);
        nextDir = Arrays.copyOf(nextDir, count);
        viewDir = Arrays.copyOf(viewDir, count);

        for (int i = old; i < count; i++) {          // sólo las nuevas
            xPosPacman[i] = 8 * blockSize;           // misma casilla “spawn”
            yPosPacman[i] = 13 * blockSize;
            direction[i] = 4;                       // parado
            nextDir[i] = 4;
            viewDir[i] = 2;                        // mirando abajo
        }
        lives = Arrays.copyOf(lives, count);
        for (int i = old; i < count; i++) lives[i] = MAX_LIVES;

        // Actualizar jugadores en la sesión
        // Por simplicidad, agregamos jugadores genéricos para multijugador
        if (gameSession != null) {
            for (int i = 1; i < count; i++) { // Empezar desde 1 porque el 0 ya está agregado
                gameSession.addPlayer("player_" + i, "Jugador " + (i + 1), 0, 3);
            }
        }
    }

    @Override
    protected void gameOver() {
        // Actualizar puntuaciones de todos los jugadores
        if (gameSession != null) {
            for (int i = 0; i < Math.min(xPosPacman.length, lives.length); i++) {
                if (i == 0) {
                    gameSession.updateMainPlayerScore(currentScore, lives[i]);
                } else {
                    gameSession.updatePlayerScore("player_" + i, currentScore, lives[i]);
                }
            }
            
            // Verificar si todos los jugadores perdieron
            boolean allDead = true;
            for (int life : lives) {
                if (life > 0) {
                    allDead = false;
                    break;
                }
            }
            
            // Terminar el juego - si allDead es true, la partida NO está completada
            gameSession.endGame(!allDead);
        }
        
        super.gameOver();
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
            c.drawText("♥" + lives[i], xPosPacman[i], yPosPacman[i] - 4, textPaint);
        }
    }

    @Override
    public void movePacman(Canvas c) {
        for (int i = 0; i < xPosPacman.length; i++) {
            super.moveSinglePacman(c, i,
                    xPosPacman, yPosPacman,
                    direction, nextDir,
                    viewDir);

            if (intersects(xPosPacman[i], yPosPacman[i], xPosGhost, yPosGhost)) {


                currentScore = Math.max(0, currentScore - GHOST_DAMAGE);

                if (--lives[i] == 0) {

                    lives[i] = 0;
                    xPosPacman[i] = -9999;
                    yPosPacman[i] = -9999;
                    direction[i] = 4;
                    nextDir[i] = 4;


                    boolean alguienVivo = false;
                    for (int v : lives)
                        if (v > 0) {
                            alguienVivo = true;
                            break;
                        }
                    if (!alguienVivo) {
                        gameOver();
                        return;
                    }
                    continue;
                }


                xPosPacman[i] = RESPAWN_X * blockSize;
                yPosPacman[i] = RESPAWN_Y * blockSize;
                direction[i] = 4;
                nextDir[i] = 4;
                viewDir[i] = 2;

            }
        }
        int bestIdx = -1;
        int bestDst = Integer.MAX_VALUE;

        for (int i = 0; i < xPosPacman.length; i++) {
            if (lives[i] <= 0) continue;
            int dist = Math.abs(xPosGhost - xPosPacman[i])
                    + Math.abs(yPosGhost - yPosPacman[i]);
            if (dist < bestDst) {
                bestDst = dist;
                bestIdx = i;
            }
        }

        if (bestIdx != -1) {
            super.xPosPacman = xPosPacman[bestIdx];
            super.yPosPacman = yPosPacman[bestIdx];
        }
    }
}
