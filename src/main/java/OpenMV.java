import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import com.fazecast.jSerialComm.SerialPort;

import utils.JStruct;

public class OpenMV{
    private JStruct struct = new JStruct();
    private SerialPort connection;

    private static int FB_HDR_SIZE   = 12;

    //USB Debug commands
    private static byte USBDBG_CMD            = (byte) 48  ;
    private static byte USBDBG_FW_VERSION     = (byte) 0x80;
    private static byte USBDBG_FRAME_SIZE     = (byte) 0x81;
    private static byte USBDBG_FRAME_DUMP     = (byte) 0x82;
    private static byte USBDBG_ARCH_STR       = (byte) 0x83;
    private static byte USBDBG_SCRIPT_EXEC    = (byte) 0x05;
    private static byte USBDBG_SCRIPT_STOP    = (byte) 0x06;
    private static byte USBDBG_SCRIPT_SAVE    = (byte) 0x07;
    private static byte USBDBG_SCRIPT_RUNNING = (byte) 0x87;
    private static byte USBDBG_TEMPLATE_SAVE  = (byte) 0x08;
    private static byte USBDBG_DESCRIPTOR_SAVE= (byte) 0x09;
    private static byte USBDBG_ATTR_READ      = (byte) 0x8A;
    private static byte USBDBG_ATTR_WRITE     = (byte) 0x0B;
    private static byte USBDBG_SYS_RESET      = (byte) 0x0C;
    private static byte USBDBG_FB_ENABLE      = (byte) 0x0D;
    private static byte USBDBG_TX_BUF_LEN     = (byte) 0x8E;
    private static byte USBDBG_TX_BUF         = (byte) 0x8F;

    private static int ATTR_CONTRAST         = (byte) 0;
    private static int ATTR_BRIGHTNESS       = (byte) 1;
    private static int ATTR_SATURATION       = (byte) 2;
    private static int ATTR_GAINCEILING      = (byte) 3;

    private static long BOOTLDR_START         = 0xABCD0001;
    private static long BOOTLDR_RESET         = 0xABCD0002;
    private static long BOOTLDR_ERASE         = 0xABCD0004;
    private static long BOOTLDR_WRITE         = 0xABCD0008;
    
    public OpenMV(int baudRate, int timeout) {

        for(SerialPort port: SerialPort.getCommPorts()){
            if(port.getPortDescription().contains("OpenMV")){
                System.out.println(port.getPortDescription());
                connection = port;
            }
        }
        connection.setBaudRate(baudRate);
        connection.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, timeout, timeout);
        connection.openPort();
        System.out.println(connection.isOpen());
        System.out.println(this.read(10));
    }

    public static void main(String[] args) throws Exception {
        OpenMV openMV = new OpenMV( 921600, 0);
        openMV.enableFb(5);
        while(true){
            Thread.sleep(20);
            openMV.fbDump("test.jpg");
        }
        
    }

    private void write(byte[] bytes) {
        //printStringByteBuffer(bytes);
        connection.writeBytes(bytes, bytes.length);
    }

    private byte[] read(long numBytes) {
        var buff = new byte[(int)numBytes];
        connection.readBytes(buff, numBytes);
        return buff;
    }

    public long[] fbSize() throws Exception {
        write(struct.pack("<BBI", USBDBG_CMD, USBDBG_FRAME_SIZE, FB_HDR_SIZE));
        var buffer = read(12);
        return struct.unpack("III", buffer);
    }

    public void execScript(byte[] buff) throws Exception {
        write(struct.pack("<BBI", USBDBG_CMD,USBDBG_SCRIPT_EXEC, buff.length));
        write(buff);
    }

    public void stopScript() throws Exception {
        write(struct.pack("<BBI", USBDBG_CMD,USBDBG_SCRIPT_STOP,0));
    }

    public boolean scriptRunning() throws Exception {
        write(struct.pack("<BBI", USBDBG_CMD, USBDBG_SCRIPT_RUNNING, 4));
        return struct.unpack("I", read(4))[0] != 0;
    }

    public void reset() throws Exception {
        write(struct.pack("<BBI", USBDBG_CMD, USBDBG_SYS_RESET, 0));
    }

    public boolean bootloaderStart() throws Exception {
        write(struct.pack("<I", BOOTLDR_START));
        return struct.unpack("I", read(4))[0] == BOOTLDR_START;
    }

    public void bootloaderReset() throws Exception {
        write(struct.pack("<I", BOOTLDR_RESET));
    }

    public void flashErase(byte sector) throws Exception {
        write(struct.pack("<II", BOOTLDR_ERASE, sector));
    }

    public long txBufLen() throws Exception {
        write(struct.pack("<BBI", USBDBG_CMD, USBDBG_TX_BUF_LEN, 4));
        return struct.unpack("I", read(4))[0];
    }

    public byte[] txBuf(byte bytes) throws Exception {
        write(struct.pack("<BBI", USBDBG_CMD, USBDBG_TX_BUF, bytes));
        return read(bytes);
    }

    public long[] fwVersion() throws Exception {
        write(struct.pack("<BBI", USBDBG_CMD, USBDBG_FW_VERSION, 12));
        return struct.unpack("III", read(12));
    }

    public void enableFb(int enable) throws Exception {
        write(struct.pack("<BBI", USBDBG_CMD, USBDBG_FB_ENABLE, 4));
        write(struct.pack("<I", enable));
    }

    public void fbDump(String s) throws Exception {
        long[] size = fbSize();
        
        if (size[0] == 0){
            return;
        }
        long numBytes = 0;
        if (size[2] > 2){ //JPEG
            numBytes = size[2];
        }
        else
            numBytes = size[0]*size[1]*size[2];

        // read fb data
        write(struct.pack("<BBI", (long)USBDBG_CMD, (long)USBDBG_FRAME_DUMP, numBytes));
        byte[] buff = read((int)numBytes);

        FileOutputStream output = new FileOutputStream(new File(s));
        output.write(buff);
        output.close();
    }
}