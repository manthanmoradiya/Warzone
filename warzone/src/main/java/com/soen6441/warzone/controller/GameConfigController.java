package com.soen6441.warzone.controller;

import com.soen6441.warzone.config.StageManager;
import static com.soen6441.warzone.config.WarzoneConstants.PHASE_GAME_START_UP;
import static com.soen6441.warzone.config.WarzoneConstants.PHASE_MAP;
import static com.soen6441.warzone.config.WarzoneConstants.GAME_DEF_PATH;
import com.soen6441.warzone.model.CommandResponse;
import com.soen6441.warzone.model.GameData;
import com.soen6441.warzone.model.Player;
import com.soen6441.warzone.model.WarMap;
import com.soen6441.warzone.observerpattern.LogEntryBuffer;
import com.soen6441.warzone.observerpattern.WriteLogFile;
import com.soen6441.warzone.service.GameConfigService;
import com.soen6441.warzone.service.GeneralUtil;
import com.soen6441.warzone.service.MapHandlingInterface;
import com.soen6441.warzone.state.IssueOrderPhase;
import com.soen6441.warzone.state.MapPhase;
import com.soen6441.warzone.state.Phase;
import com.soen6441.warzone.state.StartUpPhase;
import com.soen6441.warzone.view.FxmlView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

/**
 * This Class is made to handle Game Config controller request
 *
 * @author <a href="mailto:patelvicky1995@gmail.com">Vicky Patel</a>
 */
@EqualsAndHashCode
@Controller
public class GameConfigController implements Initializable {

    public static final String LOAD_MAP = "loadmap";
    public static final String SHOW_MAP = "showmap";
    public static final String GAME_PLAYER = "gameplayer";
    public static final String ASSIGN_COUNTRY = "assigncountries";
    private static int AssignCountryFlag = 0;

    @FXML
    private TextField d_CommandLine;

    @FXML
    private TextArea d_showPlayPhase;

    @FXML
    private Button d_StartGame;

    @FXML
    private Button d_FireCommand;

    @Autowired
    private WarMap d_warMap;

    @Autowired
    private GameConfigService d_gameConfigService;

    @Autowired
    private GeneralUtil d_generalUtil;

    @Autowired
    private GameData d_gameData;

    @Lazy
    @Autowired
    private StageManager d_stageManager;

    @Autowired
    private MapHandlingInterface d_maphandlinginterface;

    @Autowired
    private GameEngine d_gameEngine;

    private LogEntryBuffer d_logEntryBuffer = new LogEntryBuffer();
    private WriteLogFile d_writeLogFile = new WriteLogFile(d_logEntryBuffer);

    /**
     * This is the initialization method of this controller
     *
     * @param p_location of the FXML file
     * @param p_resources is properties information
     * @see javafx.fxml.Initializable#initialize(java.net.URL,
     * java.util.ResourceBundle)
     */
    @Override
    public void initialize(URL p_location, ResourceBundle p_resources) {
        d_StartGame.setDisable(true);
        d_showPlayPhase.setStyle("-fx-font-family: monospace");
    }

    /**
     * This method will redirect user to the Home Screen
     *
     * @param p_event represents value send from view
     */
    @FXML
    void backToWelcome(ActionEvent p_event) {
        d_stageManager.switchScene(FxmlView.HOME, null, "");
        d_gameData = new GameData();
    }

    /**
     * This method is used to set the Game Phase
     *
     * @param p_gameEngine represent the phase to be set
     */
    public void setGameEngine(GameEngine p_gameEngine) {
        d_gameEngine = p_gameEngine;

    }

    /**
     * This method will redirect user Game Start Screen
     *
     * @param p_event represent value send from view
     */
    @FXML
    void toStartGame(ActionEvent p_event) {
        StartUpPhase st = (StartUpPhase) d_gameEngine.getPhase();
        st.next(d_gameData);
    }

    /**
     * This method is used to get fire command from user and put it as a
     * parameter in validation
     *
     * @param p_event : events from view
     */
    public void getData(ActionEvent p_event) {
        String l_command = d_CommandLine.getText().trim();
        //write log about command
        d_logEntryBuffer.setLogEntryBuffer("Command:: " + l_command);
        List<String> l_commandSegments = Arrays.asList(l_command.split(" "));
        CommandResponse l_gmConfigRes = new CommandResponse();

        if (l_command.toLowerCase().startsWith(SHOW_MAP)) {                                                     //condition if user gives input to show the map
            if (d_gameData.getD_warMap() != null) {
                l_gmConfigRes = d_gameConfigService.showPlayerMap(d_gameData);
            } else {
                l_gmConfigRes.setD_isValid(false);
                l_gmConfigRes.setD_responseString("Please load the map first");
            }
        } else if (l_command.toLowerCase().startsWith(LOAD_MAP)) {                                               //condition satisfies if user wants to load the map
            if (AssignCountryFlag == 1) {                                          //if countries are assigned already then ,this condition won't allow to load map again
                l_gmConfigRes.setD_isValid(false);
                l_gmConfigRes.setD_responseString("countries are already assigned to each player");
            } else {
                String l_fileName = (l_commandSegments != null && l_commandSegments.size() == 2) ? l_commandSegments.get(1) : null;
                if (l_fileName != null) {
                    if (d_generalUtil.validateIOString(l_fileName, "^[a-zA-Z]+.?[a-zA-Z]+") || d_generalUtil.validateIOString(l_fileName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")) {      //validates the filename given by user
                        l_gmConfigRes = loadMap(l_fileName);
                    } else {
                        d_generalUtil.prepareResponse(false, "Please enter valid file name for loadmap command");
                        l_gmConfigRes = d_generalUtil.getResponse();
                    }
                } else {
                    d_generalUtil.prepareResponse(false, "Please enter valid loadmap command");
                    l_gmConfigRes = d_generalUtil.getResponse();
                }
            }
        } else if (l_command.toLowerCase().startsWith(GAME_PLAYER)) {                                  //if user wants to add or remove players
            if (AssignCountryFlag == 1) {                                                  //if countries are assigned already then ,this condition won't allow to add player again
                l_gmConfigRes.setD_isValid(false);
                l_gmConfigRes.setD_responseString("Countries are already assigned to each player");
            } else {
                if (d_gameData.getD_warMap() != null) {
                    if (d_generalUtil.validateIOString(l_command, "gameplayer((\\s-add\\s[a-z|A-Z|_-]+\\s(human|random|cheater|aggressive|benevolent))|(\\s-remove\\s[a-z|A-Z|_-]+))+")) {                                 //validates the command
                        Map.Entry<GameData, CommandResponse> l_updatedGamePlay = d_gameConfigService.updatePlayer(d_gameData, l_command);

                        if (l_updatedGamePlay.getValue().isD_isValid()) {
                            d_gameData = l_updatedGamePlay.getKey();
                            String l_playerName = "\nPlayers : \n[";
                            if (d_gameData.getD_playerList() != null) {
                                for (Player l_p : d_gameData.getD_playerList()) {           //stores players name and print
                                    l_playerName = l_playerName + " " + l_p.getD_playerName() + ",";
                                }
                                l_playerName = l_playerName + "]";
                            }
                            l_updatedGamePlay.getValue().setD_responseString(l_updatedGamePlay.getValue().getD_responseString() + l_playerName);
                        }
                        d_generalUtil.prepareResponse(l_updatedGamePlay.getValue().isD_isValid(), l_updatedGamePlay.getValue().getD_responseString());
                    } else {                                                                    //if command is not valid
                        d_generalUtil.prepareResponse(false, "Please enter valid Game Player command!@");
                    }
                    l_gmConfigRes = d_generalUtil.getResponse();
                } else {                                                                     //if map of game engine is empty
                    l_gmConfigRes.setD_isValid(false);
                    l_gmConfigRes.setD_responseString("Please load the map first");
                }
            }
        } else if (l_command.toLowerCase().startsWith(ASSIGN_COUNTRY)) {                           //if user wants to assigncountries to players
            if (d_gameData.getD_warMap() == null) {
                l_gmConfigRes.setD_isValid(false);
                l_gmConfigRes.setD_responseString("Please load the map first");
            } else {
                if (l_commandSegments.size() == 1) {                                          //to validate the command
                    l_gmConfigRes = d_gameConfigService.assignCountries(d_gameData);
                    if (l_gmConfigRes.isD_isValid()) {
                        d_StartGame.setDisable(false);
                        AssignCountryFlag = 1;
                    }
                } else {                                                                        //if validation of command fails
                    d_generalUtil.prepareResponse(false, "Please enter validloadmap command");
                    l_gmConfigRes = d_generalUtil.getResponse();
                }
            }

        } else if (d_generalUtil.validateIOString(l_command, "savegame\\s+[a-zA-Z]+.?[a-zA-Z]+") && l_commandSegments.size() == 2) {
            if (d_gameData.getD_warMap() != null) {
                d_gameEngine.saveGame(d_gameData, l_commandSegments.get(1));
                d_generalUtil.prepareResponse(true, "Game saved successfully.");
            } else {
                d_generalUtil.prepareResponse(false, "Nothing to save.");
            }
            l_gmConfigRes = d_generalUtil.getResponse();
        } else if (d_generalUtil.validateIOString(l_command, "loadgame\\s+[a-zA-Z]+.?[a-zA-Z]+") && l_commandSegments.size() == 2) {
            try {
                List<String> l_games = new ArrayList<>();

                // get available files in games directory
                l_games = d_generalUtil.getListOfAllFiles(Paths.get(GAME_DEF_PATH), ".txt");

                // check file extension entered by user
                String l_fullName;
                int index = l_commandSegments.get(1).lastIndexOf('.');
                l_fullName = index > 0
                        ? l_commandSegments.get(1).toLowerCase() : l_commandSegments.get(1).toLowerCase() + ".txt";

                // check if file exists
                if (l_games.contains(l_fullName)) {
                    d_gameData = d_gameEngine.loadGame(l_fullName);

                    // check game data is null or not
                    if (d_gameData != null) {
                        // check player is added or not
                        if (d_gameData.getD_playerList() != null) {
                            // check countries are assigned or not
                            if (d_gameData.getD_playerList().get(0).getD_ownedCountries().size() != 0) {
                                d_StartGame.setDisable(false);
                                AssignCountryFlag = 1;
                                d_generalUtil.prepareResponse(true, "Game loaded successfully.");
                            } else {
                                d_StartGame.setDisable(true);
                                AssignCountryFlag = 0;
                                d_generalUtil.prepareResponse(true, "Game loaded successfully. Please run assigncountries command to play game!!");
                            }
                        } else {
                            d_generalUtil.prepareResponse(false, "Game loaded successfully. Please add players\n and run assigncountries command to play game!!");
                        }
                    } else {
                        d_generalUtil.prepareResponse(false, "File does not contains valid game data.");
                    }
                } else {
                    d_generalUtil.prepareResponse(false, "File does not found.");
                }

                l_gmConfigRes = d_generalUtil.getResponse();
            } catch (Exception e) {
                d_generalUtil.prepareResponse(false, "Error in loadgame command");
                l_gmConfigRes = d_generalUtil.getResponse();
            }
        }
        else {
            d_generalUtil.prepareResponse(false, "Please enter valid command");              //general command if none of the above condition matches
            l_gmConfigRes = d_generalUtil.getResponse();

        }

        d_showPlayPhase.setText(l_gmConfigRes.toString());
        d_logEntryBuffer.setLogEntryBuffer("Response:: " + l_gmConfigRes.getD_responseString());
        d_CommandLine.clear();
    }

    /**
     * This is used as Sub function for Loading map
     *
     * @param p_fileName : File Name to load file
     * @return CommandResponse of the loadMap Command
     */
    public CommandResponse loadMap(String p_fileName) {

        List<String> l_mapFileNameList;
        try {
            l_mapFileNameList = d_generalUtil.getAvailableMapFiles();

            String l_fullName;
            int l_index = p_fileName.lastIndexOf('.');
            l_fullName = l_index > 0
                    ? p_fileName.toLowerCase() : p_fileName.toLowerCase() + ".map";
            if (l_mapFileNameList.contains(l_fullName)) {                                     //check whether file is present or not
                try {
                    d_warMap = d_gameConfigService.loadMap(l_fullName);
                    if (d_maphandlinginterface.validateMap(d_warMap)) {                       //validation of file
                        d_warMap.setD_status(true);                                           // Set status and map file name
                        d_warMap.setD_mapName(l_fullName);
                        d_generalUtil.prepareResponse(true, "Map loaded successfully!");
                        d_gameData.setD_warMap(d_warMap);                                     //set loaded map in the Game play object
                        d_gameData.setD_fileName(p_fileName);
                        d_warMap.setD_mapName(l_fullName);
                    } else {
                        d_generalUtil.prepareResponse(false, "Map is Invalid, Please select another map");
                    }
                } catch (IOException e) {
                    d_generalUtil.prepareResponse(false, "Exception in EditMap, Invalid Map Please correct Map");
                }
            } else {
                d_generalUtil.prepareResponse(false, "Map not found in system");
            }
        } catch (IOException ex) {
            d_generalUtil.prepareResponse(false, "Not able to get the Maps");
        }
        return d_generalUtil.getResponse();

    }

}
