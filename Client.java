// Client.java

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

        // oggetti utilizzati dal client per la comunicazione e la lettura del file
        // locale
        Socket socket = null;
        FileInputStream inFile = null;
        DataInputStream inSock = null;
        DataOutputStream outSock = null;
        String dirname = null;
        File directory = null;

        // creazione stream di input da tastiera
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
            System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory esistente: ");

        }

        System.out
                .print("Client Started.\n\n^D(Unix)/^Z(Win)+invio per uscire, oppure inserisci il nome di una directory esistente: ");
        try {
            while ((dirname = stdIn.readLine()) != null) {
                System.out.print("Client Started.\n\n^D(Unix)/^Z(Win)+invio per uscire, oppure inserisci la dimensione minima dei file per la directory " + dirname + ": ");

                //il metodo parseUnsignedLong lancia NumberFormatException se è inserito un num.negativo o se è mal formattata
                threshold = Long.parseUnsignedLong(stdIn.readLine());
                directory = new File(dirname);
                if (!directory.isDirectory()) {
                    System.out.println("Directory non presente: " + dirname);
                    System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                    // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                    continue;
                }

                String response = null;

                /* itero dentro la directory e invio tutti i file */
                // creazione stream di input da file
                try {
                    for (File f : directory.listFiles()) {
                        if (f.length() < threshold) continue;

                        inFile = new FileInputStream(f);
                        //Invio il nome del file
                        outSock.writeUTF(f.getName());

                        response = inSock.readUTF();

                        if (response.equals("salta")) {
                            System.out.println("Il file " + f.getName() + " esiste già nel server");

                            // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                            continue;
                        } else if (response.equals("attiva")) {

                            System.out.println("Invio lunghezza file: " + f.length());
                            outSock.writeLong(f.length());
                            System.out.println("Inizio la trasmissione di " + f.getName());

                            // trasferimento file
                            try {
                                FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(inFile), outSock, f.length());
                                inFile.close();            // chiusura file
                                System.out.println("Trasmissione di " + f + " terminata ");

                            } catch (SocketTimeoutException ste) {
                                System.out.println("Timeout scattato: ");
                                ste.printStackTrace();
                                System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory esistente: ");
                                // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                                continue;
                            } catch (Exception e) {
                                System.out.println("Problemi nell'invio di " + f.getName() + ": ");
                                e.printStackTrace();
                                socket.close();
                                System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory esistente: ");
                                // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                                continue;
                            }
                        }
                    }
                    System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                    //nullPointerException lanciata se non esiste directory
                } catch (NullPointerException e) {
                    System.out.println("Problemi nella creazione dello stream di input da "
                            + dirname + ": ");
                    e.printStackTrace();
                    System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory esistente: ");
                    // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                    continue;
                }

            }
            //chiudo la socket fuori dal while, poichè non arrivano più nomi di directory
            socket.close();
            System.out.println("Client: termino...");

            // qui catturo le eccezioni non catturate all'interno del while
            // quali per esempio la caduta della connessione con il server
            // in seguito alle quali il client termina l'esecuzione
        } catch (Exception e) {
            System.err.println("Errore! ");
            e.printStackTrace();
            System.err.println("Chiudo!");
            System.exit(3);
        }
    } // main
}
