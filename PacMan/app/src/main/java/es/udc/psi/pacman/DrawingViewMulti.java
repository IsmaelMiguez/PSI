package es.udc.psi.pacman;

import android.content.Context;
import android.graphics.Canvas;

/** Versión de 2 jugadores: hereda toda la lógica y amplía arrays */
public class DrawingViewMulti extends DrawingView {

    // Sobrescribe los atributos simples por arrays (solo los necesarios)
    private int[] xPosPacman  = new int[2];
    private int[] yPosPacman  = new int[2];
    private int[] direction   = new int[]{4,4};
    private int[] nextDir     = new int[]{4,4};
    private int[] viewDir     = new int[]{2,2};

    public DrawingViewMulti(Context ctx) { super(ctx); }

    /* ===== API pública para ExtendedPlayActivity ===== */
    public void setNextDirection(int player, int dir) {
        if (player>=0 && player<2) nextDir[player] = dir;
    }

    /* ====== Sobrescribe los métodos que mueven/dibujan ====== */

    @Override public void drawPacman(Canvas c) {
        for (int i=0;i<2;i++) super.drawSinglePacman(
                c, viewDir[i], xPosPacman[i], yPosPacman[i]);
    }

    @Override public void movePacman(Canvas c) {
        for (int i=0;i<2;i++) super.moveSinglePacman(
                c,i,xPosPacman,yPosPacman,direction,nextDir,viewDir);
    }

}
