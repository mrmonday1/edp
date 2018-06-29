/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

/**
 *
 * @author EDIOMA
 */
public class EDP_Project extends Application implements Runnable{
    
    //THIS IS NETWORKING
    
    private final String ip = "localhost";
    private final int port = 22222;
    private final Thread thread;
    
    private Socket socket;
    private ServerSocket serverSocket;
    private DataOutputStream outputTurn;
    private DataInputStream inputTurn;
    private ObjectOutputStream outputBoard;
    private ObjectInputStream inputBoard;
    
    private boolean isServer = false;
    private boolean clientConnected = false;
    
    private boolean lostConnection = false;
    private int numErrors = 0;
    
    //GAME LOGIC HERE
    
    private boolean myTurn = false;
    private final String[] wordList;
    private String[] board;
    
    private ToggleButton selected1;
    private ToggleButton selected2;
    private int myMatches = 0;
    private int oppMatches = 0;
    
    private boolean gameOver = false;
    private boolean gameStateUpdate = false;
    private ToggleButton lastTileSelected;
    
    //GUI SO GUI
    
    private final int numCols;
    private final int numRows;
    private TilePane tilePane;
    private Stage stage;
    private final double TILE_SIZE = 100;
    
    
    public EDP_Project() {
        wordList = new String[] {"Fake","This","Stuff","I'm","In","I","Love","Java"};
        
        numCols = (int)Math.ceil(Math.sqrt(wordList.length*2));
        numRows = numCols;
        
        //CHECK SERVER CONNECTION FIRST, ELSE, BECOME YOUR OWN FUCKING SERVER
        if(!connectToServer()){
            
            initServer();
            
            // 1. CREATE INITIAL BOARD STATE
            HashMap<String, Integer>wordCount = new HashMap<>();   //COLLECTION OF STRING(BASICALLY, ARRAY OF STRINGS)
            for (String s: wordList){
                wordCount.put(s, 0);
            }
            
            board = new String[2*wordList.length];
            for(int i =0; i<board.length; i++){
                String word = wordList[ (int) (Math.random() * wordList.length)];
                while (wordCount.get(word) >=2 ){
                    word = wordList[ (int) (Math.random() * wordList.length)];
                }
                wordCount.put(word, wordCount.get(word)+1);
                board[i] = word;
            }
            
            // 2. WAIT FOR CLIENT TO JOIN
            
            
        }else{
            
            // IF NOT SERVER, FETCH BOARD STATE FROM SERVER
            
            fetchBoardStateFromServer();
            
        }
        
        thread = new Thread(this,"Demo");
        thread.start();
    }
    
    /**************************************************************************
    
    NETWORKING METHODS =__="
    
    ***/
            
    private boolean connectToServer(){
        System.out.println("\nCONNECTING TO SERVER...");
        try{
            socket = new Socket(ip, port); //  <----- CHANGE IP AND PORT HERE
            outputTurn = new DataOutputStream(socket.getOutputStream());
            inputTurn = new DataInputStream(socket.getInputStream());
            
            outputBoard = new ObjectOutputStream(socket.getOutputStream());
            inputBoard = new ObjectInputStream(socket.getInputStream());
            
            clientConnected = true;
            isServer = false;
            System.out.println("\nSUCCESSFULLY CONNECTED TO SERVER");
            
        }catch(IOException e){
            System.out.println("\nUNABLE TO CONNECT TO SERVER AT ADDRESS"+ip+":"+port+"");
            return false;
        }
        return true;
    }
    
    private void initServer(){
         System.out.println("\nINITIALIZING SERVER");
         try{
             serverSocket = new ServerSocket(port,8,InetAddress.getByName(ip));
             
             myTurn = true;
             isServer = true;
         }catch(IOException e){
         }
    }
    
    private void listenForServerRequest(){
        //Socket socket = null;
        try{
            //THIS LINE WILL LOCK PROGRAM UNTIL REQUEST
            socket = serverSocket.accept();
            
            outputTurn = new DataOutputStream(socket.getOutputStream());
            inputTurn = new DataInputStream(socket.getInputStream());
            
            outputBoard = new ObjectOutputStream(socket.getOutputStream());
            inputBoard = new ObjectInputStream(socket.getInputStream());
            
            clientConnected = true;
            isServer = true;
            myTurn = true;
            
            //NOW THAT WE HAVE CLIENT, SEND THEM BOARD STATE
            
            sendClientBoardState();
            
            //ALLOW SERVER TO PLAY
            disableTiles(false);
            
        }catch(IOException e){
        }
    }
    
    private void sendClientBoardState(){
        try{
            outputBoard.writeObject(board);
            outputBoard.flush();
        }catch (IOException e){
            numErrors++;
        }
    }
    
    private void fetchBoardStateFromServer(){
        board = new String[2*wordList.length];
        try {
            String[] fromServer = (String[]) inputBoard.readObject();
            board = fromServer.clone();
        } catch(IOException | ClassNotFoundException e){
        }
    }
    
    /****************NETWORKING ENDS HERE*****************/
    
    
    /**************************************************************************
    
    APPLICATION GUI METHODS 
    
    ***/
    
      
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        
        tilePane = new TilePane();
        tilePane.setPrefColumns(numCols);
        tilePane.setPrefColumns(numRows);
        
        for( int i=0; i<board.length; i++){
            tilePane.getChildren().add(createButton(i));
        }
        
        disableTiles(true);
        
        StackPane root = new StackPane();
        root.getChildren().add(tilePane);
        
        Scene scene = new Scene(root, numCols*TILE_SIZE,numRows*TILE_SIZE);
        
        if(isServer){
            addStringToTitle("WAITING FOR CLIENT TO CONNECT...");
        }else{
            addStringToTitle("CONNECTED TO SERVER");
        }
        
        this.stage.setScene(scene);
        this.stage.show();
    }
    
    private ToggleButton createButton(int index){
        ToggleButton btn = new ToggleButton(board[index]);
        btn.setPrefHeight(TILE_SIZE);
        btn.setPrefWidth(TILE_SIZE);
        
        btn.setOnAction((ActionEvent e) -> {
            ToggleButton source = (ToggleButton)e.getSource();
            lastTileSelected = source;
            gameStateUpdate = true;
        });
        return btn;
    }
    
    private void addStringToTitle(String s){
        String clientServer = "Server";
        if(!isServer){
            clientServer = "Client";
        }
        if(this.stage != null){
            this.stage.setTitle(clientServer+":"+s);
        }
    }
    
    private void highlightMatch(boolean myMatch){
        selected1.getStyleClass().clear();
        selected2.getStyleClass().clear();
        if(myMatch){
            selected1.getStyleClass().add("my-match");
            selected2.getStyleClass().add("my-match");
            myMatches++;
        }else{
            selected1.getStyleClass().add("my-match");
            selected2.getStyleClass().add("my-match");
            myMatches++;
        }
    }
    
    /****************APP GUI ENDS HER
     * @param args****************/
    public static void main(String[] args) {
        launch(args);
        
        EDP_Project demo = new EDP_Project();
    }
    
    /************************************************************************************
     
     * GAME LOGIC METHODS HERE
     * 
     * 
     ***/
    
    @Override
    public void run() {
        while(!gameOver){
            gameTick();
            
            if(isServer && !clientConnected){
                listenForServerRequest();
            }
        }
    }
    
    private void gameTick(){
        //CHECK IF WE STILL HAVE CONNECTION
        if(numErrors > 10){
            lostConnection = true;
            addStringToTitle("CONNECTION LOST");
            
         // try to reconnect...idk how
        }
        
        //MAKE SURE TURN LABELS ARE UPDATED 
        if(myTurn){
            addStringToTitle("MY  TURN ");
        }else{
            addStringToTitle("OPPONENT  TURN ");
        }
        
        // CHECK IF U OR OPPONENT HAS SELECTED TILES--> UPDATE BOARD STAET
        updateBoardStateOnSelection();
    }
    
    private void updateBoardStateOnSelection(){
            ToggleButton selectedTile = null;
            
            //IF MY TURN
            if(myTurn && gameStateUpdate){
                gameStateUpdate = false;
                
                selectedTile = lastTileSelected;
                
                // lock button toggle cuz its selected
                selectedTile.setDisable(true);
                
                //send board state to opponent
                try{
                    outputTurn.writeInt(getIndexOfTile(selectedTile));
                    outputTurn.flush();
                }catch(IOException e){
                    numErrors++;
                }
            }
            
            
            //IF OPPONENT TURN
            if(!myTurn){
                try{
                    int tileIndex = inputTurn.readInt();
                    selectedTile = (ToggleButton) tilePane.getChildren().get(tileIndex);
                    
                    //simulate opponent tile press
                    selectedTile.setSelected(true);
                }catch(IOException e){
                    numErrors++;
                }
            }
            
            // assign selected tile to compare match
            if(selected1 == null){
                selected1 = selectedTile;
            }else{
                selected2 =  selectedTile;
            }
            
            //chack for match
            if(selected1 != null && selected2 != null){
                checkForMatch();
            }
        }
    private int getIndexOfTile(ToggleButton b){
        int index = 0;
        for(Node n : tilePane.getChildren()){
            if(n.equals(b)){
                return index;
            }
            index++;
        }
        return -1;
    }
    
    private void checkForMatch(){
        disableTiles(true);
        
        if(selected1.getText().equals(selected2.getText())){
            //if its out turn, eneable tiles for 2nd selection
            if(myTurn){
                disableTiles(false);
            }
            highlightMatch(myTurn);
            
            checkGameOver();
        }else{
            try{
                Thread.sleep(1);
            }catch(InterruptedException e){
            }
            
            //hide tiles
            selected1.setSelected(false);
            selected1.setSelected(true);
            
            //swap player
            myTurn = !myTurn;
            disableTiles(!myTurn);
            if(myTurn){
                addStringToTitle("My Turn");
            }else{
                addStringToTitle("Opponent's Turn");
            }
        }
        
        //reset selected tags for next turn
        selected1 = null;
        selected2 = null;
    }
    
    private void checkGameOver(){
        if((myMatches+oppMatches) != wordList.length){
            gameOver = false;
        }else{
            gameOver = true;
            
            if(myMatches>oppMatches){
                addStringToTitle("VICTORY!");
            }else if (myMatches == oppMatches){
                addStringToTitle("DRAW!");
            }else{
                addStringToTitle("DEFEAT!");
            }
        }
    }
    
    private void disableTiles(boolean disabledOrNot){
        tilePane.getChildren().stream().filter((child) -> (child instanceof ToggleButton)).map((child) -> (ToggleButton)child).filter((tile) -> (tile.isSelected()==false)).forEachOrdered((tile) -> {
            tile.setDisable(disabledOrNot);
        });
    }
    
    /****************GAME LOGIC ENDS HERE*****************/
}
