package com.kcl.osc.imageprocessor;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;

public class ImageProcessorMT implements Runnable {
    private Image image;
    private String opfilename;
    private String filterType;
    private boolean save;
    private Thread thread;
    private boolean hasFinished;
    private ArrayList<ImageSliceProcessor> imageSliceProcessors;
    private int SLICE_SIZE;
    private int NUMBER_OF_THREADS;

    public ImageProcessorMT(Image image, String filter, boolean save, String opname) {

        this.image = image;
        this.opfilename = opname;
        this.imageSliceProcessors = new ArrayList<>();
        this.filterType = filter;
        this.save = save;
        this.NUMBER_OF_THREADS = 2;
        this.SLICE_SIZE = (int)image.getWidth() / this.NUMBER_OF_THREADS;
    }

    public ImageProcessorMT(Image image, String filter, boolean save, String opname, int NUMBER_OF_THREADS) {

        this.image = image;
        this.opfilename = opname;
        this.imageSliceProcessors = new ArrayList<>();
        this.filterType = filter;
        this.save = save;
        this.NUMBER_OF_THREADS = NUMBER_OF_THREADS;
        this.SLICE_SIZE = (int)image.getWidth() / this.NUMBER_OF_THREADS;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Runs this image processor.
     */
    public void run() {
        if(filterType.equals("GREY")) {
            int rowToStartFrom = 0;

            for(int i = 0; i < this.NUMBER_OF_THREADS; i++) {
                if (i == this.NUMBER_OF_THREADS - 1) {
                    int sliceSizeForLastThread = (int)image.getWidth() - (i * SLICE_SIZE);
                    ImageSliceProcessor isp = new ImageSliceProcessor(this.image,this.filterType,sliceSizeForLastThread,rowToStartFrom);
                    imageSliceProcessors.add(isp);
                    isp.start();
                }
                else {
                    ImageSliceProcessor isp = new ImageSliceProcessor(this.image,this.filterType,SLICE_SIZE,rowToStartFrom);
                    imageSliceProcessors.add(isp);
                    isp.start();
                }
                rowToStartFrom += SLICE_SIZE;
            }

        }
        else {
            int rowToStartFrom = 0;
            Color[][] borderedPixels = getImageDataExtended();

            for(int i = 0; i < this.NUMBER_OF_THREADS; i++) {
                if (i == this.NUMBER_OF_THREADS - 1) {
                    int sliceSizeForLastThread = (int)image.getWidth() - (i * SLICE_SIZE);
                    ImageSliceProcessor isp = new ImageSliceProcessor(borderedPixels,this.filterType,sliceSizeForLastThread,rowToStartFrom);
                    imageSliceProcessors.add(isp);
                    isp.start();
                }
                else {
                    ImageSliceProcessor isp = new ImageSliceProcessor(borderedPixels,this.filterType,SLICE_SIZE,rowToStartFrom);
                    imageSliceProcessors.add(isp);
                    isp.start();
                }
                rowToStartFrom += SLICE_SIZE;
            }

        }

        for(ImageSliceProcessor isp: imageSliceProcessors) {
            isp.join();
        }

        if(save) {
            saveImage(opfilename);
        }

        this.hasFinished = true;
    }

    public boolean hasFinished() {
        return this.hasFinished;
    }

    private Color[][] getImageDataExtended() {
        PixelReader pr = image.getPixelReader();
        Color[][] pixels = new Color[(int) image.getWidth() + 2][(int) image.getHeight() + 2];

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels.length; j++) {
                pixels[i][j] = new Color(0.5, 0.5, 0.5, 1.0);
            }
        }

        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                pixels[i + 1][j + 1] = pr.getColor(i, j);
            }
        }

        return pixels;
    }

    private void saveImage(String fileName) {
        WritableImage wimg = new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());
        PixelWriter pw = wimg.getPixelWriter();

        int count = 0;
        int sliceToGet = 0;
        for(int i = 0; i < image.getHeight(); i++) {
            Color[][]pixel = imageSliceProcessors.get(sliceToGet).getOutputPixel();
            for(int j = 0; j < image.getWidth(); j++) {
                pw.setColor(i,j,pixel[count][j]);
            }

            if(count == pixel.length - 1) {
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
}
