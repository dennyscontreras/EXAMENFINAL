package com.example.textdetection;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.textdetection.ml.Modelomascota;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import camerax.CameraConnectionFragment;
import camerax.ImageUtils;

public class MainActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener, // Implementa OnInitListener para TextToSpeech
        OnSuccessListener<Text>,
        OnFailureListener,
        ImageReader.OnImageAvailableListener {
    // Resto del código de la clase


    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;
    Bitmap mSelectedImage;
    String[] labels = {"AUDITORIO","COMEDOR 2","BIBLIOTECA","CENTRO MEDICO","COMEDOR 1","DEPARTAMENTO ACADEMICO","DEPARMENTO DE ARCHIVOS","FACULTAD DE PEDAGOGÌA","FACULTAD DE CIENCIAS EMPRESARIALES","FACULTAD DE SALUD","FACULTAD DE CIENCIAS SOCIAL, FINANCIERA Y ECONOMIC","INSTITUTO INFORMATICO","INVESTIGACIÒN","PARQUEADERO ADMINISTRATIVO","PARQUEADERO DE AUTORIDADES","PARQUEADERO DE ESTUDIANTES","PARQUEADERO INSTITUCIONAL","POLIDEPORTIVO","RECTORADO","ROTONDA"};
    ImageView mImageView;
    ArrayList<String> permisosNoAprobados;
    Button btnCamara;
    Button btnGaleria;
    Button btnNarrar; // Botón para narrar el texto
    TextView txtResults;
    private TextToSpeech textToSpeech; // Objeto TextToSpeech
    private static final String UTTERANCE_ID = "ReadText";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayList<String> permisos_requeridos = new ArrayList<String>();
        permisos_requeridos.add(Manifest.permission.CAMERA);
        permisos_requeridos.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        permisos_requeridos.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        txtResults = findViewById(R.id.txtresults);
        btnCamara = findViewById(R.id.btCamera);
        btnGaleria = findViewById(R.id.btGallery);
        btnNarrar = findViewById(R.id.btspeak); // Referencia al botón para narrar

        // Inicializa el objeto TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // Se llama cuando comienza a hablar
            }

            @Override
            public void onDone(String utteranceId) {
                // Se llama cuando finaliza la narración
            }

            @Override
            public void onError(String utteranceId) {
                // Se llama cuando hay un error en la narración
            }
        });

        permisosNoAprobados = getPermisosNoAprobados(permisos_requeridos);
        requestPermissions(permisosNoAprobados.toArray(new String[permisosNoAprobados.size()]),
                100);
    }
    public void speak(View view) {
        String textToRead = txtResults.getText().toString();
        if (!textToRead.isEmpty()) {
            textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Configura el idioma a español
            int result = textToSpeech.setLanguage(new Locale("es", "ES")); // Cambia "es" a la abreviatura del idioma que deseas si es diferente

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Maneja el caso en que el idioma no está disponible
                Log.e("TTS", "El idioma no está disponible.");
            }
        } else {
            Log.e("TTS", "La inicialización de TTS falló.");
        }
    }
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    public void abrirGaleria (View view){
    Intent i = new Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(i, REQUEST_GALLERY);
}
    public void abrirCamera (View view){
    //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    //startActivityForResult(intent, REQUEST_CAMERA);
    this.setFragment();
}
    @Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && null != data) {
        try {
            if (requestCode == REQUEST_CAMERA)
                mSelectedImage = (Bitmap) data.getExtras().get("data");
            else
                 mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());

            mImageView.setImageBitmap(mSelectedImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for(int i=0; i<permissions.length; i++){
            if(permissions[i].equals(Manifest.permission.CAMERA)){
                btnCamara.setEnabled(grantResults[i] == PackageManager.PERMISSION_GRANTED);
            } else if(permissions[i].equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE) ||
                    permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)
            ) {
                //btnGaleria.setEnabled(grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
        }
    }

    public ArrayList<String> getPermisosNoAprobados(ArrayList<String>  listaPermisos) {
        ArrayList<String> list = new ArrayList<String>();
        Boolean habilitado;


        if (Build.VERSION.SDK_INT >= 23)
            for(String permiso: listaPermisos) {
                if (checkSelfPermission(permiso) != PackageManager.PERMISSION_GRANTED) {
                    list.add(permiso);
                    habilitado = false;
                }else
                    habilitado=true;

                if(permiso.equals(Manifest.permission.CAMERA))
                    btnCamara.setEnabled(habilitado);
                else if (permiso.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE)  ||
                        permiso.equals(Manifest.permission.READ_EXTERNAL_STORAGE))
                    btnGaleria.setEnabled(habilitado);
            }


        return list;
    }
    public void OCRfx(View v) {
    InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    recognizer.process(image)
            .addOnSuccessListener(this)
            .addOnFailureListener(this);
}


    @Override
    public void onFailure(@NonNull Exception e) {
        txtResults.setText("Error al procesar imagen");
    }

    @Override
    public void onSuccess(Text text) {
        List<Text.TextBlock> blocks = text.getTextBlocks();
    String resultados="";
    if (blocks.size() == 0) {
        resultados = "No hay Texto";
    }else{
        for (int i = 0; i < blocks.size(); i++) {
           List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                 List<Text.Element> elements = lines.get(j).getElements();
                 for (int k = 0; k < elements.size(); k++) {
                     resultados = resultados + elements.get(k).getText() + " ";
                 }
            }
        }
        resultados=resultados + "\n";
    }
    txtResults.setText(resultados);
    }


    public void Rostrosfx(View  v) {
    InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();

    FaceDetector detector = FaceDetection.getClient(options);
    detector.process(image)
            .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                @Override
                public void onSuccess(List<Face> faces) {
                    if (faces.size() == 0) {
                        txtResults.setText("No Hay rostros");
                    }else{
                        txtResults.setText("Hay " + faces.size() + " rostro(s)");
                    }
                }
            })
            .addOnFailureListener(this);
}

    public void PersonalizedModel(View v) {
    try {
        Modelomascota model = Modelomascota.newInstance(getApplicationContext());
        TensorImage image = TensorImage.fromBitmap(mSelectedImage);

        //Modelomascota.Outputs outputs = model.process(image);

        /*List<Category> probability = outputs.getProbabilityAsCategoryList();
        Collections.sort(probability, new CategoryComparator());

        String res="";
        for (int i = 0; i < probability.size(); i++) {
            res = res + probability.get(i).getLabel() +  " " +  probability.get(i).getScore()*100 + " % \n";
        }

        txtResults.setText(res);*/

        model.close();
    } catch (IOException e) {
        txtResults.setText("Error al procesar Modelo");
    }
}
    int previewHeight = 0,previewWidth = 0;
int sensorOrientation;
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
protected void setFragment() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    String cameraId = null;
    try {
        cameraId = manager.getCameraIdList()[0];
    } catch (CameraAccessException e) {
        e.printStackTrace();
    }
    CameraConnectionFragment fragment;
    CameraConnectionFragment camera2Fragment =
            CameraConnectionFragment.newInstance(
                    new CameraConnectionFragment.ConnectionCallback() {
                        @Override
                        public void onPreviewSizeChosen(final Size size, final int rotation) {
                            previewHeight = size.getHeight();    previewWidth = size.getWidth();
                            sensorOrientation = rotation - getScreenOrientation();
                        }
                    },
                    this,   R.layout.camerafragment,            new Size(640, 480));

    camera2Fragment.setCamera(cameraId);
    fragment = camera2Fragment;
    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
}
    protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
        case Surface.ROTATION_270:
            return 270;
        case Surface.ROTATION_180:
            return 180;
        case Surface.ROTATION_90:
            return 90;
        default:
            return 0;
    }
}
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private Bitmap rgbFrameBitmap;
    @Override
    public void onImageAvailable(ImageReader imageReader) {
        if (previewWidth == 0 || previewHeight == 0)           return;
    if (rgbBytes == null)    rgbBytes = new int[previewWidth * previewHeight];
    try {
        final Image image = imageReader.acquireLatestImage();
        if (image == null)    return;
        if (isProcessingFrame) {           image.close();            return;         }
        isProcessingFrame = true;
        final Image.Plane[] planes = image.getPlanes();
        fillBytes(planes, yuvBytes);
        yRowStride = planes[0].getRowStride();
        final int uvRowStride = planes[1].getRowStride();
        final int uvPixelStride = planes[1].getPixelStride();
        imageConverter =  new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420ToARGB8888( yuvBytes[0], yuvBytes[1], yuvBytes[2], previewWidth,  previewHeight,
                yRowStride,uvRowStride, uvPixelStride,rgbBytes);
                    }
                };
        postInferenceCallback =      new Runnable() {
                    @Override
                    public void run() {  image.close(); isProcessingFrame = false;  }
                };

        processImage();

    } catch (final Exception e) {    }
        
    }
    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
	for (int i = 0; i < planes.length; ++i) {
      		  final ByteBuffer buffer = planes[i].getBuffer();
       	  if (yuvBytes[i] == null) {
          		  yuvBytes[i] = new byte[buffer.capacity()];
        	}
        	buffer.get(yuvBytes[i]);
    }
}
    private void processImage() {
    imageConverter.run();

    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
    rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);

    try {
        Modelomascota model = Modelomascota.newInstance(getApplicationContext());
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
        rgbFrameBitmap = Bitmap.createScaledBitmap(rgbFrameBitmap, 224, 224, true);
       // TensorImage image = TensorImage.fromBitmap(rgbFrameBitmap);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int [] intValues = new int[224 * 224];
        rgbFrameBitmap.getPixels(intValues, 0, rgbFrameBitmap.getWidth(), 0, 0, rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight());
        int pixel = 0;
        for(int i = 0; i < 224; i++){
            for(int j = 0; j < 224; j++){
                int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }
        inputFeature0.loadBuffer(byteBuffer);
        Modelomascota.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        //Modelomascota.Outputs outputs = model.process(image);
        //List<Category> probability = outputs.getProbabilityAsCategoryList();
       // Collections.sort(probability, new CategoryComparator());
       // String res="";
       // for (int i = 0; i < probability.size(); i++)
       //     res = res + probability.get(i).getLabel() +  " " +  probability.get(i).getScore()*100 + " % \n";
       
        //txtResults.setText(res);
        txtResults.setText(labels[getMax(outputFeature0.getFloatArray())] + " ");
        model.close();
    } catch (IOException e) {
        txtResults.setText("Error al procesar Modelo");
    }

    postInferenceCallback.run();
}

    int getMax(float[] arr) {
        int max = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > arr[max]) max = i;
        }
        return max;
    }

}


class CategoryComparator implements java.util.Comparator<Category> {
    @Override
    public int compare(Category a, Category b) {
        return (int)(b.getScore()*100) - (int)(a.getScore()*100);
    }
}