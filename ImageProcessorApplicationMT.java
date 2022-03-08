package com.kcl.osc.imageprocessor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

public class ImageProcessorApplicationMT extends Application {
    /**
     * Change this constant to change the filtering operation. Options are
     * IDENTITY, EDGE, BLUR, SHARPEN, EMBOSS, EDGE, GREY
     */
    private static final String filter = "GREY";
    private static final int SLICE_SIZE = 126;

//    private ThreadManager threadManager;
//
//    private ArrayList<ImageProcessorMT> threads;

    /**
     * Set this boolean to false if you do NOT wish the new images to be
     * saved after processing.
     */
    private static final boolean saveNewImages = true;

    @Override
    public void start(Stage stage) throws Exception{

        // gets the images from the 'img' folder.
        ArrayList<ImageProcessorApplicationMT.ImageInfo> images = findImages();


        System.out.println("Working.");
        final long startTime = System.nanoTime();
        ThreadManager threadManager = new ThreadManager(1);


        for(int i = 0; i < images.size(); i++) {
            ImageProcessorMT ip = new ImageProcessorMT(images.get(i).getImage(),filter,saveNewImages,images.get(i).getFilename() + "_filtered.png");
            threadManager.addImageProcessor(ip);
        }
        threadManager.start();
        threadManager.join();

        System.out.println("Done.");
        final long duration = System.nanoTime() - startTime;
        System.out.println(duration);

        // Kill this application
        Platform.exit();
    }

    /**
     * This method expects all of the images that are to be processed to
     * be in a folder called img that is in the current working directory.
     * In Eclipse, for example, this means the img folder should be in the project
     * folder (alongside src and bin).
     * @return Info about the images found in the folder.
     */
    private ArrayList<ImageProcessorApplicationMT.ImageInfo> findImages() {
        ArrayList<ImageProcessorApplicationMT.ImageInfo> images = new ArrayList<ImageProcessorApplicationMT.ImageInfo>();
        Collection<File> files = listFileTree(new File("img"));
        for (File f: files) {
            if (f.getName().startsWith(".")) {
                continue;
            }
            Image img = new Image("file:" + f.getPath());
            ImageProcessorApplicationMT.ImageInfo info = new ImageProcessorApplicationMT.ImageInfo(img, f.getName());
            images.add(info);
        }
        return images;
    }

    private static Collection<File> listFileTree(File dir) {
        Set<File> fileTree = new HashSet<File>();
        if (dir.listFiles() == null)
            return fileTree;
        for (File entry : dir.listFiles()) {
            if (entry.isFile())
                fileTree.add(entry) /* */;
            else
                fileTree.addAll(listFileTree(entry));
        }
        return fileTree;
    }

    private void saveImage(Image image, String fileName, ArrayList<ImageProcessorMT> threads) {
        WritableImage wimg = new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());
        PixelWriter pw = wimg.getPixelWriter();

        int count = 0;
        int sliceToGet = 0;
        for(int i = 0; i < image.getHeight(); i++) {
            Color[][]pixel = threads.get(sliceToGet).getOutputPixel();
            for(int j = 0; j < image.getWidth(); j++) {
                pw.setColor(i,j,pixel[count][j]);
            }
            if(count == SLICE_SIZE - 1) {
                count = 0;
                sliceToGet ++;
            }
            else {
                count++;
            }
        }

        File newFile = new File(fileName);

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(wimg, null), "png", newFile);
        } catch (Exception s) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Gets the pixel data from the image but does
     * NOT add a border.
     * @return The pixel data.
     */
    private Color[][] getPixelData(Image image, int rowToCopy) {
        PixelReader pr = image.getPixelReader();
        Color[][] pixels = new Color[1][(int)image.getHeight()];
        for (int i = 0; i < image.getHeight(); i++) {
//            for (int j = 0; j < image.getHeight(); j++) {
//                pixels[i][j] = pr.getColor(i, j);
//            }
            pixels[0][i] = pr.getColor(rowToCopy,i);
        }

        return pixels;
    }

    /**
     * This method adds a border to the image and colors the whole image as grey.
     * @param image
     * @return pixels which is a grey colored image with a border.
     */
    private Color[][] getGreyImage(Image image) {
        PixelReader pr = image.getPixelReader();
        Color[][] pixels = new Color[(int) image.getWidth() + 2][(int) image.getHeight() + 2];

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels.length; j++) {
                pixels[i][j] = new Color(0.5, 0.5, 0.5, 1.0);
            }
        }

        return pixels;
    }

    private Color[][] getPixelDataExtendedForFilters(Color[][] pixels, int row, Image image) {
        PixelReader pr = image.getPixelReader();
        int numberOfRows = row == 0 || row == image.getHeight() ? 2 : 1;
        Color[][] pixelRow = new Color[numberOfRows][(int)image.getHeight()];


        for(int i = 0; i < pixelRow.length; i++) {
            for(int j = 0; j < image.getHeight(); j++) {

            }
        }

        return pixelRow;
    }


    private static class ImageInfo {
        private Image image;
        private String filename;

        public ImageInfo(Image image, String filename) {
            this.image = image;
            this.filename = filename;
        }

        public Image getImage() {
            return image;
        }

        public String getFilename() {
            return filename;
        }
    }
}
