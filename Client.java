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
            if (args.length == 3) {
                addr = InetAddress.getByName(args[0]);
                port = Integer.parseInt(args[1]);
                threshold = Long.parseUnsignedLong(args[2]); // Il metodo lancia un'eccezione se è inserito un num.negativo
            } else {
                System.out.println("Usage: java Client serverIp serverPort threshold>0");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Sono stati riscontrati i seguenti problemi: ");
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
        System.out.print("Client Started.\n\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti un nome directory: ");

        try {
            while ((dirname = stdIn.readLine()) != null) {
                // Se la directory esiste, creo la socket
                if ((directory = new File(dirname)).isDirectory()) {
                    
                    try { // Creazione socket
                        socket = new Socket(addr, port);
                        socket.setSoTimeout(30000);
                        System.out.println("Creata la socket: " + socket);
                    } catch (Exception e) {
                        System.out.println("Problemi nella creazione della socket: ");
                        e.printStackTrace();
                        System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                        continue;
                    }

                    // Creazione stream di input/output su socket
                    try {
                        inSock = new DataInputStream(socket.getInputStream());
                        outSock = new DataOutputStream(socket.getOutputStream());
                    } catch (IOException e) {
                        System.out.println("Problemi nella creazione degli stream su socket: ");
                        e.printStackTrace();
                        System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                        continue;
                    }
                }
                else { // Se la richiesta non è corretta, non proseguo
                    System.out.println("Directory non presente.");
                    System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                    continue;
                }

                String risposta = null;

                /* Itero dentro la directory e invio tutti i file */
                // Creazione stream di input da file
                try {
                    for (File f : directory.listFiles()) {
                        if (f.length() < threshold) {
                            continue;
                        }

                        inFile = new FileInputStream(f);

                        // Trasmissione del nome
                        try {
                            // Invio il nome del file
                            outSock.writeUTF(f.getName());
                            System.out.println("Inviato il nome del file " + f);
                        } catch (Exception e) {
                            System.out.println("Problemi nell'invio del nome di " + f + ": ");
                            e.printStackTrace();
                            System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                            continue;
                        }

                        risposta = inSock.readUTF();

                        if (risposta.equals("salta")) {
                            System.out.println("Il file esiste già nel server");
                            continue;
                        }
                        else if (risposta.equals("attiva")) {
                            System.out.println("Invio lunghezza file: " + f.length());
                            outSock.writeLong(f.length());
                            System.out.println("Inizio la trasmissione di " + f);

                            // Trasferimento del file
                            try {
                                FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(inFile), outSock);
                                inFile.close();
                                System.out.println("Trasmissione di " + f + " terminata ");
                            } catch (SocketTimeoutException ste) {
                                System.out.println("Timeout scattato: ");
                                ste.printStackTrace();
                                System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
                                continue;
                            } catch (Exception e) {
                                System.out.println("Problemi nell'invio di " + f + ": ");
                                e.printStackTrace();
                                socket.close();
                                System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
                                continue;
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    System.out.println("Problemi nella creazione dello stream di input da " + dirname + ": ");
                    e.printStackTrace();
                    System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
                    continue;
                }
            }

            // Chiudo la socket fuori dal while, poiché non arrivano più nomi di directory
            socket.close();
            System.out.println("PutFileClient: termino...");
        } catch (Exception e) {
            // Qui catturo le eccezioni non catturate all'interno del while, come ad esempio la caduta
            // della connessione con il server in seguito alle quali il client termina l'esecuzione
            System.err.println("Errore irreversibile, il seguente: ");
            e.printStackTrace();
            System.err.println("Chiudo!");
            System.exit(3);
        }
    }
}