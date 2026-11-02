package na.eraka.utils

import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.runBlocking

object WhisperBridge {
    @JvmStatic
    @Throws(RuntimeException::class)
    fun createContext(modelPath: String): WhisperContext {
        return WhisperContext.createContextFromFile(modelPath)
    }

    @JvmStatic
    fun transcribe(context: WhisperContext, audio: FloatArray): String = runBlocking {
        context.transcribeData(audio, false)
    }

    /** Eraka: Whisper's estimate (0..1) that the last audio contained no speech. */
    @JvmStatic
    fun noSpeechProb(context: WhisperContext): Float = runBlocking {
        context.getNoSpeechProb()
    }

    /** Eraka: vocabulary hint (the child's picture words). "" clears it. */
    @JvmStatic
    fun setPrompt(context: WhisperContext, prompt: String) {
        context.setInitialPrompt(prompt)
    }

    @JvmStatic
    fun release(context: WhisperContext) = runBlocking {
        context.release()
    }
}