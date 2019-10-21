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
            System.out.println("Usage: java Client serverIp serverPort");
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
        System.out
                .print("Client Started.\n\n^D(Unix)/^Z(Win)+invio per uscire, oppure inserisci il nome di una directory esistente: ");

        try {
            while ((dirname = stdIn.readLine()) != null) {
                System.out
                        .print("Client Started.\n\n^D(Unix)/^Z(Win)+invio per uscire, oppure inserisci la dimensione minima dei file per la directory selezionata");

                //il metodo parseUnsignedLong lancia NumberFormatException se è inserito un num.negativo o se è mal formattata
                threshold = Long.parseUnsignedLong(stdIn.readLine());
                // se la directory esiste creo la socket
                if ((directory = new File(dirname)).isDirectory()) {
                    // creazione socket
                    try {
                        socket = new Socket(addr, port);
                        socket.setSoTimeout(30000);
                        System.out.println("Creata la socket: " + socket);
                    } catch (Exception e) {
                        System.out.println("Problemi nella creazione della socket: ");
                        e.printStackTrace();
                        System.out
                                .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                        continue;
                        // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                    }

                    // creazione stream di input/output su socket
                    try {
                        inSock = new DataInputStream(socket.getInputStream());
                        outSock = new DataOutputStream(socket.getOutputStream());
                    } catch (IOException e) {
                        System.out
                                .println("Problemi nella creazione degli stream su socket: ");
                        e.printStackTrace();
                        System.out
                                .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                        continue;
                        // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                    }
                }
                // se la richiesta non � corretta non proseguo
                else {
                    System.out.println("Directory non presente.");
                    System.out
                            .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                    // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                    continue;
                }

                String risposta = null;

                /* itero dentro la directory e invio tutti i file */
                // creazione stream di input da file
                //NB se non esiste la directory viene sollevata nullPointerException
                try {
                    for (File f : directory.listFiles()) {

                        if (f.length() < threshold) continue;

                        inFile = new FileInputStream(f);

                        // trasmissione del nome
                        try {
                            //Invio il nome del file
                            outSock.writeUTF(f.getName());
                            System.out.println("Inviato il nome del file " + f);
                        } catch (Exception e) {
                            System.out.println("Problemi nell'invio del nome di " + f
                                    + ": ");
                            e.printStackTrace();
                            System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                            // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                            continue;
                        }

                        risposta = inSock.readUTF();

                        if (risposta.equals("salta")) {
                            System.out.println("Il file esiste già nel server");
                            // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                            continue;
                        } else if (risposta.equals("attiva")) {

                            System.out.println("Invio lunghezza file: " + f.length());
                            outSock.writeLong(f.length());
                            System.out.println("Inizio la trasmissione di " + f);

                            // trasferimento file
                            try {
                                //FileUtility.trasferisci_a_linee_UTF_e_stampa_a_video(new DataInputStream(inFile), outSock);
                                FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(inFile), outSock, f.length());
                                inFile.close();            // chiusura file
                                System.out.println("Trasmissione di " + f + " terminata ");
                            } catch (SocketTimeoutException ste) {
                                System.out.println("Timeout scattato: ");
                                ste.printStackTrace();
                                System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
                                // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                                continue;
                            } catch (Exception e) {
                                System.out.println("Problemi nell'invio di " + f + ": ");
                                e.printStackTrace();
                                socket.close();
                                System.out
                                        .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
                                // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                                continue;
                            }
                        }
                    }
                    //nullPointerException lanciata se non esiste directory
                } catch (NullPointerException e) {
                    System.out
                            .println("Problemi nella creazione dello stream di input da "
                                    + dirname + ": ");
                    e.printStackTrace();
                    System.out
                            .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome directory esistente: ");
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
            System.err.println("Errore irreversibile, il seguente: ");
            e.printStackTrace();
            System.err.println("Chiudo!");
            System.exit(3);
        }
    } // main
}
