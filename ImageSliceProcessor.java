package com.kcl.osc.imageprocessor;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class ImageSliceProcessor implements Runnable {
    private Image image;
    private String filterType;
    private Thread thread;
    private Color[][] outputPixel;
    private int sliceSize;
    private int rowToSliceFrom;
    private Color[][] pixels;

    public ImageSliceProcessor(Image image, String filter, int sliceSize, int rowToSliceFrom) {
        this.image = image;
        this.filterType = filter;
        this.sliceSize = sliceSize;
        this.rowToSliceFrom = rowToSliceFrom;
    }

    public ImageSliceProcessor(Color[][]pixels, String filter, int sliceSize, int rowToSliceFrom) {
        this.pixels = pixels;
        this.filterType = filter;
        this.sliceSize = sliceSize;
        this.rowToSliceFrom = rowToSliceFrom;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        this.filter();
    }

    public void setOutputPixel(Color[][] output) {
        this.outputPixel = output;
    }


    public Color[][] getOutputPixel() {
        return outputPixel;
    }

    private Color[][] filterImage() {

        if (filterType.equals("GREY")) {
            return applyGreyscale();
        }

        Color[][] pixels = getPixelDataExtended();

        float[][] filter = createFilter(filterType);

        Color[][] filteredImage = applyFilter(pixels, filter);

        setOutputPixel(filteredImage);

        return filteredImage;
    }


    /**
     * Applies the greyscale operation.
     * @return the new pixel data.
     */
    private Color[][] applyGreyscale() {
        Color[][] pixel = getPixelData();
        Color[][] outputPixels = new Color[pixel.length][pixel[0].length];
        for (int i = 0; i < (pixel.length);i++) {
            for (int j = 0; j < (pixel[0].length); j++) {

                double red = pixel[i][j].getRed();
                double green = pixel[i][j].getGreen();
                double blue = pixel[i][j].getBlue();

                double newRGB = (red + green + blue) / 3;
                newRGB = clampRGB(newRGB);

                Color newPixel = new Color(newRGB, newRGB, newRGB, 1.0);
                outputPixels[i][j] = newPixel;
            }
        }

        return outputPixels;
    }

    public void join() {
        try {
            thread.join();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Applies the required filter to the input pixel data.
     * @param pixels The input pixel data.
     * @param filter The filter.
     * @return The new, filtered pixel data.
     */
    private Color[][] applyFilter(Color[][] pixels, float[][] filter) {

        Color[][] finalImage = new Color[pixels.length - 2][pixels[0].length - 2];

        for (int i = 1; i <= finalImage.length; i++) {
            for (int j = 1; j <= finalImage[0].length; j++) {

                double red = 0.0;
                double green = 0.0;
                double blue = 0.0;

                for (int k = -1; k < filter.length - 1; k++) {
                    for (int l = -1; l < filter[0].length - 1; l++) {

                        red += pixels[i + k][j + l].getRed() * filter[1 + k][1 + l];
                        green += pixels[i + k][j + l].getGreen() * filter[1 + k][1 + l];
                        blue += pixels[i + k][j + l].getBlue() * filter[1 + k][1 + l];
                    }
                }

                red = clampRGB(red);
                green = clampRGB(green);
                blue = clampRGB(blue);
                finalImage[i - 1][j - 1] = new Color(red,green,blue,1.0);
            }
        }

        return finalImage;
    }

    private void filter() {
        Color[][] pixels = filterImage();
        this.setOutputPixel(pixels);
    }

    /**
     * Creates the filter.
     * @param filterType The type of filter required.
     * @return The filter.
     */
    private float[][] createFilter(String filterType) {
        filterType = filterType.toUpperCase();

        if (filterType.equals("IDENTITY")) {
            return (new float[][] {{0,0,0},{0,1,0},{0,0,0}});
        } else if (filterType.equals("BLUR")) {
            return (new float[][] {{0.0625f,0.125f,0.0625f},{0.125f,0.25f,0.125f},{0.0625f,0.125f,0.0625f}});
        } else if (filterType.equals("SHARPEN")) {
            return (new float[][] {{0,-1,0},{-1,5,-1},{0,-1,0}});
        } else if (filterType.equals("EDGE")) {
            return (new float[][] {{-1,-1,-1},{-1,8,-1},{-1,-1,-1}});
        } else if (filterType.equals("EMBOSS")) {
            return (new float[][] {{-2,-1,0},{-1,0,1},{0,1,2}});
        }
        return null;
    }

    /**
     * This method ensures that the computations on color values have not
     * strayed outside of the range [0,1].
     * @param RGBValue the value to clamp.
     * @return The clamped value.
     */
    protected static double clampRGB(double RGBValue) {
        if (RGBValue < 0.0) {
            return 0.0;
        } else if (RGBValue > 1.0) {
            return 1.0;
        } else {
            return RGBValue;
        }
    }

    /**
     * Gets the pixel data from the image but does
     * NOT add a border.
     * @return The pixel data.
     */
    private Color[][] getPixelData() {
        PixelReader pr = image.getPixelReader();
        Color[][] pixels = new Color[this.sliceSize][(int) image.getHeight()];
        for(int i = 0; i < sliceSize;i++) {
            for(int j = 0; j < image.getHeight(); j++) {
                pixels[i][j] = pr.getColor(rowToSliceFrom, j);
            }
            rowToSliceFrom++;
        }
        return pixels;
    }

    /**
     * Gets the pixel data from the image but with a one-pixel border added.
     * @return The pixel data.
     */
    private Color[][] getPixelDataExtended() {

        Color[][] pixelData = new Color[this.sliceSize + 2][this.pixels[0].length];

        int sizeToUse = sliceSize + 2;

        for(int i = 0; i < sizeToUse; i++) {
            for(int j = 0; j < pixels[0].length; j++) {
                pixelData[i][j] = pixels[rowToSliceFrom][j];
            }
            rowToSliceFrom++;
        }

        return pixelData;
    }
}
