import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Client {

    public static void main(String[] args) throws IOException {
        InetAddress addr = null;
        int port = -1;
        long threshold = 0;

        try {
            if (args.length == 2) {
                addr = InetAddress.getByName(args[0]);
                port = Integer.parseInt(args[1]);
            } else {
                System.out.println("Usage: java Client serverIp serverPort");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Problemi, i seguenti: ");
            e.printStackTrace();
            System.out.println("Usage: java Client serverIp serverPort threshold>0");
            System.exit(2);
        }

        // Oggetti utilizzati dal client per la comunicazione e la lettura del file locale
        Socket socket = null;
        FileInputStream inFile = null;
        DataInputStream inSock = null;
        DataOutputStream outSock = null;
        String dirname = null;
        File directory = null;

        // Creazione stream di input da tastiera
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        try {
            socket = new Socket(addr, port);
            socket.setSoTimeout(30000);
            System.out.println("Creata la socket: " + socket);
            inSock = new DataInputStream(socket.getInputStream());
            outSock = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Problemi nella creazione degli stream su socket: ");
            e.printStackTrace();
            System.exit(3);
        }

        System.out.print("Client Started.\n\n^D(Unix)/^Z(Win)+invio per uscire, oppure inserisci il nome di una directory esistente: ");

        try {
            while ((dirname = stdIn.readLine()) != null) {
                System.out.print("Client Started.\n\n^D(Unix)/^Z(Win)+invio per uscire, oppure inserisci la dimensione minima dei file per la directory " + dirname + ": ");

                // Il metodo parseUnsignedLong lancia NumberFormatException se è inserito un numero negativo o se è mal formattata
                threshold = Long.parseUnsignedLong(stdIn.readLine());
                directory = new File(dirname);
                if (!directory.isDirectory()) {
                    System.out.println("Directory non presente: " + dirname);
                    System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                    continue;
                }

                String response = null;

                /* Itero dentro la directory e invio tutti i file */
                // Creazione stream di input da file
                try {
                    for (File f : directory.listFiles()) {
                        if (f.length() < threshold) {
                            continue;
                        }

                        inFile = new FileInputStream(f);
                        outSock.writeUTF(f.getName()); // Invio il nome del file

                        response = inSock.readUTF();
                        if (response.equals("salta")) {
                            System.out.println("Il file " + f.getName() + " esiste già nel server");
                            continue;
                        } else if (response.equals("attiva")) {
                            System.out.println("Invio lunghezza file: " + f.length());
                            outSock.writeLong(f.length());
                            System.out.println("Inizio la trasmissione di " + f.getName());

                            // Trasferimento file
                            try {
                                FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(inFile), outSock, f.length());
                                inFile.close(); // Chiusura file
                                System.out.println("Trasmissione di " + f + " terminata ");
                            } catch (Exception e) {
                                System.out.println("Problemi nell'invio di " + f.getName() + ": ");
                                e.printStackTrace();
                                socket.close();
                                System.exit(4);
                            }
                        }
                    }

                    System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                } catch (NullPointerException e) { // nullPointerException lanciata se non esiste directory
                    System.out.println("Problemi nella creazione dello stream di input da " + dirname + ": ");
                    e.printStackTrace();
                    System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory esistente: ");
                    continue;
                }
            }

            // Chiudo la socket fuori dal while in quanto non arrivano più nomi di directory
            socket.close();
            System.out.println("Client: termino...");

            // Qui catturo le eccezioni non catturate all'interno del while, come ad esempio la caduta della connessione
            // con il server in seguito a cui il client termina l'esecuzione
        } catch (Exception e) {
            System.err.println("Errore! ");
            e.printStackTrace();
            System.err.println("Chiudo!");
            System.exit(5);
        }
    }
}