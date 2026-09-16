package johnseagull.jif;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AppController {

    public List<List<Color>> mainBuffer = new ArrayList<>();
    public List<List<Byte>> valueBuffer = new ArrayList<List<Byte>>();
    public List<List<Byte>> satBuffer = new ArrayList<>();
    public List<List<Byte>> hueBuffer = new ArrayList<List<Byte>>();
    public Path outputpath;
    public Canvas canvas;
    public TextField path;
    public Button copy;
    public Button encode;
    public Button dump;
    public Image image;
    public ProgressBar p3;
    public ProgressBar p2;
    public ProgressBar p1;
    public TextArea label;
    public TextField quality;
    public TextField cee;
    public Pane viewport;
    public Button export;
    public TextField maxrle;
    public CheckBox fancyCEE;
    public CheckBox hue;
    public CheckBox sat;
    public CheckBox ceeColor;

    private double lastMouseX;
    private double lastMouseY;

    private double scal_min = 0.1;
    private double scal_max = 10.0;
    private double zom = 1.1;
    @FXML
    public void initialize() {
        setupZom();
        setupPanning();
    }


    private void setupPanning() {
        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(viewport.widthProperty());
        clipRect.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clipRect);
        canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isPrimaryButtonDown()) {
                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
            }
        });

        viewport.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (event.isPrimaryButtonDown()) {
                double deltaX = event.getSceneX() - lastMouseX;
                double deltaY = event.getSceneY() - lastMouseY;

                canvas.setTranslateX(canvas.getTranslateX() + deltaX);
                canvas.setTranslateY(canvas.getTranslateY() + deltaY);

                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();

                event.consume();
            }
        });

    }

    private void setupZom() {
        canvas.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            
            double scaleFactor = (event.getDeltaY() > 0) ? zom : (1 / zom);
            double currentScale = canvas.getScaleX();
            double newScale = Math.clamp(currentScale * scaleFactor, scal_min, scal_max);
            canvas.setScaleX(newScale);
            canvas.setScaleY(newScale);

            event.consume();
        });
    }
    Task<Void> compress = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
            return null;
        }
    };

    private byte[] decomp(byte[] input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (int i = 0; i < input.length; i++) {
            if ((input[i] & 0xFF) == 0xFF) {
                byte v = input[i + 1];
                int run = input[i + 2] & 0xFF;

                for (int j = 0; j < run; j++) {
                    output.write(v);
                }
                i += 2;
            } else {
                output.write(input[i]);
            }
        }
        return output.toByteArray();
    }


    @FXML
    protected void onLoadClick() {
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();

        if (path.getText().replace("\"","").endsWith(".jif")) {
            onDecodeClick(path.getText().replace("\"", ""));
            return;
        }
        image = new Image("file:" + path.getText().replace("\"", ""));
        System.out.println("load image: " + path.getText().replace("\"", ""));
        int w = 0;
        int h = 0;

        if (image.getWidth() > canvas.getWidth() || image.getHeight() > canvas.getHeight() || image.getWidth() < canvas.getWidth() || image.getHeight() < canvas.getHeight()) {
            w = (int) canvas.getWidth();
            float wr = (float) (w / image.getWidth());
            h = (int) (image.getHeight() * wr);
        }

        gc.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
        if (image.getWidth() == 0 || image.getHeight() == 0) {
            throwError(new Exception("Invalid Format"));
        }
    }


    public void onEncodeClick(MouseEvent mouseEvent) {

        if (image == null) return;
        int q;
        try {
            q = Integer.parseInt(quality.getText());
        } catch (NumberFormatException e) {
            System.out.println("Invalid quality");
            return;
        }
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        int c =0;
        try {

             c = Integer.parseInt(cee.getText());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        PixelReader pr = image.getPixelReader();


        int finalC = c;
        Task<Boolean> encode = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                updateValue(true);
                updateMessage("Preparing...\nCreating working cache");
                valueBuffer.clear();
                hueBuffer.clear();
                satBuffer.clear();
                int p = 0;
                int pmax = ((h*w + (((h/q)*(w/q))*q))*q) +h*w;
                updateMessage("Caching value data...");
                for (int y = 0; y < h; y++) {
                    List<Byte> lineBuffer = new ArrayList<>();
                    for (int x = 0; x < w; x++) {

                        int v = (int) (pr.getColor(x, y).getBrightness() * 254.0);
                        lineBuffer.add((byte) v);
                        p++;
                        if (p% 128 == 0) {
                            updateProgress(p, pmax);
                        }
                    }
                    valueBuffer.add(lineBuffer);
                }
                updateMessage("Caching hue data...");
                for (int y = 0; y < h; y+=q) {
                    List<Byte> lineBuffer = new ArrayList<>();
                    for (int x = 0; x < w; x+=q) {
                        int v = 0;
                        if (hue.isSelected()) {
                            double sum = 0;
                            for (int iy = 0; iy < q; iy++) {
                                for (int ix = 0; ix < q; ix++) {
                                    double ve = (pr.getColor(x + ix, y + iy).getHue());
                                    sum += ve;
                                }
                            }
                            v = (int) (sum / (q * q) * (255.0 / 360.0));
                        } else {
                            v = (int) (pr.getColor(x,y).getHue()* (255.0 / 360.0));
                        }
                        for (int z = 0; z < q; z++) {
                            lineBuffer.add((byte) v);
                        }
                        p++;
                        if (p% 128 == 0) {
                            updateProgress(p, pmax);
                        }
                    }
                    for (int z = 0; z < q; z++) {
                        hueBuffer.add(lineBuffer);
                    }
                }
                updateMessage("Caching saturation data...");
                for (int y = 0; y < h; y+=q) {
                    List<Byte> lineBuffer = new ArrayList<>();
                    for (int x = 0; x < w; x+=q) {
                        int v = 0;
                        if (hue.isSelected()) {
                            double sum = 0;
                            for (int iy = 0; iy < q; iy++) {
                                for (int ix = 0; ix < q; ix++) {
                                    double ve = (pr.getColor(x + ix, y + iy).getSaturation());
                                    sum += ve;
                                }
                            }
                            v = (int) (sum / (q * q) * (254));
                        } else {
                            v = (int) (pr.getColor(x,y).getHue()* (254));
                        }
                        for (int z = 0; z < q; z++) {
                            lineBuffer.add((byte) v);
                        }
                        p++;
                        if (p% 128 == 0) {
                            updateProgress(p, pmax);
                        }
                    }
                    for (int z = 0; z < q; z++) {
                        satBuffer.add(lineBuffer);
                    }
                }
                updateMessage("Writing buffer to file...");

                File source = new File(path.getText().replace("\"", ""));
                File out = new File(source.getParentFile(), source.getName().replaceFirst("\\.[^.]+$", "") + ".jif");

                try (FileOutputStream fos = new FileOutputStream(out)) {
                    int hrs = 16;
                    int vs = (int) (h * w);
                    int hs = (int) ((h / q) * (w / q));
                    int ss = (int) ((h / q) * (w / q));
                    byte[] buffer = new byte[hrs + vs + hs + ss + 1];

                    buffer[0] = 0x4a;
                    buffer[1] = 0x55;
                    buffer[2] = 0x44;
                    buffer[3] = 0x03;

                    short iw = (short) w;
                    short ih = (short) h;
                    buffer[4] = (byte) (iw >> 8);
                    buffer[5] = (byte) (iw);
                    buffer[6] = (byte) (ih >> 8);
                    buffer[7] = (byte) (ih);
                    buffer[8] = (byte) q;
                    buffer[9] = (byte) finalC;
                    buffer[10] = (byte)Integer.parseInt(maxrle.getText());
                    if (fancyCEE.isSelected()) {
                        buffer[11] = (byte) 0x01;
                    }
                    int pos = 16;
                    updateMessage("Writing value data...");
                    for (int y = 0; y < ih; y++) {
                        for (int x = 0; x < iw; x++) {
                            buffer[pos++] = valueBuffer.get(y).get(x);
                            p++;
                            if (p% 1024 == 0) {
                                updateProgress(p, pmax);
                            }
                        }
                    }
                    updateMessage("Writing hue data...");
                    for (int y = 0; y < ih / q; y++) {
                        for (int x = 0; x < iw / q; x++) {
                            buffer[pos++] = hueBuffer.get(y*q).get(x*q);
                            p++;
                            if (p% 1024 == 0) {
                                updateProgress(p, pmax);
                            }
                        }
                    }
                    updateMessage("Writing saturation data...");
                    for (int y = 0; y < ih / q; y++) {
                        for (int x = 0; x < iw / q; x++) {
                            buffer[pos++] = satBuffer.get(y*q).get(x*q);
                            p++;
                            if (p% 1024 == 0) {
                                updateProgress(p, pmax);
                            }
                        }
                    }
                    updateMessage("Compressing...");
                    boolean doCEE = (finalC != 0);
                    updateProgress(-1,0);
                    int e;
                    try {
                        e = Integer.parseInt(maxrle.getText());
                    } catch (NumberFormatException ex) {
                        e = 0;
                    }

                    updateMessage("Compressing...\nPreparing...");
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    int vSize = (int) ((image.getHeight() * image.getWidth()) + 16);
                    for (int i = 0; i < buffer.length; i++) {
                        updateProgress(i, buffer.length);
                        updateMessage("Compressing...\n"+ i + "/"+buffer.length+" Bytes");

                        int run = 1;
                        if (i > 15) {
                            if (!doCEE) {
                                while (i + run < buffer.length && buffer[i + run] == buffer[i] && run < e) {
                                    run++;
                                }
                            } else {
                                while (i + run < buffer.length && Math.abs(buffer[i + run] - buffer[i]) <= finalC && run < e) {
                                    run++;
                                }
                            }
                        }
                        if (run >= 3) {
                            byte old = 0;
                            if (fancyCEE.isSelected()) {
                                List<Byte> pixels = new ArrayList<>();
                                for (int j = i; j < i + run; j++) {
                                    pixels.add(buffer[j]);
                                }
                                long sum = 0;
                                for (Byte b : pixels) {
                                    sum += Byte.toUnsignedInt(b);
                                }
                                double avg = (double) sum / pixels.size();
                                old = (byte) avg;
                            } else {
                                old = buffer[i];
                            }
                            output.write((byte) 0xff);
                            output.write(old);
                            output.write((byte) run);

                            i += run - 1;
                        } else if (buffer[i] == (byte) 0xff) {
                            output.write((byte) 0xfe);
                        } else {
                            output.write(buffer[i]);
                        }
                    }
                  
                    
                    fos.write(output.toByteArray());
                    updateMessage("Done");


                } catch (Exception ex) {
                    System.err.println("smth done messed up");
                    System.err.println(ex.getMessage());
                    throw ex;
                }
                p = pmax;
                updateProgress(p, pmax);
                updateValue(false);
                
                
                
                
                
                
                return null;

            }
        };
        label.textProperty().unbind();
        label.textProperty().bind(encode.messageProperty());
        p1.progressProperty().unbind();
        p1.progressProperty().bind(encode.progressProperty());
        p1.visibleProperty().unbind();
        p1.visibleProperty().bind(encode.valueProperty());
        new Thread(encode).start();
    }

    public void onDumpClick(MouseEvent mouseEvent) {
        for (List<Byte> line : valueBuffer) {
            System.out.println(line.toString());
        }
        for (List<Byte> line : hueBuffer) {
            System.out.println(line.toString());
        }
    }

    public void e(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.F3) {
            throwError(new Exception("intentional crash"));
        }
    }

    public void throwError(Exception e) {
        List<String> titles = List.of(
                "Why did you do that",
                "Smth done messed up",
                "Fatal error.. or is it?",
                "Who dod that??",
                ":(",
                "Error i think",
                "To crash or not to crash",
                "spaghet ti-84+",
                "Well well well, deep subject",
                "Houston we have a problem",
                "Fatal error imo",
                "HAHAHAHAHHAHAAHAHAHAHAHA",
                "Funky error :]",
                "Oh come on",
                "But the code refused to work.",
                "These title texts are random",
                "*cue dramatic crash sound*",
                "A cat ate the code",
                "You encountered the Exception",
                " ",
                "the program did not program"
        );
        valueBuffer.clear();
        hueBuffer.clear();
        satBuffer.clear();
        Platform.runLater(() -> {
            Stage stage = JifApplication.s;
            FXMLLoader fxmlLoader = new FXMLLoader(JifApplication.class.getResource("error.fxml"));
            try {
                Scene scene = new Scene(fxmlLoader.load(), 339, 310);
                ErrorController controller = fxmlLoader.getController();
                stage.setTitle(titles.get(ThreadLocalRandom.current().nextInt(0, titles.size() - 1)));
                stage.setScene(scene);
                stage.show();
                stage.setResizable(false);
                controller.stack.setText(e.getMessage() + "\n" +Arrays.toString(e.getStackTrace()));
                if (e instanceof IOException) {
                    controller.info.setText("I/O Error: Usually an error with missing or conflicting files");
                }
                if (e instanceof ArrayIndexOutOfBoundsException) {
                    controller.info.setText("Index out of bounds: Usually means a corrupt file or bad settings");
                }
                if (e.getMessage().equals("intentional crash")) {
                    controller.info.setText("Intentional crash: congrats you found a hidden debug thing");
                }
                if (e.getMessage().equals("Invalid Format")) {
                    controller.info.setText("Invalid Format: the file is not a valid JIF image file");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }
    public void onDecodeClick(String p) {

        valueBuffer.clear();
        hueBuffer.clear();
        satBuffer.clear();

        Task<Boolean> decode = new Task<>() {
            @Override
            protected Boolean call() throws IOException {

                try {

                    updateValue(true);
                    updateMessage("Prepare for decode");
                    FileInputStream fis = new FileInputStream(p);
                    updateMessage("Image info \nSize: " + new File(p).length() + "\n");
                    byte[] rawbuffer = fis.readAllBytes();
                    byte[] buffer = decomp(rawbuffer);
                    int posmax = buffer.length;

                    if (
                            buffer[0] == 0x4a
                                    && buffer[1] == 0x55
                                    && buffer[2] == 0x44
                    ) {
                        System.out.println("found correct format - attempting decode...");
                        int w = (((buffer[4] & 0xFF) << 8) | (buffer[5] & 0xFF));
                        int h = (((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF));
                        int q = buffer[8] & 0xff;
                        int c = buffer[9] & 0xFF;
                        System.out.println("using subsample multiplier of " + q);
                        WritableImage newImage = new WritableImage(w, h);
                        int pos = 16;
                        updateMessage("Decoding value data...");
                        for (int y = 0; y < h; y++) {
                            List<Byte> lineBuffer = new ArrayList<>();
                            for (int x = 0; x < w; x++) {

                                int b = buffer[pos] & 0xFF;

                                lineBuffer.add(buffer[pos]);
                                pos++;
                                if (pos % 128 == 0) {
                                    updateProgress(pos, posmax);
                                }
                            }

                            valueBuffer.add(lineBuffer);

                        }
                        updateMessage("Decoding hue data...");
                        for (int j = 0; j < h / q; j++) {
                            int y = j * q;
                            List<Byte> lineBuffer = new ArrayList<>();
                            for (int i = 0; i < w / q; i++) {
                                if (pos % 128 == 0) {
                                    updateProgress(pos, posmax);
                                }
                                for (int z = 0; z < q; z++) {
                                    lineBuffer.add(buffer[pos]);
                                }
                                pos++;
                            }
                            for (int z = 0; z < q; z++) {
                                hueBuffer.add(lineBuffer);
                            }
                        }
                        updateMessage("Decoding saturation data...");
                        for (int j = 0; j < h / q; j++) {
                            int y = j * q;
                            List<Byte> lineBuffer = new ArrayList<>();
                            for (int i = 0; i < w / q; i++) {
                                int x = i * q;

                                if (pos % 128 == 0) {
                                    updateProgress(pos, posmax);
                                }
                                for (int z = 0; z < q; z++) {
                                    lineBuffer.add(buffer[pos]);
                                }
                                pos++;
                            }
                            for (int z = 0; z < q; z++) {
                                satBuffer.add(lineBuffer);
                            }
                        }
                        int cw = 0;
                        int ch = 0;
                        updateMessage("Rendering");
                        updateProgress(-1,0);
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int val = 0;
                                int hue = 0;
                                int sat = 0;
                                try {
                                    val = valueBuffer.get(y).get(x) & 0xFF;
                                    hue = hueBuffer.get(y).get(x) & 0xFF;
                                    sat = satBuffer.get(y).get(x) & 0xFF;
                                } catch (IndexOutOfBoundsException e) {

                                }


                                pos++;
                                if (pos % 128 == 0) {
                                    updateProgress(pos, posmax);
                                }
                                newImage.getPixelWriter().setColor(x, y, Color.hsb((double) hue * (360.0 / 255.0), Math.clamp((double) sat / 254,0,1.0), (double) val / 254));
                            }
                        }
                        if (newImage.getWidth() > canvas.getWidth() || newImage.getHeight() > canvas.getHeight() || newImage.getWidth() < canvas.getWidth() || newImage.getHeight() < canvas.getHeight()) {
                            cw = (int) canvas.getWidth();
                            float wr = (float) (cw / newImage.getWidth());
                            ch = (int) (newImage.getHeight() * wr);
                        }
                        image = newImage;
                        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                        canvas.getGraphicsContext2D().drawImage(newImage, 0, 0, cw, ch);
                        updateMessage("Done \nSize: "+ w + "x" + h  +" Fancy CEE: "+ buffer[11]+ "\nSubsampling: "+q+" CEE: " + c + "\nMax RLE length: " + (buffer[10]& 0xFF));
                    } else {
                        updateMessage("Invalid Format");
                        throwError(new Exception("Invalid Format"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println(e.getMessage());

                    throwError(e);
                }
                updateValue(false);
                return null;
            }
        };
        label.textProperty().unbind();
        label.textProperty().bind(decode.messageProperty());
        p1.progressProperty().unbind();
        p1.progressProperty().bind(decode.progressProperty());

        p1.visibleProperty().unbind();
        p1.visibleProperty().bind(decode.valueProperty());
        new Thread(decode).start();
        valueBuffer.clear();
        hueBuffer.clear();
        satBuffer.clear();
    }
    public void onExport(MouseEvent mouseEvent) {
        try {
            int w = (int) image.getWidth();
            int h = (int) image.getHeight();
            BufferedImage bImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            PixelReader pr = image.getPixelReader();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Color c = pr.getColor(x, y);
                    int rgb = ((int) Math.round(c.getRed() * 255) << 16)
                            | ((int) Math.round(c.getGreen() * 255) << 8)
                            | ((int) Math.round(c.getBlue() * 255));
                    bImage.setRGB(x, y, rgb);
                }
            }
            File source = new File(path.getText().replace("\"",""));
            File out = new File(source.getParentFile(), source.getName().replaceFirst("\\.[^.]+$", "") + "_decoded.bmp");
            ImageIO.write(bImage, "bmp", out);
            System.out.println("Exported BMP to output.bmp");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void onFlush(MouseEvent mouseEvent) {

        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        valueBuffer.clear();
        hueBuffer.clear();
        satBuffer.clear();
    }
}
