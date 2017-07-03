package entities;

import managers.Game;

public class SpaceObject {
    float x;
    float y;

    float dx;
    float dy;

    float orientation;
    float speed;
    float rotationSpeed;

    int width;
    int height;

    float[] shapex;
    float[] shapey;

    public float getx() {
        return x;
    }

    public float gety() {
        return y;
    }

    private float[] getShapex() {
        return shapex;
    }

    private float[] getShapey() {
        return shapey;
    }

    /**
     * Se bater na borda passa para o outro lado.
     */
    void wrap() {
        if (x < 0)
            x = Game.WIDTH;
        if (x > Game.WIDTH)
            x = 0;
        if (y < 0)
            y = Game.HEIGHT;
        if (y > Game.HEIGHT)
            y = 0;
    }

    /**
     * Verifica se dois SpaceObject colidem.
     *
     * @param other SpaceObject a verificar
     * @return true se colidem, false senao
     */
    public boolean intersects(SpaceObject other) {
        float[] sx = other.getShapex();
        float[] sy = other.getShapey();
        for(int i = 0; i <sx.length; i++) {
            if(contains(sx[i], sy[i]))
                return true;
        }
        return false;
    }

    public boolean contains(float x, float y) {
        boolean contain = false;
        for (int i = 0, j = shapex.length - 1; i < shapex.length; j = i++)
            if ((shapey[i] > y) != (shapey[j] > y) && (x < (shapex[j] - shapex[i]) * (y - shapey[i]) / (shapey[j] - shapey[i]) + shapex[i]))
                contain = !contain;
        return contain;
    }
}
