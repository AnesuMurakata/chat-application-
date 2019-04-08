package com.assignments.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;



/**
 *
 * @author chggtak003 nglbri001 mrkane001
 */
 

public class Server
{

    private ServerSocket serverSocket;
    
    private final ArrayList<ClientListener> clientList = new ArrayList<>();
    private final ConcurrentHashMap<Integer,PrintStream> outputList = new ConcurrentHashMap<>();
    private final ArrayList<String> users= new ArrayList<>();
    private final ArrayList<String> fileNames=new ArrayList<>();

    public static void main(String args[]){
      
        new Server();
         
    }
    
    private Server(){
           System.out.println("Server Online");
           this.init();
           runServer();
      }


    private void init() {
        int PortNumber = 1024; //ports 0-1023 is reserved

        try
        {
            serverSocket = new ServerSocket(PortNumber);
            System.out.println(serverSocket.getInetAddress());

        } catch (IOException ex)
        {
            System.out.println("Something went wrong with initiating the Server");
            ex.printStackTrace();

        }

    }

    private void runServer() {
        Socket socket;
        while (true)
        {
            try
            {
                socket = serverSocket.accept();

                ClientListener newConnection;
                newConnection = new ClientListener(socket);
                clientList.add(newConnection);
                newConnection.start();

            } catch (IOException ex)
            {
                ex.printStackTrace();

            }

        }
    }

    /**
     * 
     * @return  space separated list of online users in string format
     */
    private synchronized String getOnlineUsers(){
        StringBuilder list=new StringBuilder();
        for(String name:users){       
            list.append(name);
            list.append(" ");
        }
        list.deleteCharAt(list.length()-1);//remove unnecessary space at the end
        return list.toString();
    }


    enum CommunicationType { NONE,NEW_USER,UPDATE_USERS,DISCONNECT,FILE, TEST, ACCEPT}


    public class ClientListener extends Thread {

        Socket clientSocket = null;
        BufferedReader input;
        PrintStream output;
        DataOutputStream dataOutputStream;
        
         
        ClientListener(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try{
                dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
            }catch(Exception e){
                System.out.println("Could not get DataOutputStream");
                e.printStackTrace();
            }

        }
        
        DataOutputStream getOutput() {
            return dataOutputStream;
        }
        

        public void run() {
            System.out.println("ClientListener started");
            try
            {
                InputStream inputStream;
                output = new PrintStream(getOutput());
                outputList.put(outputList.size(),output);
                while (true)
                {
                    inputStream = clientSocket.getInputStream();
                    input = new BufferedReader(new InputStreamReader(inputStream));//in to the client
                    if (input.ready())
                    {
                        
                        String message = input.readLine();
                        
                        switch(parseCommunication(message)){
                            case NEW_USER:
                                users.add(message.substring(message.lastIndexOf(" ")+1));
                                sendCommunication("TAG",outputList.size()+"");
                                sendCommunicationToAll("UPDATE_USERS",getOnlineUsers());
                                break;
                            case UPDATE_USERS:
                                sendCommunication("UPDATE_USERS",getOnlineUsers());
                                break;
                            case DISCONNECT:
                                users.remove(message.substring(message.lastIndexOf(" ")+1));
                                break;
                            case FILE:
                                System.out.println("processing");
                                sendCommunication("TEST",message.substring(message.lastIndexOf(" ")+1));
                                String fname=receiveFile(message.substring(message.lastIndexOf(" ")+1));
                                sendCommunicationToAll("RECEIVE",fname);
                                break;
                            case ACCEPT:
                                sendCommunication("SENDING","");
                                routeFile(fileNames.get(fileNames.indexOf(message.substring(message.lastIndexOf(" ")+1))));
                                break;
                            case NONE:
                                routeMessage(message);
                                break;
                        }
                       
                        
                    }
                }

            } catch (Exception e)
            {
                System.out.println("no client to add");
                e.printStackTrace();
            }
        }
        
        private CommunicationType parseCommunication(String message){
            //System.out.println("2. Server Recieved Communication: "+message); //Debug line
            String[] breakdown= message.split(" ");
            if(!breakdown[0].equals("CLIENT_COMMUNICATION"))return CommunicationType.NONE;

            switch(breakdown[1]){
                case "NEW_USER":
                    return CommunicationType.NEW_USER;
                case "UPDATE_USERS":
                    return CommunicationType.UPDATE_USERS;
                case "DISCONNECT":
                    return CommunicationType.DISCONNECT;
                case "FILE":
                    return CommunicationType.FILE;
                case "ACCEPT":
                    return CommunicationType.ACCEPT;
                case "TEST":
                    return CommunicationType.TEST;
                default:
                    return CommunicationType.NONE;
            }

        }

        /**
         * Sends message to the other clients connected to the server
         * @param message message received by the server from the client through this listner
         */
        private void routeMessage(String message){
           for(int i:outputList.keySet()){
                   outputList.get(i).println(message);
              
           }
        }
        
        private String receiveFile(String name){
           

            SimpleDateFormat ft = new SimpleDateFormat ("YMMd_HHmm");

            String fileName= ft.format(new Date());
            try {
                System.out.printf("receiving file: %s%n",fileName);
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                int length = dis.readInt();
                byte[] byteArray = new byte[length];
                for(int i = 0; i<length; i++){
                    byteArray[i] = dis.readByte();
                }


                FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                fileOutputStream.write(byteArray);
                fileOutputStream.close();
                System.out.printf("received file: %s. Saved as:%s%n",name,fileName);
                fileNames.add(fileName);


            } catch (IOException ex) {
                ex.printStackTrace();
            } 
            

             return fileName;

        }


        /**
         * Route File sent to server to chat that accepted
         * @param fileName full path name of the file to be sent
         */
        void routeFile(String fileName){
            File file = new File(fileName);

             try {
                 byte[] content = Files.readAllBytes(file.toPath());
                 dataOutputStream.writeInt(content.length);
                 dataOutputStream.write(content);
             } catch (IOException ex) {
                 System.out.println("Could not send file");
             }
        }
        
        private void sendCommunication(String instruction,String message){
            //System.out.println("3. Server sent Communication: "+"SERVER_COMMUNICATION "+instruction+" "+message);
            output.println("SERVER_COMMUNICATION "+instruction+" "+message);
        }
        
        private void sendCommunicationToAll(String instruction,String message){
             for(int i:outputList.keySet())
            {
                PrintStream stream=outputList.get(i);
                //if(stream==output)continue;
                stream.println("SERVER_COMMUNICATION "+instruction+" "+message);
            } 
        }        
       

    }

}
/*

   

 

 */
