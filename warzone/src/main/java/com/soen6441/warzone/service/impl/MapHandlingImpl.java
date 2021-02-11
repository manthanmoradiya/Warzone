package com.soen6441.warzone.service.impl;

import com.soen6441.warzone.service.MapHandlingInterface;
import com.soen6441.warzone.model.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the implementation class of MapHandlingInterface having business
 * logic of map handling which includes create, edit and validate map etc.
 *
 * @author <a href="mailto:y_vaghan@encs.concordia.ca">Yashkumar Vaghani</a>
 *
 */
public class MapHandlingImpl implements MapHandlingInterface {

    @Autowired
    private WarMap d_warMap;

    private static int continentId = 1;
    private static int countryId = 1;
    private static int neighbourId = 1;

    /**
     * This function is used to check whether string is empty or not
     *
     * @param p_str string passed by user
     * @return true if string is not null
     */
    public boolean isNullOrEmpty(String p_str) {
        if (p_str != null && !p_str.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    /**
     * This method will validate all command entered by user
     *
     * @param p_command contains command string entered by user
     * @return true if command is valid
     */
    public boolean validateCommand(String p_command) {
        boolean l_isValid = false;
        try {
            if (!isNullOrEmpty(p_command)) {
                if (p_command.startsWith("editcontinent")) {
                    checkCommandEditContinent(p_command);
                } else if (p_command.startsWith("editcountry")) {
                    // checkCommandEditCountry(p_command);
                } else if (p_command.startsWith("editneighbor") || p_command.startsWith("editneighbour")) {
                    // checkCommandEditNeighbour(p_command);
                } else if (p_command.startsWith("showmap")) {
                    // show map
                } else if (p_command.startsWith("savemap")) {
                    // save map
                } else if (p_command.startsWith("editmap")) {
                    // edit map
                } else if (p_command.startsWith("validatemap")) {
                    //  

                } else {
                    l_isValid = false;
                }

            } else {
                    // show error message "Please enter valid command"
                    l_isValid = false;

            }
        } catch (Exception e) {
            e.printStackTrace();
            // show error message "Please enter valid command"
                    l_isValid = false;
        }
        return l_isValid;
    }

    /**
     * This method will check edit Continent command, validate and then call
     * perform next operation
     *
     * @param p_editContinentCommand is edit continent command sent from user
     * @return message of result after edit Continent operation
     */
    public String checkCommandEditContinent(String p_editContinentCommand) {
        String l_continentName = "";
        String l_continetValue = "";
        List<String> l_commandString = Arrays.asList(p_editContinentCommand.split(" "));

        for (int l_i = 0; l_i < l_commandString.size(); l_i++) {

            if (l_commandString.get(l_i).equalsIgnoreCase("--add")) {
                l_continentName = l_commandString.get(l_i + 1);
                l_continetValue = l_commandString.get(l_i + 2);
                // match continent name exist or not
                if (validateIOString(l_continentName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$") && validateIOString(l_continetValue, "[1-9][0-9]*")) {
                    boolean l_isValidName = true;
                    for (Map.Entry<Integer, Continent> l_entry : d_warMap.getD_continents().entrySet()) {
                        if (l_entry.getValue() != null && l_continentName.equalsIgnoreCase(l_entry.getValue().getD_continentName())) {
                            // show error message "continent already exists in map file"
                            l_isValidName = false;
                            break;
                        }
                    }
                    if (l_isValidName) {
                        saveCommonContinent(l_continentName, l_continetValue);
                        // show success message "continent saved successfully"
                    }

                } else {
                    // show error message "Please enter valid continent name or value"
                }

            } else if (l_commandString.get(l_i).equalsIgnoreCase("--remove")) {
                l_continentName = l_commandString.get(l_i + 1);
                if (validateIOString(l_continentName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")) {
                    if (deleteCommonContinent(l_continentName)) {
                        // show success message "Continent deleted successfully."
                    } else {
                        // show error message "Continent not found."
                    }
                } else {
                    // show error message "Please enter valid continent name."
                }
            }
        }

//        return l_result.toString();
        return "Continent changes successfully executed.";
    }

    /**
     * This method will return true and break if continent is deleted and this
     * method is common for both terminal and GUI
     *
     * @param p_continentName the name of the continent you want to delete
     * @return true if continent successfully deleted
     */
    public boolean deleteCommonContinent(String p_continentName) {
        boolean l_result = false;
        int l_continentId;

        for (Map.Entry<Integer, Continent> l_entry : d_warMap.getD_continents().entrySet()) {
            if (l_entry.getValue() != null && p_continentName.equalsIgnoreCase(l_entry.getValue().getD_continentName())) {
                l_continentId = l_entry.getKey();
                d_warMap.getD_continents().remove(l_entry.getKey());
                l_result = true;
                break;
            }
        }

        if (l_result) {
          // remove country and neighbours of continent
        }
        return l_result;
    }

    /**
     * This method will save continent for both terminal and GUI
     *
     * @param p_continentName name of continent
     * @param p_value value of Continent
     */
    public void saveCommonContinent(String p_continentName, String p_value) {

        Continent l_continent = new Continent();
        l_continent.setD_continentIndex(continentId);
        l_continent.setD_continentName(p_continentName);
        l_continent.setD_continentValue(Integer.parseInt(p_value));
        Map<Integer, Continent> l_continentMap = new HashMap();
        l_continentMap.put(continentId, l_continent);
        d_warMap.setD_continents(l_continentMap);
        continentId++;

//        loadContinentDetails();
//        loadCountryDetails();
    }

    /**
     * This method will validate the I/O given from GUI or terminal
     *
     * @param p_string string you want to validate
     * @param p_regex regex for validation
     * @return true if string matches with regex
     */
    public boolean validateIOString(String p_string, String p_regex) {
        if (!p_string.isEmpty()) {
            Pattern p = Pattern.compile(p_regex);
            Matcher m = p.matcher(p_string);
            return m.find() && m.group().equals(p_string);
        } else {
            return false;
        }
    }
}
