// Server Concorrente

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

// Thread lanciato per ogni richiesta accettata
// versione per il trasferimento di file binari
class ServerThread extends Thread {

    private Socket clientSocket = null;

    /**
     * Constructor
     *
     * @param clientSocket
     */
    public ServerThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        DataInputStream inSock;
        DataOutputStream outSock;

        String nomeFile = null;
        try {
            // creazione stream di input e out da socket
            //NB getInputStream e getOutputStream possono sollevare IOException
            inSock = new DataInputStream(clientSocket.getInputStream());
            outSock = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException ioe) {
            System.out
                    .println("Problemi nella creazione degli stream di input/output "
                            + "su socket: ");
            ioe.printStackTrace();
            return;
        }

        try {
            try {
                //ciclo while che legge fino a quando non vengono più mandati i nomiFile attraverso la socket
                //(per esempio quando si preme ctrl+D e quindi non ci sono più file delle cartelle da leggere)
                while ((nomeFile = inSock.readUTF()) != null) {
                    FileOutputStream outFile = null;
                    String esito = null;

                    File curFile = new File(nomeFile);
                    //esito = "salta":
                    // se il file esiste allora il server deve avvertire il cliente di saltare al file successivo
                    //esito = "attiva":
                    //occorre che il server chieda di inviare il resto del file (dimensione e dati)
                    if (curFile.exists()) {
                        esito = "salta";
                    } else esito = "attiva";
                    outFile = new FileOutputStream(nomeFile);

                    long fileLength = 0;

                    outSock.writeUTF(esito);
                    //svuoto il buffer per performance migliori
                    outSock.flush();
                    //nel caso di attiva salvo una copia del file nel server
                    if (esito.equals("attiva")) {
                        fileLength = inSock.readLong();
                        //ciclo di ricezione dal client, salvataggio file e stamapa a video

                        System.out.println("Ricevo il file " + nomeFile + ": \n");
                        FileUtility.trasferisci_a_byte_file_binario(inSock,
                                new DataOutputStream(outFile), fileLength);
                        System.out.println("\nRicezione del file " + nomeFile + " e copia nel server terminata\n");
                        // chiusura file
                        // NB metodo flush inutile, poichè chiudo il canale di OutputStream
                        outFile.close();

                    }
                }
                //fine ricezione dei file: EOF
            } catch (EOFException eof) {
                System.out.println("Raggiunta la fine delle ricezioni, chiudo...");
                clientSocket.close();
                System.out.println("Server: termino...");
                System.exit(0);
            } catch (SocketTimeoutException ste) {
                System.out.println("Timeout scattato: ");
                ste.printStackTrace();
                clientSocket.close();
                System.exit(1);
            } catch (Exception e) {
                System.out.println("Problemi, i seguenti : ");
                e.printStackTrace();
                System.out.println("Chiudo ed esco...");
                clientSocket.close();
                System.exit(2);
            }
        } catch (IOException ioe) {
            System.out.println("Problemi nella chiusura della socket: ");
            ioe.printStackTrace();
            System.out.println("Chiudo ed esco...");
            System.exit(3);
        }
    }

}

public class Server {
    public static final int PORT = 1050; //default port

    public static void main(String[] args) throws IOException {

        int port = -1;

        /* controllo argomenti */
        try {
            if (args.length == 1) {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    System.out.println("Usage: java LineServer [serverPort>1024]");
                    System.exit(1);
                }
            } else if (args.length == 0) {
                port = PORT;
            } else {
                System.out
                        .println("Usage: java ServerThread or java ServerThread port");
                System.exit(1);
            }
        } //try
        catch (Exception e) {
            System.out.println("Problemi, i seguenti: ");
            e.printStackTrace();
            System.out
                    .println("Usage: java ServerThread or java ServerThread port");
            System.exit(1);
        }

        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            System.out.println("Server: avviato ");
            System.out.println("Server: creata la server socket: " + serverSocket);
        } catch (Exception e) {
            System.err
                    .println("Server: problemi nella creazione della server socket: "
                            + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        try {

            while (true) {
                System.out.println("Server: in attesa di richieste...\n");

                try {
                    // bloccante fino ad una pervenuta connessione
                    clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(30000);
                    System.out.println("Server: connessione accettata: " + clientSocket);
                } catch (Exception e) {
                    System.err
                            .println("Server: problemi nella accettazione della connessione: "
                                    + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

                // serizio delegato ad un nuovo thread
                try {
                    new ServerThread(clientSocket).start();
                } catch (Exception e) {
                    System.err.println("Server: problemi nel server thread: "
                            + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

            } // while
        }
        // qui catturo le eccezioni non catturate all'interno del while
        // in seguito alle quali il server termina l'esecuzione
        catch (Exception e) {
            e.printStackTrace();
            // chiusura di stream e socket
            System.out.println("Server: termino...");
            System.exit(2);
        }

    }
} // Server class
