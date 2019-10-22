import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

// Viene lanciato un thread per ogni richiesta accettata
// Versione per il trasferimento di file binari
class ServerThread extends Thread {

    private Socket clientSocket = null;

    public ServerThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        DataInputStream inSock;
        DataOutputStream outSock;
        String fileName = null;

        // Creazione stream di input e out da socket
        try {
            // Nota: getInputStream e getOutputStream possono sollevare IOException
            inSock = new DataInputStream(clientSocket.getInputStream());
            outSock = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException ioe) {
            System.out.println("Problemi nella creazione degli stream di input/output " + "su socket: ");
            ioe.printStackTrace();
            return;
        }

        try {
            try {
                // Ciclo while che legge fino a quando non vengono più mandati i nomiFile attraverso la socket
                // (Per esempio quando si preme CTRL+D e quindi non ci sono più file delle cartelle da leggere, quindi la socket è chiusa)
                while (true) {
                    fileName = inSock.readUTF();
                    FileOutputStream outFile = null;
                    String result = null;
                    File curFile = new File(fileName);

                    // Se result = "salta", il file esiste e il server deve avvertire il cliente di saltare al file successivo
                    // Se result = "attiva", occorre che il server chieda di inviare il resto del file (dimensione e dati)

                    if (curFile.exists()) {
                        result = "salta";
                        outSock.writeUTF(result);
                    } else {
                        result = "attiva";
                        outFile = new FileOutputStream(fileName);
                        long fileLength = 0;

                        outSock.writeUTF(result);
                        outSock.flush(); // Svuoto il buffer per performance migliori
                        fileLength = inSock.readLong();

                        System.out.println("Ricevo il file " + fileName);
                        FileUtility.trasferisci_a_byte_file_binario(inSock, new DataOutputStream(outFile), fileLength);
                        System.out.println("Ricezione del file " + fileName + " e copia nel server terminata\n");
                        outFile.close();
                    }
                }
                // Fine ricezione dei file: EOF
            } catch (EOFException eof) {
                System.out.println("Raggiunta la fine delle ricezioni, chiudo...");
                clientSocket.close();
                System.out.println("Server: termino...");
            } catch (SocketTimeoutException ste) {
                System.out.println("Timeout scattato: ");
                ste.printStackTrace();
                clientSocket.close();
            } catch (Exception e) {
                System.out.println("Problemi, i seguenti : ");
                e.printStackTrace();
                System.out.println("Chiudo ed esco...");
                clientSocket.close();
                System.exit(1);
            }
        } catch (IOException ioe) {
            System.out.println("Problemi nella chiusura della socket: ");
            ioe.printStackTrace();
            System.out.println("Chiudo ed esco...");
            System.exit(2);
        }
    }
}

public class Server {
    public static final int PORT = 1050; // Default port

    public static void main(String[] args) throws IOException {
        int port = -1;

        /* Controllo argomenti */
        try {
            if (args.length == 1) {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    System.out.println("Usage: java LineServer [serverPort>1024]");
                    System.exit(3);
                }
            } else if (args.length == 0) {
                port = PORT;
            } else {
                System.out.println("Usage: java ServerThread or java ServerThread port");
                System.exit(4);
            }
        }
        catch (Exception e) {
            System.out.println("Problemi, i seguenti: ");
            e.printStackTrace();
            System.out.println("Usage: java ServerThread or java ServerThread port");
            System.exit(5);
        }

        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            System.out.println("Server: avviato ");
            System.out.println("Server: creata la server socket: " + serverSocket);
        } catch (Exception e) {
            System.err.println("Server: problemi nella creazione della server socket: " + e.getMessage());
            e.printStackTrace();
            System.exit(6);
        }

        try {
            while (true) {
                System.out.println("Server: in attesa di richieste...\n");

                try {
                    // Bloccante fino ad una pervenuta connessione
                    clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(30000);
                    System.out.println("Server: connessione accettata: " + clientSocket);
                } catch (Exception e) {
                    System.err.println("Server: problemi nella accettazione della connessione: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

                // Servizio delegato ad un nuovo thread
                try {
                    new ServerThread(clientSocket).start();
                } catch (Exception e) {
                    System.err.println("Server: problemi nel server thread: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        // Qui catturo le eccezioni non catturate all'interno del while in seguito alle quali il server termina l'esecuzione
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Server: termino...");
            System.exit(7);
        }
    }
}