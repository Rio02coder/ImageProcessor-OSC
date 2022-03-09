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
    private boolean hasFinished;
    private ArrayList<ImageSliceProcessor> imageSliceProcessors;
    private int SLICE_SIZE;
    private int NUMBER_OF_THREADS;


    /**
     * Constructor to create an Image processor.
     * Using this constructor would implicitly create two threads working on the image.
     * @param image the image it has to filter
     * @param filter the type of filter it needs to apply
     * @param save a boolean to indicate if the processor needs to save the image when filtered
     * @param opname the filename under which the filtered image would be saved.
     */
    public ImageProcessorMT(Image image, String filter, boolean save, String opname) {

        this.image = image;
        this.opfilename = opname;
        this.imageSliceProcessors = new ArrayList<>();
        this.filterType = filter;
        this.save = save;
        this.NUMBER_OF_THREADS = 2;
        this.SLICE_SIZE = (int)image.getWidth() / this.NUMBER_OF_THREADS;
    }

    /**
     * Constructor to create an image processor
     * @param image the image it has to filter
     * @param filter the type of filter it needs to apply
     * @param save a boolean to indicate if the processor needs to save the image when filtered
     * @param opname the filename under which the filtered image would be saved
     * @param NUMBER_OF_THREADS the number of threads which would be created to apply the filter on this image
     */
    public ImageProcessorMT(Image image, String filter, boolean save, String opname, int NUMBER_OF_THREADS) {

        this.image = image;
        this.opfilename = opname;
        this.imageSliceProcessors = new ArrayList<>();
        this.filterType = filter;
        this.save = save;
        this.NUMBER_OF_THREADS = NUMBER_OF_THREADS;
        this.SLICE_SIZE = (int)image.getWidth() / this.NUMBER_OF_THREADS;
    }

    /**
     * Runs this image processor.
     * It creates the threads required for slicing the image and gives each thread
     * the required the data to access its slice.
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

    /**
     * This method indicates if the process of filtering the image has finished or not.
     * @return boolean
     */
    public boolean hasFinished() {
        return this.hasFinished;
    }

    /**
     * This method takes the image which was passed to its constructor and adds a grey border around it
     * @return an array of color pixels.
     */
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


    /**
     * This method saves the image which it was asked to filter.
     * @param fileName the name under which the filtered image would be saved.
     */
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
