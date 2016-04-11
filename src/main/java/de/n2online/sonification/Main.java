package de.n2online.sonification;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.math3.util.FastMath;

public class Main extends Application {
    private Parent root;
    private Scene scene;
    private Canvas screen;
    private GraphicsContext gc;
    private Keyboard keyboard;
    private Visualization viz;
    private Motion mot;

    public Agent agent;
    public Route route;

    @Override
    public void start(Stage stage) throws Exception{
        //Graphics Setup

        root = FXMLLoader.load(getClass().getResource("/Main.fxml"));
        stage.setTitle("Sonification");
        scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();

        AnchorPane monitor = (AnchorPane) scene.lookup("#monitor");

        screen = (Canvas)scene.lookup("#screen");
        screen.widthProperty().bind(monitor.widthProperty());
        screen.heightProperty().bind(monitor.heightProperty());
        gc = screen.getGraphicsContext2D();

        viz = new Visualization(gc);
        mot = new Motion();

        //Event hooks

        keyboard = new Keyboard();

        scene.addEventFilter(KeyEvent.KEY_PRESSED, evt -> keyboard.registerKeyDown(evt));
        scene.addEventFilter(KeyEvent.KEY_RELEASED, evt -> keyboard.registerKeyUp(evt));


        //Data Setup

        agent = new Agent(32, 32, Math.toRadians(45));
        route = new Route();
        for (int i = 0; i < 5; i++) {
            route.addWaypoint(new Waypoint(FastMath.random()*screen.getWidth(), FastMath.random()*screen.getHeight()));
        }

        //##### LET IT RUN #####

        //25FPS wanted :)
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000/25), evt -> viz.paint(agent, route)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        //Motion handler
        AnimationTimer physics = new AnimationTimer() {
            private long last = 0;
            private long deltaSum = 0;
            final private long minDelta = 10;

            @Override
            public void handle(long now) {
                now /= 1000000; //calculations in milliseconds

                if (last != 0) {
                    //accumulate delta until time for action
                    deltaSum += now - last;
                    if (deltaSum >= minDelta) {
                        double partial = deltaSum / 1000.0;

                        mot.handle(partial, keyboard, agent, route);

                        //delta "used up"
                        deltaSum = 0;
                    }
                }

                last = now;
            }
        };
        physics.start();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void log(String line) {
        TextArea logger = (TextArea) scene.lookup("#log");
        logger.appendText("\n" + line);
    }
}
