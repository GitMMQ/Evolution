package gamestates;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import entities.Asteroid;
import entities.Bullet;
import entities.Food;
import entities.Ship;
import managers.GameStateManager;
import managers.Settings;

import java.util.ArrayList;
import java.util.List;

public class PlayState extends gamestates.GameState {
    private ShapeRenderer sr;

    private List<Ship> ships;
    private List<Ship> storedShips;
    private List<List<Bullet>> bullets;
    private List<Asteroid> asteroids;
    private List<Food> food;

    private int numShips;
    private int numAsteroids;
    private int numFood;

    private int GENERATION_COUNTER;
    private int ELITISM;

    public PlayState(GameStateManager gsm) {
        super(gsm);
    }

    private void setElitism(int elitism) {
        if (elitism > numShips)
            ELITISM = numShips;
        else if (elitism < 0)
            ELITISM = 0;
        else
            ELITISM = elitism;
    }

    public void init() {
        sr = new ShapeRenderer();

        ships = new ArrayList<>();
        storedShips = new ArrayList<>();
        bullets = new ArrayList<>();
        asteroids = new ArrayList<>();
        food = new ArrayList<>();

        numShips = Settings.NUMBER_OF_SHIPS;

        setElitism(Settings.ELITISM);

        spawnShips();

        if (Settings.DEBUG) {
            printGenerationHeader();
            printPopulation();
        }

        numAsteroids = Settings.NUMBER_OF_ASTEROIDS;
        numFood = Settings.NUMBER_OF_FOOD;
    }

    private void spawnSingleShip() {
        bullets.add(new ArrayList<>());
        ships.add(new Ship(bullets.get(bullets.size() - 1)));
    }

    private void spawnShips() {
        while (ships.size() < numShips)
            spawnSingleShip();
    }

    private void storeAndCleanShips() {
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            if (s.shouldRemove()) {
                storedShips.add(new Ship(s, s.getDistanceToDodge(), s.getRotationSpeed()));
                ships.remove(i);
                bullets.remove(i);
                i--;
            }
        }
    }

    /**
     * Creates a mutant based on a Ship.
     *
     * @param oldShip Mutant will be based on this ship
     */
    private void createMutation(Ship oldShip) {
        spawnSingleShip();
        double m = MathUtils.random(0, 100);
        int min_mutation_probability = Settings.MUTATION_PROBABILITY;
        int med_mutation_probability = Settings.MUTATION_PROBABILITY + (100 - Settings.MUTATION_PROBABILITY / 2);
        float mutation_variation = Settings.MUTATION_VARIATION;
        double distance_to_dodge = oldShip.getDistanceToDodge();
        float rotation_speed = oldShip.getRotationSpeed();
        float max_speed = oldShip.getMaxSpeed();
        float acceleration = oldShip.getAcceleration();
        float deceleration = oldShip.getDeceleration();
        Ship mutantShip = ships.get(ships.size() - 1);

        if (m > min_mutation_probability && m < med_mutation_probability) {
            mutantShip.setDistanceToDodge(distance_to_dodge + distance_to_dodge * mutation_variation);
        } else if (m >= med_mutation_probability) {
            mutantShip.setDistanceToDodge(distance_to_dodge - distance_to_dodge * mutation_variation);
        } else
            mutantShip.setDistanceToDodge(distance_to_dodge);
        if (mutantShip.getDistanceToDodge() < 1)
            mutantShip.setDistanceToDodge(1);

        m = MathUtils.random(0, 100);
        if (m > min_mutation_probability && m < med_mutation_probability) {
            mutantShip.setRotationSpeed(rotation_speed + rotation_speed * mutation_variation);
        } else if (m >= med_mutation_probability) {
            mutantShip.setRotationSpeed(rotation_speed - rotation_speed * mutation_variation);
        } else
            mutantShip.setRotationSpeed(rotation_speed);

        m = MathUtils.random(0, 100);
        if (m > min_mutation_probability && m < med_mutation_probability) {
            mutantShip.setMaxSpeed(max_speed + max_speed * mutation_variation);
        } else if (m >= med_mutation_probability) {
            mutantShip.setMaxSpeed(max_speed - max_speed * mutation_variation);
        } else
            mutantShip.setMaxSpeed(max_speed);

        m = MathUtils.random(0, 100);
        if (m > min_mutation_probability && m < med_mutation_probability) {
            mutantShip.setAcceleration(acceleration + acceleration * mutation_variation);
        } else if (m >= med_mutation_probability) {
            mutantShip.setAcceleration(acceleration - acceleration * mutation_variation);
        } else
            mutantShip.setAcceleration(acceleration);

        m = MathUtils.random(0, 100);
        if (m > min_mutation_probability && m < med_mutation_probability) {
            mutantShip.setDeceleration(deceleration + deceleration * mutation_variation);
        } else if (m >= med_mutation_probability) {
            mutantShip.setDeceleration(deceleration - deceleration * mutation_variation);
        } else
            mutantShip.setDeceleration(deceleration);
    }

    private List<Double> fitnessFunction() {
        List<Double> lifeTimeList = new ArrayList<>();
        double sumLifes = 0;
        for (Ship s : storedShips) {
            double fitness = Math.pow(s.getLifeTime(), 4);
            lifeTimeList.add(fitness);
            sumLifes += fitness;
        }

        lifeTimeList.set(0, lifeTimeList.get(0) / sumLifes);
        for (int i = 1; i < lifeTimeList.size(); i++) {
            lifeTimeList.set(i, lifeTimeList.get(i - 1) + (lifeTimeList.get(i) / sumLifes));
        }

        return lifeTimeList;
    }

    private void elitism() {
        for (int i = (storedShips.size() - 1); i >= (storedShips.size() - ELITISM - 1); i--) {
            bullets.add(new ArrayList<>());
            ships.add(new Ship(storedShips.get(i)));
        }
    }

    private void evolution() {
        ships = new ArrayList<>();
        bullets = new ArrayList<>();
        int[] childOfWho = new int[numShips];

        elitism();

        List<Double> lifeTimeList = fitnessFunction();
        for (int i = 0; i < (numShips - ELITISM); i++) {
            double f = Math.random();
            int j = 0;
            while (f > lifeTimeList.get(j))
                j++;
            childOfWho[j] += 1;
            Ship oldShip = storedShips.get(j);
            createMutation(oldShip);
        }

        if (Settings.DEBUG) {
            printMostChild(childOfWho);
            System.out.println();
            printGenerationHeader();
            printPopulation();
        }

        storedShips = new ArrayList<>();
    }

    private void updateShips(float dt) {
        for (Ship s : ships) {
            s.nearest(food, asteroids);
            s.update(dt);
        }


        storeAndCleanShips();

        if (ships.isEmpty()) {
            if (Settings.DEBUG)
                printBestShips();
            evolution();
        }
    }

    /**
     * Creates a new Asteroid.
     */
    private void spawnSingleAsteroid() {
        asteroids.add(new Asteroid(Asteroid.LARGE));
    }

    private void spawnAsteroids() {
        while (asteroids.size() < numAsteroids)
            spawnSingleAsteroid();
    }

    private void splitAsteroid(Asteroid a) {
        if (a.getType() == Asteroid.LARGE) {
            asteroids.add(new Asteroid(a.getX(), a.getY(), Asteroid.MEDIUM));
            asteroids.add(new Asteroid(a.getX(), a.getY(), Asteroid.MEDIUM));
        } else if (a.getType() == Asteroid.MEDIUM) {
            asteroids.add(new Asteroid(a.getX(), a.getY(), Asteroid.SMALL));
            asteroids.add(new Asteroid(a.getX(), a.getY(), Asteroid.SMALL));
        }
    }

    private void spawnSingleFood() {
        food.add(new Food());
    }

    private void spawnFood() {
        while (food.size() < numFood)
            spawnSingleFood();
    }

    public void update(float dt) {
        // get user input
        handleInput();

        updateShips(dt);

        // update ship bullets
        for (List<Bullet> bullets_of_ship : bullets)
            for (int i = 0; i < bullets_of_ship.size(); i++) {
                bullets_of_ship.get(i).update(dt);
                if (bullets_of_ship.get(i).shouldRemove()) {
                    bullets_of_ship.remove(i);
                    i--;
                }
            }

        // update asteroids
        for (int i = 0; i < asteroids.size(); i++) {
            Asteroid a = asteroids.get(i);
            a.update(dt);
            if (a.shouldRemove()) {
                asteroids.remove(i);
                i--;
            }
        }

        // update food
        for (int i = 0; i < food.size(); i++) {
            Food f = food.get(i);
            f.update(dt);
            if (f.shouldRemove()) {
                food.remove(i);
                i--;
            }
        }

        checkCollisions();
        spawnAsteroids();
        spawnFood();
    }

    /**
     * Checks if any Ship collided with a Asteroid.
     */
    private void checkShipsAsteroidsCollisions() {
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            for (int j = 0; j < asteroids.size(); j++) {
                Asteroid a = asteroids.get(j);
                if (a.intersects(s)) {
                    ships.remove(i);
                    bullets.remove(i);
                    i--;
                    asteroids.remove(j);
                    splitAsteroid(a);
                    break;
                }
            }
        }
    }

    /**
     * Check if any Ship collided with a Food.
     */
    private void checkShipsFoodCollisions() {
        for (Ship s : ships)
            for (int j = 0; j < food.size(); j++) {
                Food f = food.get(j);
                if (s.contains(f.getX(), f.getY())) {
                    s.setLifeTimer(-2);
                    s.setLifeTime(2);
                    food.remove(j);
                    break;
                }
            }
    }

    /**
     * Check if any Ship collided with a Bullet.
     */
    private void checkShipsBulletsCollisions() {
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            for (int j = 0; j < bullets.size(); j++)
                if (i != j) {
                    List<Bullet> enemy_bullets = bullets.get(j);
                    for (int k = 0; k < enemy_bullets.size(); k++) {
                        Bullet b = enemy_bullets.get(k);
                        if (s.contains(b.getX(), b.getY())) {
                            enemy_bullets.remove(b);
                            ships.remove(i);
                            bullets.remove(i);
                            i--;
                            j--;
                            break;
                        }
                    }
                }
        }
    }

    /**
     * Check if any Bullet collided with an Asteroid.
     */
    private void checkBulletsAsteroidsCollisions() {
        for (List<Bullet> bullets_flying : bullets)
            for (int i = 0; i < bullets_flying.size(); i++) {
                Bullet b = bullets_flying.get(i);
                for (int j = 0; j < asteroids.size(); j++) {
                    Asteroid a = asteroids.get(j);
                    if (a.contains(b.getX(), b.getY())) {
                        bullets_flying.remove(b);
                        i--;
                        asteroids.remove(a);
                        splitAsteroid(a);
                        break;
                    }
                }
            }
    }

    private void checkCollisions() {
        checkShipsBulletsCollisions();
        checkShipsFoodCollisions();
        checkShipsAsteroidsCollisions();
        checkBulletsAsteroidsCollisions();
    }

    private void printBestShips() {
        int i = 0;
        if (ELITISM <= 0) {
            System.out.println("\tBest Ships:");
            while (i < 3 && i < numShips) {
                i++;
                System.out.println("\t\t" + i + ": " + storedShips.get(storedShips.size() - i) + "; ");
            }
        } else {
            System.out.println("\tElite Ships:");
            while (i < ELITISM) {
                i++;
                System.out.println("\t\t" + i + ": " + storedShips.get(storedShips.size() - i) + "; ");
            }
        }
    }

    private void printMostChild(int[] numberOfChildren) {
        System.out.println("\tChildren:");
        System.out.print("\t\t");
        boolean found = false;
        for (int i = 0; i < numberOfChildren.length; i++)
            if (numberOfChildren[i] > 2) {
                found = true;
                System.out.print("[" + i + ": " + numberOfChildren[i] + "]; ");
            }
        if (!found)
            System.out.print("All have less than 2 children.");
        System.out.println();
    }

    private void printPopulation() {
        System.out.println("\tPopulation:");
        System.out.print("\t\t");
        for (Ship ship : ships)
            System.out.print(ship + " ");
        System.out.println();
    }

    private void printGenerationHeader() {
        System.out.println("Generation " + GENERATION_COUNTER++ + ":");
    }

    private void drawShips() {
        for (Ship ship : ships)
            ship.draw(sr);
    }

    private void drawBullets() {
        for (List<Bullet> bullets_flying : bullets)
            for (Bullet bullet : bullets_flying)
                bullet.draw(sr);
    }

    private void drawAsteroids() {
        for (Asteroid asteroid : asteroids)
            asteroid.draw(sr);
    }

    private void drawFood() {
        for (Food f : food)
            f.draw(sr);
    }

    public void draw() {
        drawShips();
        drawBullets();
        drawAsteroids();
        drawFood();
    }

    public void handleInput() {
        /*
        ships.get(0).setLeft(GameKeys.isDown(GameKeys.LEFT));
        ships.get(0).setRight(GameKeys.isDown(GameKeys.RIGHT));
        ships.get(0).setUp(GameKeys.isDown(GameKeys.UP));
        if (GameKeys.isPressed(GameKeys.SPACE)) {
            ships.get(0).shoot();
        }
        */
    }

    public void dispose() {
    }
}
