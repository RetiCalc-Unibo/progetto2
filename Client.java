// Client.java

import java.net.*;
import java.io.*;

public class Client {

    public static void main(String[] args) throws IOException {

        InetAddress addr = null;
        int port = -1;
        long threshold = 0;

        try{
            if(args.length == 3){
                addr = InetAddress.getByName(args[0]);
                port = Integer.parseInt(args[1]);
                threshold = Long.parseUnsignedLong(args[2]); // il metodo lancia un'eccezione se è inserito un num.negativo
            } else{
                System.out.println("Usage: java Client serverIp serverPort threshold>0");
                System.exit(1);
            }
        }

        catch(Exception e){
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
        System.out
                .print("Client Started.\n\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti un nome directory: ");

        try{
            while ( (dirname=stdIn.readLine()) != null){
                // se la directory esiste creo la socket
                if((directory = new File(dirname)).isDirectory()){
                    // creazione socket
                    try{
                        socket = new Socket(addr, port);
                        socket.setSoTimeout(30000);
                        System.out.println("Creata la socket: " + socket);
                    }
                    catch(Exception e){
                        System.out.println("Problemi nella creazione della socket: ");
                        e.printStackTrace();
                        System.out
                                .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                        continue;
                        // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                    }

                    // creazione stream di input/output su socket
                    try{
                        inSock = new DataInputStream(socket.getInputStream());
                        outSock = new DataOutputStream(socket.getOutputStream());
                    }
                    catch(IOException e){
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
                else{
                    System.out.println("Directory non presente.");
                    System.out
                            .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                    // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                    continue;
                }

                String risposta = null;

                /* itero dentro la directory e invio tutti i file */
                // creazione stream di input da file
                try {
                    for (File f : directory.listFiles()){

                        if(f.length() < threshold) continue;

                        inFile = new FileInputStream(f);

                        // trasmissione del nome
                        try{
                            outSock.writeUTF(f);
                            System.out.println("Inviato il nome del file " + f);
                        }
                        catch(Exception e){
                            System.out.println("Problemi nell'invio del nome di " + f
                                    + ": ");
                            e.printStackTrace();
                            System.out
                                    .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure reinserisci una directory: ");
                            // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                            continue;
                        }

                        risposta = inSock.readUTF();

                        if(risposta.equals("salta")){
                            System.out.println("Il file esiste già nel server");
                            // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                            continue;
                        }else if(risposta.equals("attiva")){

                            System.out.println("Invio lunghezza file: " +f.length());
                            outSock.writeInt(f.length());

                            System.out.println("Inizio la trasmissione di " + f);

                            // trasferimento file
                            try{
                                //FileUtility.trasferisci_a_linee_UTF_e_stampa_a_video(new DataInputStream(inFile), outSock);
                                FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(inFile), outSock);
                                inFile.close(); 			// chiusura file
                                System.out.println("Trasmissione di " + f + " terminata ");
                            }
                            catch(SocketTimeoutException ste){
                                System.out.println("Timeout scattato: ");
                                ste.printStackTrace();
                                System.out
                                        .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
                                // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                                continue;
                            }
                            catch(Exception e){
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
                }

                catch(FileNotFoundException e){
                    System.out
                            .println("Problemi nella creazione dello stream di input da "
                                    + dirname + ": ");
                    e.printStackTrace();
                    System.out
                            .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
                    // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                    continue;
                }
                /*try{
                    inFile = new FileInputStream(dirname);
                }*/

                /*
                 * abbiamo gia' verificato che esiste, a meno di inconvenienti, es.
                 * cancellazione concorrente del file da parte di un altro processo, non
                 * dovremmo mai incorrere in questa eccezione.
                 */




                // ricezione esito
                String esito;
                try{
                    esito = inSock.readUTF();
                    System.out.println("Esito trasmissione: " + esito);
                    // chiudo la socket in downstream
                    socket.shutdownInput();
                    System.out.println("Terminata la chiusura della socket: " + socket);
                }
                catch(SocketTimeoutException ste){
                    System.out.println("Timeout scattato: ");
                    ste.printStackTrace();
                    socket.close();
                    System.out
                            .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
                    // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                    continue;
                }
                catch(Exception e){
                    System.out
                            .println("Problemi nella ricezione dell'esito, i seguenti: ");
                    e.printStackTrace();
                    socket.close();
                    System.out
                            .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
                    continue;
                    // il client continua l'esecuzione riprendendo dall'inizio del ciclo
                }

                // tutto ok, pronto per nuova richiesta
                System.out
                        .print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");

            }
            socket.close();
            System.out.println("PutFileClient: termino...");
        }
        // qui catturo le eccezioni non catturate all'interno del while
        // quali per esempio la caduta della connessione con il server
        // in seguito alle quali il client termina l'esecuzione
        catch(Exception e){
            System.err.println("Errore irreversibile, il seguente: ");
            e.printStackTrace();
            System.err.println("Chiudo!");
            System.exit(3);
        }
    } // main
} // PutFileClient
