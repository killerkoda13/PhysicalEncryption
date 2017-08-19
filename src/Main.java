/**
 * Created by alex on 8/17/2017.
 */
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;


public class Main extends JFrame {

    /** 45 px per meter */
    public static final double SCALE = 45.0;

    public static final double NANO_TO_BASE = 1.0e9;


    public static class GameObject extends Body {
        protected Color color;


        public GameObject() {

            //Remove later on. I like colors. :)
            this.color = new Color(
                    (float)Math.random() * 0.5f + 0.5f,
                    (float)Math.random() * 0.5f + 0.5f,
                    (float)Math.random() * 0.5f + 0.5f);
        }

        public void render(Graphics2D g) {
            AffineTransform ot = g.getTransform();

            AffineTransform lt = new AffineTransform();
            lt.translate(this.transform.getTranslationX() * SCALE, this.transform.getTranslationY() * SCALE);
            lt.rotate(this.transform.getRotation());
            g.transform(lt);
            for (BodyFixture fixture : this.fixtures) {
                Convex convex = fixture.getShape();
                Graphics2DRenderer.render(g, convex, SCALE, color);
            }
            g.setTransform(ot);
        }
    }

    protected Canvas canvas;

    protected World world;

    protected boolean stopped;

    protected long last;

    public Main() {
        super("PhysicalEncrypt Window");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.addWindowListener(new WindowAdapter() {
            /* (non-Javadoc)
             * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
             */
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
                super.windowClosing(e);
            }
        });

        Dimension size = new Dimension(800, 600);

        this.canvas = new Canvas();
        this.canvas.setPreferredSize(size);
        this.canvas.setMinimumSize(size);
        this.canvas.setMaximumSize(size);

        this.add(this.canvas);

        this.setResizable(false);

        this.pack();

        this.stopped = false;

        this.initializeWorld();
    }

    public static int generateRandom(int i) {
        Random random = new Random();

        int s = random.nextInt(i);

        if (s == 0)
        {
            s = generateRandom(i);
            return s;
        }
        return s;
    }

    protected void initializeWorld() {

        Random random = new Random();
        this.world = new World();

        Rectangle floorRectBottom = new Rectangle(100, 1);
        GameObject Bottom = new GameObject();
        Bottom.addFixture(new BodyFixture(floorRectBottom));
        Bottom.setMass(MassType.INFINITE);
        // move the floor down a bit
        Bottom.translate(1, 0.1);
        this.world.addBody(Bottom);
        for(int i = 0; i<20;i++){

            Rectangle floorRect = new Rectangle(generateRandom(15), generateRandom(5));
            GameObject floor = new GameObject();
            floor.addFixture(new BodyFixture(floorRect));
            floor.setMass(MassType.INFINITE);
            // move the floor down a bit
            floor.translate(random.nextInt(20), random.nextInt(5)*-1);
            this.world.addBody(floor);
        }


        for(int i = 0; i<25;i++){
            Polygon polyShape = Geometry.createUnitCirclePolygon(random.nextInt(20)+3, 1.0);
            GameObject polygon = new GameObject();
            polygon.addFixture(polyShape);
            polygon.setMass(MassType.NORMAL);
            polygon.translate(-2.5, 2.0);
            // set the angular velocity
            polygon.setAngularVelocity(Math.toRadians(-20.0));
            polygon.applyForce(new Vector2(random.nextInt(800), random.nextInt(800)));
            this.world.addBody(polygon);
        }
    }

    public void worldLoop(){

        for(Body body : this.world.getBodies()){

            if(body.getAngularVelocity() ==0){
                if(body.getMass().getType().equals(MassType.INFINITE)){}else {
                    System.out.println(body.getLocalCenter().x);
                }
            }
        }
    }

    public void start() {
        this.last = System.nanoTime();
        this.canvas.setIgnoreRepaint(true);
        this.canvas.createBufferStrategy(2);
        Thread thread = new Thread() {
            public void run() {
                while (!isStopped()) {
                    gameLoop();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    protected void gameLoop() {
        worldLoop();

        Graphics2D g = (Graphics2D)this.canvas.getBufferStrategy().getDrawGraphics();
        AffineTransform yFlip = AffineTransform.getScaleInstance(1, -1);
        AffineTransform move = AffineTransform.getTranslateInstance(400, -300);
        g.transform(yFlip);
        g.transform(move);

        this.render(g);

        g.dispose();

        BufferStrategy strategy = this.canvas.getBufferStrategy();
        if (!strategy.contentsLost()) {
            strategy.show();
        }

        Toolkit.getDefaultToolkit().sync();

        long time = System.nanoTime();
        long diff = time - this.last;
        this.last = time;
        double elapsedTime = diff / NANO_TO_BASE;
        this.world.update(elapsedTime*20);
    }

    protected void render(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(-400, -300, 1200, 1000);
        g.translate(0.0, -1.0 * SCALE);
        for (int i = 0; i < this.world.getBodyCount(); i++) {
            GameObject go = (GameObject) this.world.getBody(i);
            go.render(g);
        }
    }

    public synchronized void stop() {
        this.stopped = true;
    }

    public synchronized boolean isStopped() {
        return this.stopped;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        Main window = new Main();
        window.setSize(1200,1000);
        window.setVisible(true);
        window.start();
    }
}