import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import utils.Base64Util;
import utils.Constants;
import utils.GsonUtils;
import utils.HttpUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CameraTest {

    static OpenCVFrameConverter.ToIplImage cvFrameConverter = new OpenCVFrameConverter.ToIplImage();

    static Java2DFrameConverter converter = new Java2DFrameConverter();

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) throws FrameGrabber.Exception, InterruptedException {

    }

    public static void doExecuteFrame(Frame f, String targetFileName) {
        if (null == f || null == f.image) {
            return;
        }
        Java2DFrameConverter converter = new Java2DFrameConverter();
        BufferedImage bi = converter.getBufferedImage(f);
        File output = new File(targetFileName);
        try {
            ImageIO.write(bi, "jpg", output);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static Mat moveDetect(Mat background, Mat frame) throws IOException {
        return null;
    }

    /**
     *
     *
     * @param mat
     * @return
     * @throws IOException
     */
    private static byte[] matToData(Mat mat) throws IOException {

        if (mat.height() > 0 && mat.width() > 0) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BufferedImage image = converter.getBufferedImage(cvFrameConverter.convert(mat));
            ImageIO.write(image, "jpg", outputStream);
            return outputStream.toByteArray();
        }
        return null;
    }

    /**
     *
     *
     * @param url
     * @param image
     * @param accessToken
     * @return
     */
    public static String detect(String url, String image, String accessToken) {

        Map<String, Object> map = new HashMap<String, Object>();

        try {
            map.put("image", image);
            map.put("image_type", "BASE64");
            map.put("face_field", "age,beauty,expression,gender,glasses,race,face_type");

            String param = GsonUtils.toJson(map);

            String result = HttpUtil.post(url, accessToken, "application/json", param);
            System.out.println(result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     *
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void takeShot() throws IOException, InterruptedException {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();
        CanvasFrame canvas = new CanvasFrame("摄像头");
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);
        Mat frame = new Mat();
        Mat firstFrame = null;
        VideoCapture capture = new VideoCapture();
        capture.open(0);
        int i = 0;
        while (true) {
            if (!canvas.isDisplayable()) {
                System.out.println("已关闭");
                grabber.stop();
                System.exit(2);
            }
            canvas.showImage(grabber.grab());
            capture.read(frame);
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, gray, new Size(21, 21), 0);
            if (firstFrame == null) {
                firstFrame = gray;
                continue;
            }

            Mat frameDelta = new Mat();
            Core.absdiff(firstFrame, gray, frameDelta);

            Mat thresh = new Mat();
            Imgproc.threshold(frameDelta, thresh, 25, 255, Imgproc.THRESH_BINARY);
            List<MatOfPoint> matOfPoints = new ArrayList<MatOfPoint>();

            Mat hierarchy = new Mat();
            Imgproc.findContours(thresh, matOfPoints, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            Imgproc.dilate(thresh, thresh, new Mat(), new Point(-1, -1), 2);

            if (matOfPoints.size() != 0) {
                byte[] data = matToData(frame);
                String url = "https://aip.baidubce.com/rest/2.0/face/v3/detect";
                String imageCode = Base64Util.encode(data);
                String result = detect(url, imageCode, Constants.ACCESS_TOKEN);
                if (result.contains("SUCCESS")) {
                    BufferedImage res = (BufferedImage) HighGui.toBufferedImage(frame);
                    ImageIO.write(res, "jpg", new File("/Users/thobomas/OpenCVTestA/src/main/java/pics/" + UUID.randomUUID() + ".jpg"));
                    System.out.println(++i);
                }
            }
            Thread.sleep(25);
            firstFrame = gray;
        }

    }

}
