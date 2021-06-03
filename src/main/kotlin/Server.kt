import java.io.ByteArrayInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import javax.sound.sampled.*
import kotlin.concurrent.thread


class Server(port: Int = 25565, val serverSocket: DatagramSocket = DatagramSocket(port)) {
    lateinit var audioInputStream: AudioInputStream
    lateinit var ais: AudioInputStream
    var format: AudioFormat
    var status = true
    var sampleRate = 44100f

    var dataLineInfo: DataLine.Info
    var sourceDataLine: SourceDataLine

    init {
        System.out.println("Server started at port:$port")


        /**
         * Formula for lag = (byte_size/sample_rate)*2
         * Byte size 9728 will produce ~ 0.45 seconds of lag. Voice slightly broken.
         * Byte size 1400 will produce ~ 0.06 seconds of lag. Voice extremely broken.
         * Byte size 4000 will produce ~ 0.18 seconds of lag. Voice slightly more broken then 9728.
         */

        /**
         * Formula for lag = (byte_size/sample_rate)*2
         * Byte size 9728 will produce ~ 0.45 seconds of lag. Voice slightly broken.
         * Byte size 1400 will produce ~ 0.06 seconds of lag. Voice extremely broken.
         * Byte size 4000 will produce ~ 0.18 seconds of lag. Voice slightly more broken then 9728.
         */


        format = AudioFormat(sampleRate, 16, 2, true, true)
        dataLineInfo = DataLine.Info(SourceDataLine::class.java, format)
        sourceDataLine = AudioSystem.getLine(dataLineInfo) as SourceDataLine
        sourceDataLine.open(format)
        sourceDataLine.start()

        val volumeControl:FloatControl  = sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl;
        volumeControl.value = 0.10f


        startListening()
    }

    fun startListening() {
        val receiveData = ByteArray(4096)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)

        val byteArrayInputStream = ByteArrayInputStream(receivePacket.data)

        thread {
            while(true) if(serverSocket.isConnected) println("Connected!")
        }

        while (status) {
            println("Listening")
            serverSocket.receive(receivePacket)
            println("Received")
            println(receiveData.contentToString())
            ais = AudioInputStream(byteArrayInputStream, format, receivePacket.length.toLong())
            toSpeaker(receivePacket.data)
        }

        sourceDataLine.drain()
        sourceDataLine.close()
    }

    fun toSpeaker(soundbytes: ByteArray) {
        try {
            println("At the speaker")
            sourceDataLine.write(soundbytes, 0, soundbytes.size)
        } catch (e: Exception) {
            println("Not working in speakers...")
            e.printStackTrace()
        }
    }
}