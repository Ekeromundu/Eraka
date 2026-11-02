package na.eraka.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WhisperHelper {

    public static String getModelPath(Context context, String modelName) {
        File file = new File(context.getFilesDir(), modelName);

        try {
            long assetLength = getAssetLength(context, modelName);

            if (!file.exists() || file.length() != assetLength) {
                copyModelFromAssets(
                        context,
                        modelName,
                        file,
                        assetLength
                );
            }

            return file.getAbsolutePath();

        } catch (IOException e) {
            android.util.Log.e(
                    "whisper",
                    "Unable to prepare Whisper model",
                    e
            );
            return null;
        }
    }

    /**
     * Deletes the previously copied model and copies a fresh version
     * from the assets folder.
     */
    public static String refreshModelPath(
            Context context,
            String modelName
    ) {
        File file = new File(context.getFilesDir(), modelName);
        File tempFile = new File(
                context.getFilesDir(),
                modelName + ".tmp"
        );

        try {
            if (tempFile.exists() && !tempFile.delete()) {
                throw new IOException(
                        "Could not delete temporary Whisper model"
                );
            }

            if (file.exists() && !file.delete()) {
                throw new IOException(
                        "Could not delete existing Whisper model"
                );
            }

            long assetLength = getAssetLength(context, modelName);

            copyModelFromAssets(
                    context,
                    modelName,
                    file,
                    assetLength
            );

            return file.getAbsolutePath();

        } catch (IOException e) {
            android.util.Log.e(
                    "whisper",
                    "Unable to refresh Whisper model",
                    e
            );
            return null;
        }
    }

    private static long getAssetLength(
            Context context,
            String modelName
    ) throws IOException {

        try (AssetFileDescriptor descriptor =
                     context.getAssets().openFd(modelName)) {

            return descriptor.getLength();
        }
    }

    private static void copyModelFromAssets(
            Context context,
            String modelName,
            File file,
            long expectedLength
    ) throws IOException {

        File tempFile = new File(
                file.getParentFile(),
                modelName + ".tmp"
        );

        if (tempFile.exists() && !tempFile.delete()) {
            throw new IOException(
                    "Could not delete stale Whisper model temp file"
            );
        }

        long copiedLength = 0;

        try (
                InputStream inputStream =
                        context.getAssets().open(modelName);

                FileOutputStream outputStream =
                        new FileOutputStream(tempFile)
        ) {
            byte[] buffer = new byte[8192];
            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                copiedLength += read;
            }

            outputStream.flush();
            outputStream.getFD().sync();
        } catch (IOException e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }

            throw e;
        }

        if (copiedLength != expectedLength) {
            tempFile.delete();

            throw new IOException(
                    "Incomplete Whisper model copy. Expected "
                            + expectedLength
                            + " bytes but copied "
                            + copiedLength
            );
        }

        if (file.exists() && !file.delete()) {
            tempFile.delete();

            throw new IOException(
                    "Could not replace stale Whisper model file"
            );
        }

        if (!tempFile.renameTo(file)) {
            tempFile.delete();

            throw new IOException(
                    "Could not finalize Whisper model file"
            );
        }
    }
}