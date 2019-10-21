/* FileUtility.java */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

public class FileUtility {

    /**
     * Nota: sorgente e destinazione devono essere correttamente aperti e chiusi
     * da chi invoca questa funzione.
     */
    static protected void trasferisci_a_byte_file_binario(DataInputStream src,
                                                          DataOutputStream dest, long length) throws IOException {


        int numByteRead = 0;
        int buffer = 0;
        try {
            // esco dal ciclo fino a quando non ho letto gli n byte che compongono il file
            while (numByteRead < length) {
                //leggo un byte dalla DataInputStream
                buffer = src.read();
                //scrivo quel byte sul DataOutputStream
                dest.write(buffer);
                //incremento il numero di byte letti
                numByteRead++;
            }
            dest.flush();
        } catch (EOFException e) {
            System.out.println("Problemi, i seguenti: ");
            e.printStackTrace();
        }
    }
}
