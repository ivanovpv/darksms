package ru.ivanovpv.gorets.psm.cipher;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by pivanov on 17.02.2015.
 */
public class CipherInputStream extends InputStream {
    private static final String TAG=CipherInputStream.class.getName();
    static final int BUFFER_SIZE=CipherOutputStream.BUFFER_SIZE; //should be identical to those
    private InputStream is; //stream to read ciphered data
    private byte[] inBuffer;
    private int inMarker;
    private Cipher cipher;
    private final int bufferSize;


    public CipherInputStream(InputStream is, Cipher cipher) throws IOException, CipherException {
        this.is=is;
        this.cipher=cipher;
        bufferSize=cipher.getBlockSize()+4+(BUFFER_SIZE/cipher.getBlockSize())*cipher.getBlockSize();
        inMarker=0;
        readAndDecrypt();
    }

    /**
     * Reads from stream and puts in inBuffer decrypted data
     * @throws IOException
     * @throws CipherException
     */
    private void readAndDecrypt() throws IOException, CipherException {
        byte[] buffer=new byte[bufferSize];
        int size=is.read(buffer);
        inBuffer=cipher.decryptBuffer(buffer);
        if(size==-1)
            inBuffer=new byte[0];
        else if(size!=bufferSize) {
            byte[] buf=new byte[size];
            System.arraycopy(buffer, 0, buf, 0, size);
            inBuffer=cipher.decryptBuffer(buffer);
        }
        inMarker=0;
    }

    @Override
    public int read() throws IOException {
        if(inMarker < inBuffer.length)
            return inBuffer[inMarker++];
        else {
            try {
                readAndDecrypt();
            }
            catch(CipherException ex) {
                throw new IOException("Error decrypting from stream", ex);
            }
            if(inBuffer.length==0)
                return -1;
            return inBuffer[inMarker++];
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        int size, b;
        for(size = 0; size < count; size++) {
            b=this.read();
            if(b==-1)
                break;
            buffer[size+offset]=(byte )b;
        }
        return size;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    @Override
    public void close() throws IOException {
        is.close();
        inMarker=0;
        inBuffer=null;
    }
}
