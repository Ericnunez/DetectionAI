package com.nunezeric.controllers;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.text.Text;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.nunezeric.converters.ImageConverter.matToImage;

public class MainController {

    public ImageView currentFrame;
    public Button start_button;
    public Text faceCount;
    public Text eyeCount;
    public CheckBox eyeTrackingCheckbox;
    public CheckBox smileTrackingCheckbox;
    private int absoluteFaceSize = 0;
    private final VideoCapture camera = new VideoCapture();
    private ScheduledExecutorService timer;
    private boolean cameraTurnedOn = false;
    private static final int cameraId = 0;
    private CascadeClassifier cascadeClassifier;
    private CascadeClassifier eyeCascadeClassifier;
    private CascadeClassifier smileCascadeClassifier;


    public void startCamera(ActionEvent actionEvent) {
        if(!cameraTurnedOn) {
            turnOnCamera();
            if (camera.isOpened()) {
                Runnable frameGrabber = new Runnable() {
                    @Override
                    public void run() {
                        // grab a frame
                        //currentFrame is the imageView we are grabbing a single frame and updating the frameGrabber
                        Mat frame = grabFrame();
                        Image image = matToImage(frame);
                        updateImageView(currentFrame, image);
                    }
                };
                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
                updateCameraButton();
            }
        }
        else {
            this.cameraTurnedOn = false;
            updateCameraButton();
            stopApplication();
        }
    }

    public Mat grabFrame(){
        Mat frame = new Mat();
        if(cameraTurnedOn) { //this basically takes a still
            camera.read(frame);
            if(!frame.empty()) {
                detectAndDisplay(frame);
            }
        }
        return frame;
    }

    public void stopApplication() {
        stopTimer();
        stopCamera();
        updateCameraButton();
    }

    public void stopTimer() {
        if(timer == null) {
            return;
        }
        if(!timer.isShutdown() && timer != null) {
            try{
                timer.shutdown();
                timer.awaitTermination(33,TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
        if(cameraTurnedOn)
            stopCamera();
    }

    public void stopCamera() {
        if(cameraTurnedOn) {
            camera.release();
            cameraTurnedOn=false;
            updateCameraButton();
        }
    }

    public void updateCameraButton () {
        String button = cameraTurnedOn ? "Stop Camera" : "Start Camera";
        start_button.setText(button);
    }

    public void turnOnCamera() {
        camera.open(cameraId);
        cameraTurnedOn = true;
    }

    private void updateImageView(ImageView imageView, Image image) {
        onFXThread(imageView.imageProperty(),image);
    }
    public static <T> void onFXThread(final ObjectProperty<T> property, final T value)
    {
        Platform.runLater(() -> {
            property.set(value);
        });
    }
    private void detectAndDisplay(Mat frame) {
        if(this.cascadeClassifier.empty()) {
            System.out.println("cascade is empty");
            return;
        }
        // this holds the rectangle to hover over detected faces,
        // basically an overlay:should be same size of image to be processed
        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();

        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // compute minimum face size (20% of the frame height, in our case)
        if (this.absoluteFaceSize == 0) {
            int height = grayFrame.rows();
            if (Math.round(height * 0.2f) > 0) {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }
        // detect faces
        this.cascadeClassifier.detectMultiScale(
                grayFrame, faces, 1.1, 6,
                0 | Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize),
                new Size());

        // each rectangle in faces is a face: draw them!
        // Scalar is how to add color: values= BGR
        Rect[] facesArray = faces.toArray();

        faceCount.setText("Faces Detected: " + facesArray.length);
        //tl = top left, br = bottom right axis
        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(
                    frame, facesArray[i].tl(),
                    facesArray[i].br(), new Scalar(0, 255, 0), 2);

            Mat detectedFace = grayFrame.submat(facesArray[i]);
            if(eyeTrackingCheckbox.isSelected()) {

                MatOfRect eyes = new MatOfRect();
                eyeCascadeClassifier.detectMultiScale(detectedFace, eyes, 1.3,15);

                Rect[] eyeArray = eyes.toArray();
                eyeCount.setText("Eyes Detected: " + eyeArray.length);
                for (Rect eye : eyeArray) {
                    Point eyeCenter = new Point(facesArray[i].x + eye.x + eye.width / 2, facesArray[i].y + eye.y + eye.height / 2);
                    int radius = (int) Math.round((eye.width + eye.height) * 0.25);
                    Imgproc.circle(frame, eyeCenter, radius, new Scalar(255, 0, 0), 4);
                }
            }
            if(smileTrackingCheckbox.isSelected()) {
                MatOfRect smiles = new MatOfRect();
                smileCascadeClassifier.detectMultiScale(detectedFace,smiles,1.5,60);
                Rect[] smileArray = smiles.toArray();
                for(Rect smile : smileArray) {
                    Imgproc.rectangle(frame,
                            new Point(facesArray[i].x + smile.x, facesArray[i].y + smile.y),
                            new Point(facesArray[i].x + smile.x + smile.x ,facesArray[i].y + smile.y + smile.y/2), new Scalar(0,0,255),2);
                }
            }
        }
    }

    public void loadClassifier(){
        cascadeClassifier = new CascadeClassifier(
                "src/main/resources/haarcascade_frontalface_default.xml");
        eyeCascadeClassifier = new CascadeClassifier(
                "src/main/resources/haarcascade_eye.xml");
        smileCascadeClassifier = new CascadeClassifier(
                "src/main/resources/haarcascade_smile.xml");
    }


}
