package de.n2online.sonification;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

public class Visualization {
    GraphicsContext gc;
    private Image character;

    public Visualization(GraphicsContext context) {
        gc = context;
        character = new Image(getClass().getResourceAsStream("/agent.png"));
    }

    private void drawCenteredImage(Image image, double center_x, double center_y, double angle) {
        //to forget transformation later on, push and pull gc state from stack
        gc.save();
        Rotate matrix = new Rotate(Math.toDegrees(angle), center_x, center_y);
        gc.setTransform(matrix.getMxx(), matrix.getMyx(), matrix.getMxy(), matrix.getMyy(), matrix.getTx(), matrix.getTy());
        double x = center_x - image.getWidth() / 2;
        double y = center_y - image.getHeight() / 2;
        gc.drawImage(image, x, y);
        gc.restore();
    }

    public void paint(Agent agent) {
        Canvas screen = gc.getCanvas();

        gc.setFill(Color.GREY);
        gc.fillRect(0, 0, screen.getWidth(), screen.getHeight());
        gc.setFill(Color.BLUE);
        gc.fillRect(screen.getWidth()*Math.random(),screen.getHeight()*Math.random(),50,50);

        drawCenteredImage(character, agent.pos.getX(), agent.pos.getY(), agent.getOrientation());
    }

}
