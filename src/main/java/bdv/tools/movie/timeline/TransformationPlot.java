package bdv.tools.movie.timeline;


import bdv.tools.movie.preview.MovieFrameInst;
import bdv.tools.movie.serilizers.MovieFramesSerializer;
import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import javax.swing.*;
import java.awt.geom.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static bdv.tools.movie.MovieProducer.accel;

//Extends JPanel class
public class TransformationPlot extends JPanel {

    private final List<MovieFrameInst> movieFrames;

    //    int[] cord = {65, 20, 40, 80};
    int marg = 50;

    public TransformationPlot(List<MovieFrameInst> movieFrames) {
        this.movieFrames = movieFrames;
    }

    protected void paintComponent(Graphics grf) {
        super.paintComponent(grf);
        Graphics2D graph = (Graphics2D) grf;

        //Sets the value of a single preference for the rendering algorithms.
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // get width and height
        int width = getWidth();
        int height = getHeight();

        // draw graph
        graph.draw(new Line2D.Double(marg, height - marg, width - marg, height - marg));


        List<Double> similarity = new ArrayList<>();
        for (int k = 1; k < movieFrames.size(); ++k) {
            AffineTransform3D transformsStart = movieFrames.get(k - 1).getTransform();
            AffineTransform3D last = transformsStart;
            AffineTransform3D transformsEnd = movieFrames.get(k).getTransform();
            int frames = movieFrames.get(k).getFrames();
            int accel = movieFrames.get(k).getAccel();
            for (int n = 0; n < movieFrames.get(k).getFrames(); ++n) {

                final SimilarityTransformAnimator animator = new SimilarityTransformAnimator(
                        transformsStart,
                        transformsEnd,
                        0,
                        0,
                        0);

                final AffineTransform3D tkd = animator.get(accel((double) n / (double) frames, accel));
                double sim = getSimilarity(last, tkd);
                similarity.add(sim);
                last = tkd;
            }
        }
        Double max = Collections.max(similarity);
        System.out.println("Max : " + max);
        //find value of x and scale to plot points
        double x = (double) (width - 2 * marg) / (similarity.size() - 1);
        double scale = (double) (height - 2 * marg) / max;

        //set color for points
        graph.setPaint(Color.RED);

        // set points to the graph
        for (int i = 0; i < similarity.size(); i++) {
            double x1 = marg + i * x;
            double y1 = height - marg - scale * similarity.get(i);
            graph.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
        }
    }

    private double getSimilarity(AffineTransform3D last, AffineTransform3D tkd) {

        AffineTransform3D diff = tkd.copy();
        diff.concatenate(last.inverse());
        double all = 0;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                all += diff.get(i, j);
        return all;
    }


    //main() method start
    public static void main(String args[]) throws FileNotFoundException {
        String jsonTransformations = "/Users/zouinkhim/Desktop/test2.json";
        List<MovieFrameInst> movieFrames = MovieFramesSerializer.getFrom(new File(jsonTransformations));
        JFrame frame = new JFrame();
        frame.add(new TransformationPlot(movieFrames));
        frame.setSize(400, 400);
        frame.setLocation(200, 200);
        frame.setVisible(true);

        jsonTransformations = "/Users/zouinkhim/Desktop/test.json";
        List<MovieFrameInst> movieFrames2 = MovieFramesSerializer.getFrom(new File(jsonTransformations));
        JFrame frame2 = new JFrame();
        frame2.add(new TransformationPlot(movieFrames2));
        frame2.setSize(400, 400);
        frame2.setLocation(200, 200);
        frame2.setVisible(true);
    }
}