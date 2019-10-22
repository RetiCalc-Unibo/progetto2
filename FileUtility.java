import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

public class FileUtility {

    // Nota: sorgente e destinazione devono essere correttamente aperti e chiusi da chi invoca questa funzione

    static protected void trasferisci_a_byte_file_binario(DataInputStream src, DataOutputStream dest, long length) throws IOException {
        int numByteRead = 0;
        int buffer = 0;

        try {
            // Il ciclo itera fino a quando non ho letto gli n byte che compongono il file
            while (numByteRead < length) {
                buffer = src.read(); // Leggo un byte dalla DataInputStream
                dest.write(buffer); // Scrivo quel byte sul DataOutputStream
                numByteRead++; // Incremento il contatore del numero di byte letti
            }
            dest.flush();
        } catch (EOFException e) {
            System.out.println("Problemi, i seguenti: ");
            e.printStackTrace();
        }
    }
}