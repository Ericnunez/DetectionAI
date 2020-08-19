package com.nunezeric.converters;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class ImageConverter {

    public static BufferedImage matToBufferedImage(Mat frame) {
        BufferedImage bufferedImage = null;
        int width = frame.width();
        int height = frame.height();
        int channels = frame.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        frame.get(0,0,sourcePixels);

        if(frame.channels() > 1) {
            bufferedImage = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);
        }
        else {
            bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
        return bufferedImage;
    }

    public static Image matToImage(Mat frame) {
        BufferedImage bufferedImage = matToBufferedImage(frame);
        return SwingFXUtils.toFXImage(bufferedImage,null);
    }
}
