import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread


class Client(host: String = "localhost", port: Int = 25565, val socket: Socket = Socket(host, port)) {
    fun sendText(csq: CharSequence) {
        val outputWriter = socket.getOutputStream().bufferedWriter()
        outputWriter.append(csq)
        outputWriter.flush()
        outputWriter.close()
    }

    suspend fun startListening() = coroutineScope {
        launch {
            val inputReader = socket.getInputStream().bufferedReader()
            while (true) {
                inputReader.read()
            }
        }
    }

    /**
     * Protocol: Always send @param chunkSize (default:4096) bytes
     */
    fun startListeningAndPrinting(chunkSize:Int = 4096) = thread {
        while(true){
            val bytes = ByteArray(chunkSize)
            var numberOfReadBytes = socket.getInputStream().read(bytes)
            if(numberOfReadBytes==-1) {
                Thread.sleep(500)
                continue
            }
            bytes.map { b-> b.toInt().toChar() }.take(numberOfReadBytes).forEach(::print)
        }
    }

    fun startListeningAndEmmittingAudio() = thread{
        var line: TargetDataLine
        var dgp: DatagramPacket

        val encoding = AudioFormat.Encoding.PCM_SIGNED
        val rate = 44100.0f
        val channels = 2
        val sampleSize = 16
        val bigEndian = true
        var addr: InetAddress
        val format = AudioFormat(encoding, rate, sampleSize, channels, sampleSize / 8 * channels, rate, bigEndian)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        if (!AudioSystem.isLineSupported(info)) {
            error("Line matching $info not supported.")
        }

        try{
            line = AudioSystem.getLine(info) as TargetDataLine

            var buffsize = line.bufferSize / 5
            buffsize += 512

            line.open(format)

            line.start()

            var numBytesRead: Int
            val data = ByteArray(buffsize)

            addr = InetAddress.getByName("127.0.0.1")
            val socket = DatagramSocket()
            while (true) {
                // Read the next chunk of data from the TargetDataLine.
                numBytesRead = line.read(data, 0, data.size)
                // Save this chunk of data.
                dgp = DatagramPacket(data, data.size, addr, 50005)
                socket.send(dgp)
            }
        }catch (e:Exception){
            TODO("Handle exceptions")
        }
    }
}