import java.io.*;

public class FileUtility {
	static protected void trasferisci_a_byte_file_binario(DataInputStream src, DataOutputStream dest) throws IOException {
		int buffer;

		// ciclo di lettura da sorgente e scrittura su destinazione
		// N.B.: la funzione consuma l'EOF
	    try {
	    	while ((buffer = src.read()) >= 0) {
	    		dest.write(buffer);
	    	}

	    	dest.flush();
	    } catch (EOFException e) {
	    	System.out.println("Sono stati riscontrati i seguenti problemi: ");
	    	e.printStackTrace();
	    }
	}
}