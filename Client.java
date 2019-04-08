package com.assignments.chat;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author chggtak003 nglbri001 mrkane001
 */


class Client {
     private Socket socket;
     private PrintStream output;
     private final ClientInterface clientInterface;
     private final String ip;
     private String username;
     private String tag;
     private String fileRequest;
     private ArrayList<String> serverFiles;
        
    
    Client(ClientInterface clientUI, String ip, String name) {
        this.ip =ip;
        int PortNumber = 1024; //ports 0-1023 is reserved
        clientInterface=clientUI;
        username=name;

        try
        {
            socket = new Socket(ip, PortNumber);//socket
            ServerListener serverListener = new ServerListener(
                    new BufferedReader(new InputStreamReader(socket.getInputStream())),this);
            serverListener.start();
            output= new PrintStream(socket.getOutputStream());//output stream
            tag=output.toString();

        } catch (IOException ex)
        {
            System.out.println("Something went wrong with creating the CLIENT");
            System.out.println(ex);
        }
        

        this.sendCommunication("NEW_USER",username);       
       
    }


    /**
     *
     * @param tag tag number provided server to uniquely identify client
     */
    void setTag(String tag){
        this.tag =tag;
    }

    /**
     *
     * @param message update the UI with the received message
     */
    void updateChat(String message) {
        System.out.println(message);
        String sentTag=message.substring(0,message.indexOf(" "));
        String senderName=message.substring(message.indexOf(" ")+1,message.indexOf(":")+1);
        message=message.substring(message.indexOf(":")+2);
        if(sentTag.equals(tag))clientInterface.updateChat("ME:"+" "+message);
        else if(!sentTag.equals(tag))clientInterface.updateChat(senderName+" "+message);

    }

    /**
     *
     * @param fileName the name of the file (as saved on the server) to be downloaded
     */
    void showFileRequestDialog(String fileName){
        if(serverFiles==null)serverFiles=new ArrayList();
        serverFiles.add(fileName);
        clientInterface.fileDialog();
    }


    /**
     *
     * @param message message to be sent to the server
     */
    void sendMessage(String message){
        output.println(tag+" "+username+": "+message);
    }
    
    private void sendCommunication(String instruction, String communication){
        output.println("CLIENT_COMMUNICATION "+instruction+" "+communication);
    }


    /**
     *
     * @param stringList a space separated list of users on the server;
     */
    void onlineUsers(String stringList){
        stringList=stringList.substring(stringList.indexOf(" ")+1);
        stringList=stringList.replaceFirst(username,"(Me)");
        clientInterface.updateOnlineUsers(stringList);
    }

    /**
     * detach from the server
     */
    public void disconnect(){
         
          sendCommunication("DISCONNECT",username);
          try {
              socket.close();
          } catch (IOException ex) {
              Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
          }
        
    }


    /**
     * Send file to server
     * @param fileName name of file to be sent
     */
    void transmitFile(String fileName){
        File file = new File(fileName);

        DataOutputStream os;
         try {
             byte[] content = Files.readAllBytes(file.toPath());
             os = new DataOutputStream(output);
             os.writeInt(content.length);
             os.write(content);
             System.out.println("sent "+fileName);
         } catch (IOException ex) {
             ex.printStackTrace();
         }
        
    }


    /**
     * Convenience method for GUI. Starts file sending process by communicating with server
     * @param filePath absolute path of file to be sent
     */
    void sendFile(String filePath){
        sendCommunication("FILE",filePath);

    }

    /**
     * Convenience method for GUI. Starts file receiving process by communicating with server

     */
    void getFileFromServer(){
        sendCommunication("ACCEPT",serverFiles.get(serverFiles.size()-1));
    }

    /**
     * Get last file that was sent to the server;
     */
    void receiveFile(){
        SimpleDateFormat ft = new SimpleDateFormat ("'client_'YYMMdd_HHmm");
        fileRequest=serverFiles.get(serverFiles.size()-1);
        String fileName= ft.format(new Date());
        try {
           
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            int length = dis.readInt();
            byte[] ba = new byte[length];
            for(int i = 0; i<length; i++){
                ba[i] = dis.readByte();
            }

            FileOutputStream fileout = new FileOutputStream(fileName);
            fileout.write(ba);
            fileout.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } 
        clientInterface.successDialog(fileRequest);
        
        }


    /**
     * Objects of this class 'listen' for incoming communication from Server and respond accordingly
     */
    public static class ServerListener extends Thread
    {
         private final BufferedReader input ;
         private final Client client;


        enum ComType{UPDATE_USERS,TAG,RECEIVE,NONE,TEST,SENDING}
        ServerListener(BufferedReader dataInputStream, Client aclient)
        {
           input=dataInputStream;
           client = aclient;
        }



       public void run(){

           while(true){

                try
                {

                    if(input.ready()){

                        String r = ""+input.readLine();

                        switch(parseCommunication(r)){
                            case TAG:
                                client.setTag(r.substring(r.lastIndexOf(" ")+1));
                                break;
                            case UPDATE_USERS:
                                client.onlineUsers(r.substring(r.indexOf(" ")+1));
                                break;
                            case RECEIVE:
                                client.showFileRequestDialog(r.substring(r.lastIndexOf(" ")+1));
                                break;
                            case SENDING:
                                client.receiveFile();
                                break;
                            case TEST:
                                client.transmitFile(r.substring(r.lastIndexOf(" ")+1));
                            case NONE:
                                client.updateChat(r);
                                break;
                        }


                    }

                } catch (IOException ex)
                {
                    System.out.println(ex);
                    Logger.getLogger(ClientInterface.class.getName()).log(Level.SEVERE, null, ex);
                }


           }

        }

        /**
         * Determines the type of communication received from the server
         * @param message the communication
         * @return the <code>ComType</code> of the message
         */
        private ComType parseCommunication(String message){
                String[] breakdown= message.split(" ");
                if(!breakdown[0].equals("SERVER_COMMUNICATION"))return ComType.NONE;
                try{
                    return ComType.valueOf(breakdown[1]);
                }catch (IllegalArgumentException ex){
                    return ComType.NONE;
                }

        }



    }
}
