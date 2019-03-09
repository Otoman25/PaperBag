import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;

public class PaperBag extends JFrame {
    private ArrayList<CannonBall> currentGeneration = new ArrayList<>();
    private ArrayList<CannonBall> successfulEscapees = new ArrayList<>();
    private static Font monoFont = new Font("SanSerif", Font.BOLD, 12);
    static ArrayList<Rectangle> bagSides = new ArrayList<>();
    static final Rectangle BAG_BOUNDS = new Rectangle(225, 211, 55, 79); //Allow for on top of the sides

    private int screenWidth;
    private int screenHeight;
    private  boolean paused = true;
    private int generationNumber = 0;

    private static final int MIN_VELOCITY = 35;
    private static final int MAX_VELOCITY = 70;
    private static final int MIN_DEGREES = 30;
    private static final int MAX_DEGREES = 150;
    private static final int PAIR_PER_GENERATION = 10;
    private static final int MUTATE_LIMIT_VELOCITY = 6;
    private static final int MUTATE_LIMIT_DEGREES = 10;

    private PaperBag(String title){
        super(title);
    }

    private int mutate(int original, int limit){
        Random rand = new Random();
        return original + (rand.nextInt(limit * 2) - limit);
    }

    private void create() {
        Timer timer = new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                physics();
                repaint();

            }
        });

        timer.start();

        createGeneration();

        //Create and set up the window.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenHeight = screenSize.height * 2 / 5;
        screenWidth = screenSize.width * 2 / 7;
        setPreferredSize(new Dimension(screenWidth, screenHeight));

        //Display the window.
        pack();
        setVisible(true);


    }

    private void physics(){
        if(!paused) {
            ArrayList<Integer> toRemove = new ArrayList<>();

            for (CannonBall cannonball : currentGeneration) {
                if (cannonball.isMoving()) {
                    if(cannonball.y > cannonball.startY + 10){
                        cannonball.stop();
                    } else {
                        cannonball.update();
                    }
                }
            }



            boolean allStopped = true;
            int i = 0;
            //if all cballs stationary start next gen
            for(CannonBall cannonball : currentGeneration){
                if(cannonball.isMoving()){
                    allStopped = false;
                } else {
                    if (!CannonBall.isColliding(cannonball.x, cannonball.y)) {
                        successfulEscapees.add(cannonball);
                        toRemove.add(i);
                    }
                }

                i++;
            }

            for(int removalIndex : toRemove){
                currentGeneration.remove(removalIndex);
            }

            if(allStopped){
                paused = true;
                createGeneration();
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics buffer;
        Image image = null;
        image = createImage(getWidth(), getHeight());
        buffer = image.getGraphics();


        buffer.fillRect(0,0, getWidth(), getHeight());

        buffer.setColor(Color.LIGHT_GRAY);
        buffer.fillRect(150,150, 200, 150);

        buffer.setColor(Color.BLACK);
        buffer.fillRect(215,210, 10, 80);
        buffer.fillRect(280,210, 10, 80);
        buffer.fillRect(215,290, 75, 10); // 260 x & 290 y

        buffer.setColor(new Color(255,0,0, 120));
        buffer.fillRect(BAG_BOUNDS.x, BAG_BOUNDS.y, BAG_BOUNDS.width, BAG_BOUNDS.height); // 260 x & 290 y

        bagSides.add(new Rectangle(215, 210, 10, 80));
        bagSides.add(new Rectangle(280, 210, 10, 80));
        bagSides.add(new Rectangle(215, 290, 75, 10));

        addInfo(buffer);
        addStats(buffer);

        for(CannonBall c : currentGeneration){
            c.draw(buffer);
        }

        g.drawImage(image, 0, 0, this);
    }

    private void addStats(Graphics buffer){
        String aliveText = "All Cannonballs";
        String successfulText = "Successful Cannonballs";
        String header = "Vel.   Theta   Coords";

        buffer.setColor(Color.RED);
        buffer.drawString(aliveText, 25, 50);
        buffer.drawString(successfulText, screenWidth - 155, 50);
        buffer.drawString(header, screenWidth - 155, 65);
        buffer.drawString(header, 25, 65);

        int lineStartX = 80;
        int[] colPos = {25, 55, 90};
        int line = 0;
        for(CannonBall c : currentGeneration){
            buffer.drawString(""+c.velocity, colPos[0], lineStartX + (15 * line));
            buffer.drawString(""+c.theta, colPos[1], lineStartX + (15 * line));
            buffer.drawString("(" + c.x + ", " + c.y + ")", colPos[2], lineStartX + (15 * line));
            line++;
        }

        line = 0;
        for(CannonBall c : successfulEscapees){
            buffer.drawString(""+c.velocity, screenWidth - 180 + colPos[0], lineStartX + (15 * line));
            buffer.drawString(""+c.theta, screenWidth - 180 + colPos[1], lineStartX + (15 * line));
            buffer.drawString("(" + c.x + ", " + c.y + ")", screenWidth - 180 + colPos[2], lineStartX + (15 * line));
            line++;
        }
    }

    private void addInfo(Graphics buffer){
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        String cursorString = "X: "+ mousePos.getX() +" | Y: " + mousePos.getY();
        String generationString = "Generation " + generationNumber;

        buffer.setColor(Color.WHITE);
        buffer.setFont(monoFont);
        buffer.drawString(cursorString, 30, getHeight() - 20);
        buffer.drawString(generationString, screenWidth/2 - 60, 50);
    }

    public void createGeneration(){
        Random rand = new Random();
        int baseVel = 0;
        int baseTheta = 0;

        while(!isValidVelocity(baseVel)){
            baseVel = rand.nextInt(MAX_VELOCITY - MIN_VELOCITY) + MIN_VELOCITY;
        }

        while(!isValidTheta(baseTheta)){
            baseTheta = rand.nextInt((MAX_DEGREES - MIN_DEGREES)) + MIN_DEGREES;
        }

        CannonBall newBase = new CannonBall(baseVel, baseTheta);

        if(successfulEscapees.size() >= 2){
            //pick two, crossover and mutate
            int first = rand.nextInt(successfulEscapees.size());
            int second = rand.nextInt(successfulEscapees.size());

            while(first == second){
                second = rand.nextInt(successfulEscapees.size());
            }

            newBase = new CannonBall(successfulEscapees.get(first).velocity, successfulEscapees.get(second).theta);
        }

        currentGeneration.clear();
        currentGeneration.add(newBase);

        for(int i = 1; i < PAIR_PER_GENERATION; i++){
            //populate generation with variance from base pair
            int newVelocity = 0;
            int newTheta = 0;

            while(!isValidVelocity(newVelocity)){
                newVelocity = mutate(currentGeneration.get(0).velocity, MUTATE_LIMIT_VELOCITY);
            }

            while(!isValidTheta(newTheta)){
                newTheta = mutate(currentGeneration.get(0).theta, MUTATE_LIMIT_DEGREES);
            }

            currentGeneration.add(new CannonBall(newVelocity, newTheta));
        }
        successfulEscapees.clear();
        paused = false;
        generationNumber++;
    }

    private boolean isValidVelocity(int velocity){
        return velocity > MIN_VELOCITY && velocity < MAX_VELOCITY;
    }

    private boolean isValidTheta(int theta){
        return theta > MIN_DEGREES && theta < MAX_DEGREES;
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new PaperBag("Fire your way out of a Paper Bag").create());
    }
}

class CannonBall {
    int x;
    int y;
    int startX;
    int startY;
    int velocity;
    int theta;
    private final static double g = -9.81;
    private final static int radius = 10;
    private Instant creationTime;
    private boolean moving = true;
    private static final int CANNONBALL_START_X = 250;
    static final int CANNONBALL_START_Y = 284;

    void stop(){
        moving = false;
    }

    void draw(Graphics g){
        if(isColliding(x, y)){
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.GREEN);
        }
        g.fillOval(x - (radius/2), y - (radius/2), radius, radius);
    }

    CannonBall(int initialVelocity, int theta) {
        this.startX = CANNONBALL_START_X;
        this.startY = CANNONBALL_START_Y;
        this.x = startX;
        this.y = startY;
        this.velocity = initialVelocity;
        this.theta = theta;
        this.creationTime = Instant.now();
    }

    boolean isMoving(){
        return moving;
    }

    void update(){
        Duration eTime = Duration.between(creationTime, Instant.now());

        double elapsedTime = eTime.getSeconds() + (eTime.getNano() * Math.pow(10, -9));

        if(moving) {
             x = startX + (int) (Math.floor(velocity * elapsedTime * Math.cos(Math.toRadians(theta))));
            y = startY + (int) (Math.floor((velocity * -1) * elapsedTime * Math.sin(Math.toRadians(theta))) - (0.5 * g * elapsedTime * elapsedTime));

            if (isCollidingWithSides(x, y) && y <= PaperBag.BAG_BOUNDS.y + PaperBag.BAG_BOUNDS.height) {
                    moving = false;
                }
        }
    }

    static boolean isColliding(int circleX, int circleY){
        Rectangle r = PaperBag.BAG_BOUNDS;
        float closestX = getClosestPoint(circleX, r.x, r.x + r.width);
        float closestY = getClosestPoint(circleY, r.y, r.y + r.height);

        float distanceX = circleX - closestX;
        float distanceY = circleY - closestY;

        float distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);

        return distanceSquared < (radius * radius);

    }

    private static boolean isCollidingWithSides(int circleX, int circleY){
        boolean colliding = false;
        for(Rectangle barrier : PaperBag.bagSides) {
            float closestX = getClosestPoint(circleX, barrier.x, barrier.x + barrier.width);
            float closestY = getClosestPoint(circleY, barrier.y, barrier.y + barrier.height);

            float distanceX = circleX - closestX;
            float distanceY = circleY - closestY;

            float distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);

            if(distanceSquared < (CannonBall.radius)){
                colliding = true;
                break;
            }
        }
        return colliding;
    }

    private static int getClosestPoint(int value, int min, int max){
        int x = value;

        if (x < min) {
            x = min;
        } else if (x > max) {
            x = max;
        }
        return x;
    }
}