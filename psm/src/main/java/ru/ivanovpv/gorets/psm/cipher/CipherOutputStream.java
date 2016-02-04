package ru.ivanovpv.gorets.psm.cipher;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by pivanov on 17.02.2015.
 */
public class CipherOutputStream extends OutputStream {
    private static final String TAG=CipherOutputStream.class.getName();
    static final int BUFFER_SIZE=1024;
    private OutputStream os; //stream to write cipher data
    private byte[] inBuffer;
    private int inMarker;
    private Cipher cipher;

    public CipherOutputStream(OutputStream os, Cipher cipher) {
        this.os=os;
        inBuffer=new byte[BUFFER_SIZE];
        inMarker=0;
        this.cipher=cipher;
    }

    @Override
    public void write(int oneByte) throws IOException {
        inBuffer[inMarker++]=(byte )oneByte;
        if(inMarker==BUFFER_SIZE) {
            this.encryptAndWrite();
        }
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    @Override
    public void  write (byte[] buffer, int offset, int count) throws IOException {
        for(int i=0; i < count; i++) {
            this.write(buffer[i+offset]);
        }
    }

    @Override
    public void flush() throws IOException {
        this.encryptAndWrite();
        os.flush();
    }

    /**
     * encrypts current buffer and flushes it to supplied stream
     * @throws IOException
     */
    private void encryptAndWrite() throws IOException {
        byte[] buffer;
        if(inMarker!=BUFFER_SIZE) {
            byte[] buf=new byte[inMarker];
            System.arraycopy(inBuffer, 0, buf, 0, inMarker);
            buffer = cipher.encryptBuffer(buf);
        }
        else {
            buffer = cipher.encryptBuffer(inBuffer);
        }
        os.write(buffer);
        inMarker=0;
    }

    @Override
    public void close() throws IOException{
        this.flush();
        super.close();
    }
}
