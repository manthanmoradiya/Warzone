package com.soen6441.warzone.controller;

import com.soen6441.warzone.config.StageManager;

import static com.soen6441.warzone.config.WarzoneConstants.*;

import com.soen6441.warzone.observerpattern.LogEntryBuffer;
import com.soen6441.warzone.observerpattern.WriteLogFile;
import com.soen6441.warzone.service.MapHandlingInterface;
import com.soen6441.warzone.view.FxmlView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * This is for handling welcome controller request
 *
 * @author <a href="mailto:g_dobari@encs.concordia.ca">Gaurang Dobariya</a>
 */
@Controller
public class WelcomeController implements Initializable {

    /**
     * Print TimeStamp
     */
    public boolean d_printTimeStamp = true;
    /**
     * FXML Component
     */
    @FXML
    private Button d_BtnExit;
    /**
     * Stage Manager Object
     */
    @Lazy
    @Autowired
    private StageManager d_stageManager;

    /**
     * Map handling Service
     */
    @Autowired
    private MapHandlingInterface d_mapHandlingInterface;

    /**
     * *
     * Log EntryBuffer(Observer)
     */
    private LogEntryBuffer d_logEntryBuffer = new LogEntryBuffer();
    /**
     * WriteLog File Object(Observable)
     */
    private WriteLogFile d_writeLogFile = new WriteLogFile(d_logEntryBuffer);

    /**
     * This method takes a user to map creation, where player can create mad and
     * edit it
     *
     * @param p_event will represents value sent from view
     */
    @FXML
    void createMap(ActionEvent p_event) {
        d_stageManager.switchScene(FxmlView.GAMEENGINE, null, PHASE_MAP);

    }

    /**
     * This method is used to approach a start up phase of game for a player
     *
     * @param p_event will represents value sent from view
     */
    @FXML
    void playGame(ActionEvent p_event) {
        d_stageManager.switchScene(FxmlView.GAMEENGINE, null, PHASE_GAME_START_UP);
    }

    /**
     * This method will exit the game and close the stage
     *
     * @param p_event will represents value sent from view
     */
    @FXML
    void exitGame(ActionEvent p_event) {
        d_logEntryBuffer.setLogEntryBuffer("Exit Game\n");
        Stage l_stage = (Stage) d_BtnExit.getScene().getWindow();
        l_stage.close();
    }

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
        if (d_printTimeStamp) {
            d_logEntryBuffer.setLogEntryBuffer("\n======================================" + new Date().toString() + "======================================");
            d_printTimeStamp = false;
        }
    }

}
