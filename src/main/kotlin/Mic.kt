import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.sound.sampled.*
import kotlin.math.sin


fun main() {
//    printMixers()
    Mic()
//    val format = AudioFormat(22000f, 16, 2, true, true)
//    val line = AudioSystem.getLine(getMic()?.sourceLineInfo?.first() ?: throw Exception()) as TargetDataLine;
//    line.open(format)
//    val format = AudioFormat(8000.0f, 16, 1, true, true)
//    val microphone = AudioSystem.getTargetDataLine(format)
//    microphone.open(format)
}

fun printMixers() {
    AudioSystem.getMixerInfo().map(AudioSystem::getMixer)
        .forEach { mixer -> println("mixer info: ${mixer.mixerInfo}\nsourceLineInfo: ${mixer.sourceLineInfo.contentDeepToString()}\ntargetLineInfo: ${mixer.targetLineInfo.contentDeepToString()}\n\n") }
}

fun getMic(name: String = "KT01"): Mixer? {
    return AudioSystem.getMixerInfo().map(AudioSystem::getMixer)
        .find { mixer -> "MICROPHONE" in mixer.sourceLineInfo.contentDeepToString().uppercase() }
}

class Mic {
    private val portToSendTo = 25565
    private val portToSendFrom = 55055
    lateinit var line: TargetDataLine
    val encoding: AudioFormat.Encoding = AudioFormat.Encoding.PCM_SIGNED
    val rate = 44100.0f
    val channels = 2
    val sampleSize = 16
    val bigEndian = true
    val addr: InetAddress = InetAddress.getByName("127.0.0.1")
    val format = AudioFormat(encoding, rate, sampleSize, channels, sampleSize / 8 * channels, rate, bigEndian)


    init {
        try {
            line = AudioSystem.getTargetDataLine(format)
            //TOTALLY missed this.
//                var buffsize = line.bufferSize / 5
//                buffsize += 512
            val buffsize = 4096
            line.open(format)
            line.start()
            val data = ByteArray(buffsize)

            /*
         * MICK's injection: We have a buffsize of 512; it is best if the frequency
         * evenly fits into this (avoid skips, bumps, and pops). Additionally, 44100 Hz,
         * with two channels and two bytes per sample. That's four bytes; divide
         * 512 by it, you have 128.
         *
         * 128 samples, 44100 per second; that's a minimum of 344 samples, or 172 Hz.
         * Well within hearing range; slight skip from the uneven division. Maybe
         * bump it up to 689 Hz.
         *
         * That's a sine wave of shorts, repeated twice for two channels, with a
         * wavelength of 32 samples.
         *
         * Note: Changed my mind, ignore specific numbers above.
         *
         */
//            data = testNoise()
            DatagramSocket(portToSendFrom).use { socket ->
                while (true) {
                    // Read the next chunk of data from the TargetDataLine.
                    val numBytesRead = line.read(data, 0, data.size);
                    // Save this chunk of data.
                    val dgp = DatagramPacket(data, data.size, addr, portToSendTo)
                    socket.send(dgp)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun testNoise(): ByteArray {
        val lambda = 16
        val buffer: ByteBuffer = ByteBuffer.allocate(lambda * 2 * 8 * 16)
        for (i in 0 until lambda * 4 * 16) {
            //once for each sample
            buffer.putShort(
                (sin(Math.PI * (lambda.toFloat() / i.toFloat())) * Short.MAX_VALUE).toInt()
                    .toShort()
            )
            buffer.putShort(
                (sin(Math.PI * (lambda.toFloat() / i.toFloat())) * Short.MAX_VALUE).toInt()
                    .toShort()
            )
        }
        return buffer.array()
    }
}