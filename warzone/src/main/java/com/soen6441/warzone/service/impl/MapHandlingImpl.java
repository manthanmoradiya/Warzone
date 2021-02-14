package com.soen6441.warzone.service.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import com.soen6441.warzone.service.MapHandlingInterface;
import com.soen6441.warzone.model.*;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This is the implementation class of MapHandlingInterface having business
 * logic of map handling which includes create, edit and validate map etc.
 *
 * @author <a href="mailto:y_vaghan@encs.concordia.ca">Yashkumar Vaghani</a>
 *
 */
@Service
public class MapHandlingImpl implements MapHandlingInterface {

    @Autowired
    private WarMap d_warMap;

    @Autowired
    private CommandResponse d_commandResponse;

    private static int ContinentId = 1;
    private static int CountryId = 1;
    private static int NeighbourId = 1;

    public static final String MAP_DEF_PATH = "src/main/resources/maps/";

    public static final String NAME = "name";
    public static final String FILES = "[files]";
    public static final String CONTINENTS = "[continents]";
    public static final String COUNTRIES = "[countries]";
    public static final String BORDERS = "[borders]";

    @Override
    public boolean isNullOrEmpty(String p_str) {
        if (p_str != null && !p_str.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public CommandResponse validateCommand(String p_command) {
        boolean l_isValid = false;
        try {
            if (!isNullOrEmpty(p_command)) {
                if (p_command.startsWith("editcontinent")) {
                    return checkCommandEditContinent(p_command);
                } else if (p_command.startsWith("editcountry")) {
                    return checkCommandEditCountry(p_command);
                } else if (p_command.startsWith("editneighbor") || p_command.startsWith("editneighbour")) {
                    return checkCommandEditNeighbours(p_command);
                } else if (p_command.startsWith("showmap")) {
                    return showMap();
                } else if (p_command.startsWith("savemap")) {
                    return checkCommandSaveMap(Arrays.asList(p_command.split(" ")).get(1));
                } else if (p_command.startsWith("editmap")) {
                    return checkCommandEditMap(p_command);
                } else if (p_command.startsWith("validatemap")) {
                    if (validateMap(d_warMap)) {
                        prepareResponse(true, "Map is valid");
                    } else {
                        prepareResponse(false, "Map is invalid");
                    }
                } else {
                    prepareResponse(false, "Please enter valid command");
                    l_isValid = false;
                }

            } else {
                prepareResponse(false, "Please enter valid command");
                l_isValid = false;

            }
        } catch (Exception e) {
            e.printStackTrace();
            prepareResponse(false, "Please enter valid command");
            l_isValid = false;
        }

        return d_commandResponse;
    }

    /**
     * This method will prepare response of command entered by user
     *
     * @param p_isValid to check command is successfully executed or not
     * @param p_responeMessage response message of command
     */
    public void prepareResponse(boolean p_isValid, String p_responeMessage) {
        this.d_commandResponse.setD_isValid(p_isValid);
        this.d_commandResponse.setD_responseString(p_responeMessage);
    }

    /**
     * This method will check edit Continent command, validate and then call
     * perform next operation
     *
     * @param p_editContinentCommand is edit continent command sent from user
     * @return message of result after edit Continent operation
     */
    public CommandResponse checkCommandEditContinent(String p_editContinentCommand) {
        String l_continentName = "";
        String l_continetValue = "";
        List<String> l_commandString = Arrays.asList(p_editContinentCommand.split(" "));

        for (int l_i = 0; l_i < l_commandString.size(); l_i++) {

            if (l_commandString.get(l_i).equalsIgnoreCase("-add")) {
                l_continentName = l_commandString.get(l_i + 1);
                l_continetValue = l_commandString.get(l_i + 2);
                // match continent name exist or not
                if (validateIOString(l_continentName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$") && validateIOString(l_continetValue, "[1-9][0-9]*")) {
                    boolean l_isValidName = true;

                    if (d_warMap.getD_continents() != null) {
                        for (Map.Entry<Integer, Continent> l_entry : d_warMap.getD_continents().entrySet()) {
                            if (l_entry.getValue() != null && l_continentName.equalsIgnoreCase(l_entry.getValue().getD_continentName())) {
                                prepareResponse(false, "Continent already exists in map file");
                                l_isValidName = false;
                                break;
                            }
                        }
                    }

                    if (l_isValidName) {
                        saveContinent(l_continentName, l_continetValue);
                        d_commandResponse.setD_isValid(true);
                        d_commandResponse.setD_responseString("Continent saved successfully");
                        return d_commandResponse;
                    }

                } else {
                    prepareResponse(false, "Please enter valid continent name or value");
                }

            } else if (l_commandString.get(l_i).equalsIgnoreCase("-remove")) {
                l_continentName = l_commandString.get(l_i + 1);
                if (validateIOString(l_continentName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")) {
                    if (deleteContinent(l_continentName)) {
                        prepareResponse(true, "Continent deleted successfully");
                    } else {
                        prepareResponse(true, "Continent not found");
                    }
                } else {
                    prepareResponse(true, "Please enter valid continent name");
                }
            }
        }

        return d_commandResponse;
    }

    /**
     * This method will return true and break if continent is deleted and this
     * method is common for both terminal and GUI
     *
     * @param p_continentName the name of the continent you want to delete
     * @return true if continent successfully deleted
     */
    public boolean deleteContinent(String p_continentName) {
        boolean l_result = false;
        int l_continentId;
        if (d_warMap.getD_continents() != null) {
            for (Map.Entry<Integer, Continent> l_entry : d_warMap.getD_continents().entrySet()) {
                if (l_entry.getValue() != null && p_continentName.equalsIgnoreCase(l_entry.getValue().getD_continentName())) {
                    l_continentId = l_entry.getKey();
                    d_warMap.getD_continents().remove(l_entry.getKey());
                    l_result = true;
                    break;
                }
            }
        }
        return l_result;
    }

    /**
     * This method will return true and break if country got removed and this
     * method is used for removal of country
     *
     * @param p_countryName Name of the country you want to delete for
     * @return false if not possible to delete
     */
    public CommandResponse deleteCountry(String p_countryName) {
        boolean l_result = false;
        CommandResponse l_dCountryResponse = new CommandResponse();
        //Check whether continent exist or not
        if (d_warMap.getD_continents() != null) {
            for (Map.Entry<Integer, Continent> l_continent : d_warMap.getD_continents().entrySet()) {
                List<Country> l_countryList = l_continent.getValue().getD_countryList();
                if (l_countryList != null) {
                    List<Country> l_removedCountry = l_countryList.stream().filter(countrty -> p_countryName.equalsIgnoreCase(countrty.getD_countryName()))
                            .collect(Collectors.toList());
                    //to remove and set updated countries to map 
                    if (!l_removedCountry.isEmpty()) {
                        l_countryList.removeAll(l_removedCountry);
                        l_continent.getValue().setD_countryList(l_countryList);
                        l_result = true;
                        l_dCountryResponse.setD_responseString("Country Deleted Sucessfully");
                    }
                }
            }
            l_dCountryResponse.setD_isValid(true);
            if (!l_result) {
                l_dCountryResponse.setD_responseString("Country Does Not Exist!!");
            }
        } else {
            l_dCountryResponse.setD_isValid(true);
            l_dCountryResponse.setD_responseString("No Continent Exist");
        }
        return l_dCountryResponse;
    }

    /**
     * This method will return true and break if neighbor got removed and this
     * method is used for removal of country's neighbor
     *
     * @param p_countryName name of the country you want to delete for
     * @param p_neighborCountryName name of the neighbor you want to delete
     *
     * @return false if not possible to delete or does not exist
     */
    public CommandResponse deleteNeighbour(String p_countryName, String p_neighborCountryName) {
        boolean l_result = false;
        CommandResponse l_dCountryResponse = new CommandResponse();

        //Check whether continent exist or not
        if (d_warMap.getD_continents() != null) {
            for (Map.Entry<Integer, Continent> l_continent : d_warMap.getD_continents().entrySet()) {
                if (l_continent.getValue().getD_countryList() != null) {
                    for (Country l_country : l_continent.getValue().getD_countryList()) {
                        if (p_countryName.equalsIgnoreCase(l_country.getD_countryName())) {
                            //get neighour that matches neighbour given by user
                            List<String> l_neighborToRemove = l_country.getD_neighbourCountries().stream().filter(l_neighborName -> (l_neighborName.equalsIgnoreCase(p_neighborCountryName))).collect(Collectors.toList());
                            //if neighbour found then remove for list of neighbour
                            if (!l_neighborToRemove.isEmpty()) {
                                l_country.getD_neighbourCountries().removeAll(l_neighborToRemove);
                                l_result = true;
                                l_dCountryResponse.setD_responseString("NeighbourCountry Deleted Sucessfully");
                            }
                        }
                    }
                }
            }
            l_dCountryResponse.setD_isValid(true);
            if (!l_result) {
                l_dCountryResponse.setD_responseString("NeighbourCountry Does Not Exist!!");
            }
        } else {
            l_dCountryResponse.setD_isValid(true);
            l_dCountryResponse.setD_responseString("No Continent Exist");
        }
        return l_dCountryResponse;
    }

    /**
     * This method will save continent for both terminal and GUI
     *
     * @param p_continentName name of continent
     * @param p_value value of Continent
     */
    public void saveContinent(String p_continentName, String p_value) {

        Continent l_continent = new Continent();
        l_continent.setD_continentIndex(ContinentId);
        l_continent.setD_continentName(p_continentName);
        l_continent.setD_continentValue(Integer.parseInt(p_value));
        if (d_warMap.getD_continents() == null) {
            Map<Integer, Continent> l_continentMap = new HashMap();
            l_continentMap.put(ContinentId, l_continent);
            d_warMap.setD_continents(l_continentMap);
        } else {
            d_warMap.getD_continents().put(ContinentId, l_continent);
        }
        ContinentId++;
    }

    /**
     * This method will check edit Continent command, validate and then call
     * perform next operation
     *
     * @param p_editCountryCommand is edit command sent from user
     * @return message of result after edit Country command execution
     */
    public CommandResponse checkCommandEditCountry(String p_editCountryCommand) {
        String l_countryName = "";
        String l_continentName = "";
        List<String> l_editCountryCommandString = Arrays.asList(p_editCountryCommand.split(" "));

        for (int i = 0; i < l_editCountryCommandString.size(); i++) {
            if (l_editCountryCommandString.get(i).equalsIgnoreCase("-add")) {
                l_countryName = l_editCountryCommandString.get(i + 1);
                l_continentName = l_editCountryCommandString.get(i + 2);
                if (validateIOString(l_countryName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")
                        && validateIOString(l_continentName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")) {

                    // prepare country list of continent entered by user
                    ArrayList<Country> l_countryList = new ArrayList<Country>();
                    int l_continentIndex = 1;
                    boolean isValidContinent = false;
                    if (d_warMap.getD_continents() != null) {
                        for (Map.Entry<Integer, Continent> l_entry : d_warMap.getD_continents().entrySet()) {
                            // check if continent exists or not
                            if (l_entry.getValue() != null) {
                                l_continentIndex = l_entry.getKey();
                                if (l_entry.getValue().getD_countryList() != null) {
                                    l_entry.getValue().getD_countryList().forEach((country) -> {
                                        l_countryList.add(country);
                                    });
                                }
                                isValidContinent = true;
                                break;
                            }
                        }
                    } else {
                        prepareResponse(false, "Continent not found");
                        break;
                    }

                    if (isValidContinent) {
                        if (!l_countryList.isEmpty()) {
                            // if continent already have countries
                            for (Country country : l_countryList) {
                                if (!l_countryName.equalsIgnoreCase(country.getD_countryName())) {
                                    saveCountry(l_countryName, l_continentIndex);
                                    prepareResponse(true, "Country saved successfully");
                                } else {
                                    prepareResponse(false, "Country already exists");
                                    break;
                                }
                            }
                        } else {
                            // if continent doesn't have any country then add it
                            saveCountry(l_countryName, l_continentIndex);
                            prepareResponse(true, "Country saved successfully");
                        }
                    } else {
                        prepareResponse(false, "Continent is not valid");
                        break;
                    }

                } else {
                    prepareResponse(false, "Please enter valid country name or continent name");
                }

            } else if (l_editCountryCommandString.get(i).equalsIgnoreCase("-remove")) {
                l_countryName = l_editCountryCommandString.get(i + 1);

                if (validateIOString(l_countryName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")) {
                    return deleteCountry(l_countryName);

                } else {
                    prepareResponse(false, "Please enter valid country Name");
                }
            }

        }

        return d_commandResponse;
    }

    /**
     * This method will save country given from both GUI and command line
     *
     * @param p_countryName name of the country
     * @param p_continentIndex index of continent
     */
    public void saveCountry(String p_countryName, int p_continentIndex) {
        Country l_country = new Country();
        l_country.setD_continentIndex(p_continentIndex);
        l_country.setD_countryName(p_countryName);
        l_country.setD_countryIndex(CountryId);
        Continent l_continent = d_warMap.getD_continents().get(p_continentIndex);
        if (l_continent.getD_countryList() != null) {
            l_continent.getD_countryList().add(l_country);
        } else {
            List<Country> l_countryList = new ArrayList();
            l_countryList.add(l_country);
            l_continent.setD_countryList(l_countryList);
        }
        CountryId++;
    }

    /**
     * This method is used to validate the neighbour command and calls add or
     * remove as per the user command
     *
     * @param p_neighbour
     * @return
     */
    public CommandResponse checkCommandEditNeighbours(String p_neighbour) {
        String l_countryName = "";
        String l_neighbourCountryName = "";
        boolean l_result = false;
        List<String> l_commandString = Arrays.asList(p_neighbour.split(" "));
        if (l_commandString.size() == 1 || (l_commandString.size() % 3) != 1) {
            prepareResponse(false, "Invalid Coommand");
            return d_commandResponse;
        }
        for (int l_i = 0; l_i < (l_commandString.size() - 2); l_i++) {
            l_countryName = l_commandString.get(l_i + 1);
            l_neighbourCountryName = l_commandString.get(l_i + 2);
            if (validateIOString(l_countryName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$") && validateIOString(l_neighbourCountryName, "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")) {
                if (l_commandString.get(l_i).equalsIgnoreCase("-add")) {
                    if (d_warMap.getD_continents() != null) {
                        int l_countryId = getCountryIndexByCountryName(d_warMap.getD_continents(), l_countryName);
                        int l_neighbourCountryId = getCountryIndexByCountryName(d_warMap.getD_continents(), l_neighbourCountryName);
                        l_result = saveNeighbour(l_countryId, l_neighbourCountryId);
                    }
                    if (l_result) {
                        prepareResponse(true, "Neighbour is added successfully");
                    } else {
                        prepareResponse(false, "neighbour is not added successfully");
                    }
                } else if (l_commandString.get(l_i).equalsIgnoreCase("-remove")) {
                }
            } else {
                prepareResponse(false, "Invalid Command!!");
                return d_commandResponse;
            }
        }

        return d_commandResponse;
    }

    /**
     * This method is used to add the neighbour
     *
     * @param p_countryId
     * @param p_neighbour
     * @return
     */
    public boolean saveNeighbour(int p_countryId, int p_neighbour) {
        if (p_countryId == p_neighbour) {
            return false;
        }

        boolean l_result = false;
        if (d_warMap.getD_continents() != null) {
            for (Map.Entry<Integer, Continent> l_entry : d_warMap.getD_continents().entrySet()) {
                for (Country l_country : l_entry.getValue().getD_countryList()) {
                    if (p_countryId == l_country.getD_countryIndex()) {
                        String l_neighbourNameToAdd = getCountryNamebyCountryId(d_warMap.getD_continents(), p_neighbour);
                        if (l_country.getD_neighbourCountries() == null) {
                            List<String> addToNeighbourList = new ArrayList<String>();
                            l_country.setD_neighbourCountries(addToNeighbourList);
                            l_country.getD_neighbourCountries().add(l_neighbourNameToAdd);
                            l_result = true;
                            break;
                        } else {
                            if (!l_country.getD_neighbourCountries().contains(l_neighbourNameToAdd)) {
                                l_country.getD_neighbourCountries().add(l_neighbourNameToAdd);
                                l_result = true;
                                break;

                            } else {
                                l_result = false;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return l_result;
    }

    /**
     * This method is used for getting country index by country name
     *
     * @param p_continentMap
     * @param p_countryName
     * @return CountryIndex
     */
    private int getCountryIndexByCountryName(Map<Integer, Continent> p_continentMap, String p_countryName) {
        for (Map.Entry<Integer, Continent> entry : p_continentMap.entrySet()) {
            Continent continent = entry.getValue();

            List<Country> l_countryList = continent.getD_countryList();

            for (Country country : l_countryList) {

                if (country != null) {

                    if (p_countryName.equalsIgnoreCase(country.getD_countryName())) {
                        return country.getD_countryIndex();
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public boolean validateIOString(String p_string, String p_regex) {
        if (!p_string.isEmpty()) {
            Pattern l_pattern = Pattern.compile(p_regex);
            Matcher l_matcher = l_pattern.matcher(p_string);
            return l_matcher.find();
        } else {
            return false;
        }
    }

    @Override
    public CommandResponse showMap() {
        if (d_warMap == null) {
            d_commandResponse.setD_isValid(false);
            d_commandResponse.setD_responseString("Map is Null");
            return d_commandResponse;
        }
        String l_showMapIn2D = "";
        List<Country> l_countries = getAvailableCountries(d_warMap);
        int l_countrySize = l_countries.size();
        int l_i, l_j;
        l_countrySize++;
        Pair<Integer, String[][]> pair = prepareMetricesOfMap(l_countries);
        int l_maxLength = pair.getKey();
        String[][] l_mapMetrices = pair.getValue();
        for (l_i = 0; l_i < l_countrySize; l_i++) {
            for (l_j = 0; l_j < l_countrySize; l_j++) {
                String l_stringFrmat = String.format("%1$" + l_maxLength + "s", l_mapMetrices[l_i][l_j]);
                l_showMapIn2D = l_showMapIn2D + l_stringFrmat + "\t";
            }
            l_showMapIn2D = l_showMapIn2D + "\n\t\t";
        }
        prepareResponse(true, l_showMapIn2D);

        return d_commandResponse;
    }

    /**
     * This method is used to convert map object to metric
     *
     * @param l_countries list of countries
     * @return return no of countries and metric
     */
    public Pair<Integer, String[][]> prepareMetricesOfMap(List<Country> l_countries) {
        int l_maxLength = 0;
        int l_countrySize = l_countries.size();
        int l_i, l_j;
        l_countrySize++;
        String[][] l_mapMetrices = new String[l_countrySize][l_countrySize];
        for (l_i = 0; l_i < l_countrySize; l_i++) {
            for (l_j = 0; l_j < l_countrySize; l_j++) {
                if (l_i == 0 && l_j == 0) {
                    l_mapMetrices[l_i][l_j] = " ";
                    continue;
                } else if (l_i == 0 && l_j != 0) {
                    l_mapMetrices[l_i][l_j] = l_countries.get(l_j - 1).getD_countryName();
                    if (l_maxLength < l_mapMetrices[l_i][l_j].length()) {
                        l_maxLength = l_mapMetrices[l_i][l_j].length();
                    }
                } else if (l_j == 0 && l_i != 0) {
                    l_mapMetrices[l_i][l_j] = l_countries.get(l_i - 1).getD_countryName();
                } else {
                    if (l_countries.get(l_i - 1).getD_neighbourCountries() != null) {
                        if (l_countries.get(l_i - 1).getD_neighbourCountries().contains(l_mapMetrices[0][l_j])) {
                            l_mapMetrices[l_i][l_j] = "1";
                        } else {
                            l_mapMetrices[l_i][l_j] = "0";
                        }
                    } else {
                        l_mapMetrices[l_i][l_j] = "0";
                    }
                }
            }
        }

        return new Pair<Integer, String[][]>(l_maxLength, l_mapMetrices);
    }

    /**
     * used to get all countries available in the map
     *
     * @param p_continentMap
     * @return arraylist of the country
     */
    public ArrayList<Country> getAvailableCountries(WarMap p_continentMap) {

        List<Country> l_countries = new ArrayList<Country>();
        l_countries.clear();
        for (Map.Entry<Integer, Continent> l_entry : p_continentMap.getD_continents().entrySet()) {
            for (Country l_country : l_entry.getValue().getD_countryList()) {
                l_countries.add(l_country);
            }
        }
        return (ArrayList<Country>) l_countries;
    }

    @Override
    public WarMap readMap(String p_fileName) {

        String l_fileLine = "";
        boolean l_isFiles = false;
        boolean l_isContinents = false;
        boolean l_isCountries = false;
        boolean l_isBorders = false;
        WarMap l_warMap = new WarMap();

        try (BufferedReader l_bufferedreader = new BufferedReader(new FileReader(MAP_DEF_PATH + p_fileName))) {

            Map<Integer, Continent> l_continentMap = new HashMap();
            Continent l_continent = null;
            Country l_country = null;
            int l_continentCounter = 1;

            //while loop read each line from file and process accordingly
            while ((l_fileLine = l_bufferedreader.readLine()) != null) {
                if (l_fileLine != null && !l_fileLine.isEmpty()) {

                    if (l_fileLine.startsWith(";")) {
                        continue;
                    }

                    l_warMap.setD_mapName(p_fileName);
                    l_warMap.setD_status(true);
                    if (l_fileLine.equalsIgnoreCase(FILES)) {
                        l_isFiles = true;
                        continue;
                    }
                    if (l_isFiles) {
                        //files of map
                    }
                    if (l_fileLine.equalsIgnoreCase(CONTINENTS)) {
                        l_isFiles = false;
                        l_isContinents = true;
                        continue;
                    }
                    //this if condition read all the contients from file and set into continent model
                    if (l_isContinents && !l_fileLine.equalsIgnoreCase(COUNTRIES)) {
                        l_continent = new Continent();
                        String[] l_continentArray = l_fileLine.split(" ");
                        l_continent.setD_continentName(l_continentArray[0]);
                        l_continent.setD_continentValue(Integer.parseInt(l_continentArray[1]));
                        l_continent.setD_continentIndex(l_continentCounter);
                        l_continentMap.put(l_continentCounter, l_continent);
                        l_continentCounter++;
                    }
                    if (l_fileLine.equalsIgnoreCase(COUNTRIES)) {
                        l_isContinents = false;
                        l_isCountries = true;
                        continue;
                    }
                    //this if condtion read all the countries from file and set into country model
                    if (l_isCountries && !l_fileLine.equalsIgnoreCase(BORDERS)) {

                        String[] l_countries = l_fileLine.split(" ");

                        int l_continentIndex = Integer.parseInt(l_countries[2]);
                        Continent l_currentcontinent = l_continentMap.get(l_continentIndex);

                        l_country = new Country();
                        l_country.setD_countryName(l_countries[1]);
                        l_country.setD_countryIndex(Integer.parseInt(l_countries[0]));
                        l_country.setD_continentIndex(l_continentIndex);
                        if (l_currentcontinent.getD_countryList() == null) {
                            List<Country> l_countryList = new ArrayList();
                            l_countryList.add(l_country);
                            l_currentcontinent.setD_countryList(l_countryList);
                        } else {
                            l_currentcontinent.getD_countryList().add(l_country);
                        }
                        l_continentMap.put(l_continentIndex, l_currentcontinent);
                    }
                    if (l_fileLine.equalsIgnoreCase(BORDERS)) {
                        l_isCountries = false;
                        l_isBorders = true;
                        continue;
                    }
                    //this if condition read neighbors of each country and set into neighborlist of coutey model
                    if (l_isBorders) {

                        String[] l_neighbourArray = l_fileLine.split(" ");

                        Continent l_currentContinent = getContinentByCountryId(l_continentMap, Integer.parseInt(l_neighbourArray[0]));

                        List<String> l_neighbourName = new ArrayList<String>();
                        for (int i = 1; i < l_neighbourArray.length; i++) {
                            l_neighbourName
                                    .add(getCountryNamebyCountryId(l_continentMap, Integer.parseInt(l_neighbourArray[i])));
                        }

                        for (int i = 0; i < l_currentContinent.getD_countryList().size(); i++) {
                            Country currentcountry = l_currentContinent.getD_countryList().get(i);
                            if (currentcountry.getD_countryIndex() == Integer.parseInt(l_neighbourArray[0])) {
                                currentcountry.setD_neighbourCountries(l_neighbourName);
                                l_currentContinent.getD_countryList().set(i, currentcountry);
                            }
                        }
                        l_continentMap.put(l_currentContinent.getD_continentIndex(), l_currentContinent);
                    }
                }
            }
            l_warMap.setD_continents(l_continentMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return l_warMap;
    }

    /**
     * This method will return Continent model from given countryId
     *
     * @param p_continentMap is map of continents
     * @param p_countryIndex is countryId for that you want to find continent
     * @return Continent model
     */
    private Continent getContinentByCountryId(Map<Integer, Continent> p_continentMap, int p_countryIndex) {
        Continent continent = null;
        for (Map.Entry<Integer, Continent> entry : p_continentMap.entrySet()) {

            continent = entry.getValue();

            List<Country> l_countryList = continent.getD_countryList();
            for (Country country : l_countryList) {

                if (country != null) {

                    if (country.getD_countryIndex() == p_countryIndex) {

                        return continent;
                    }
                }
            }
        }

        return continent;
    }

    /**
     * This method will return neighbor name by given Index
     *
     * @param p_continentMap is a map of continents
     * @param p_countryIndex is neighbor index
     * @return neighbor name
     */
    private String getCountryNamebyCountryId(Map<Integer, Continent> p_continentMap, int p_countryIndex) {

        String neighbourName = "";

        for (Map.Entry<Integer, Continent> entry : p_continentMap.entrySet()) {

            Continent continent = entry.getValue();

            List<Country> l_countryList = continent.getD_countryList();
            for (Country country : l_countryList) {

                if (country != null) {

                    if (p_countryIndex == country.getD_countryIndex()) {
                        neighbourName = country.getD_countryName();
                        break;
                    }
                }
            }
        }

        return neighbourName;
    }

    @Override
    public boolean writeMapToFile(WarMap p_warMap) {
        boolean status;
        try {
            StringBuilder l_continentStringBuilder = new StringBuilder(CONTINENTS).append(System.lineSeparator());
            StringBuilder l_countryStringBuilder = new StringBuilder(COUNTRIES).append(System.lineSeparator());
            StringBuilder l_neighborStringBuilder = new StringBuilder(BORDERS).append(System.lineSeparator());

            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(MAP_DEF_PATH + p_warMap.getD_mapName() + ".map"), "utf-8")));) {

                Map<Integer, Continent> l_continentMap = p_warMap.getD_continents();

                for (Map.Entry<Integer, Continent> l_entry : l_continentMap.entrySet()) {
                    Continent l_currentContinent = l_entry.getValue();

                    //here all continets will store into the l_continentStringBuilder
                    l_continentStringBuilder.append(l_currentContinent.getD_continentName() + " " + l_currentContinent.getD_continentValue()).append(System.lineSeparator());
                    List<Country> l_countryList = l_currentContinent.getD_countryList();
                    for (Country l_country : l_countryList) {

                        //here all countries will store into the l_countryStringBuilder
                        l_countryStringBuilder.append(l_country.getD_countryIndex() + " " + l_country.getD_countryName() + " " + l_country.getD_continentIndex() + "0 0")
                                .append(System.lineSeparator());

                        List<String> l_neighborList = l_country.getD_neighbourCountries();
                        if (!l_neighborList.isEmpty() && l_neighborList != null) {
                            l_neighborStringBuilder.append(l_country.getD_countryIndex());
                            for (String l_neighborName : l_neighborList) {

                                //here all neighbors will store into the l_neighborStringBuilder
                                l_neighborStringBuilder.append(" " + getCountryIndexByCountrName(p_warMap, l_neighborName));
                            }
                            l_neighborStringBuilder.append(System.lineSeparator());
                        }
                    }
                }
                //writer.println("extra lines of map file");
                writer.println("name " + p_warMap.getD_mapName());
                writer.println();
                writer.println(FILES);
                writer.println("pic risk_pic.png");
                //writer.println("file names");
                writer.println();
                writer.println(l_continentStringBuilder.toString());
                writer.println(l_countryStringBuilder.toString());
                writer.println(l_neighborStringBuilder.toString());
                status = true;
            }
        } catch (Exception e) {
            status = false;
        }
        return status;
    }

    /**
     * This method will return country index from country name
     *
     * @param p_warMap is object of WarMap model
     * @param p_countryName is the name of country
     * @return index of country
     */
    private int getCountryIndexByCountrName(WarMap p_warMap, String p_countryName) {
        int l_countryIndex = 0;
        Map<Integer, Continent> l_continentMap = p_warMap.getD_continents();

        for (Map.Entry<Integer, Continent> l_entry : l_continentMap.entrySet()) {
            Continent l_currentContinent = l_entry.getValue();

            List<Country> l_countryList = l_currentContinent.getD_countryList();
            for (Country l_country : l_countryList) {
                if (l_country != null) {
                    if (l_country.getD_countryName() == p_countryName) {
                        l_countryIndex = l_country.getD_countryIndex();
                        break;
                    }
                }
            }
        }
        return l_countryIndex;
    }

    /**
     * This method will check edit map command and if file is already exist then
     * read the data of existing map file otherwise it will create new map file
     *
     * @param p_editMapCommand
     */
    public CommandResponse checkCommandEditMap(String p_editMapCommand) {
        String l_fileName = Arrays.asList(p_editMapCommand.split(" ")).get(1);

        if (validateIOString(l_fileName, "[a-zA-Z]+.?[a-zA-Z]+")) {
            List<String> l_mapFileNameList;
            try {
                l_mapFileNameList = getAvailableMapFiles();

                String l_fullName;
                int index = l_fileName.lastIndexOf('.');
                l_fullName = index > 0
                        ? l_fileName.toLowerCase() : l_fileName.toLowerCase() + ".map";

                // Set status and map file name 
                d_warMap.setD_status(true);
                d_warMap.setD_mapName(l_fullName);

                if (l_mapFileNameList.contains(l_fullName)) {
                    try {
                        d_warMap = readMap(l_fullName);
                        prepareResponse(true, "Map loaded successfully! Do not forget to save map file after editing");
                    } catch (Exception e) {
                        prepareResponse(false, "Exception in EditMap, Invalid Map Please correct Map");
                    }
                } else {
                    prepareResponse(true, "Map not found in system, new map is created. Pleaase do not forget to save map file after editing");
                }
            } catch (IOException ex) {
                prepareResponse(false, "Exception in editmap");
            }
        } else {
            prepareResponse(false, "Please enter valid file name for editMap command");
        }

        return d_commandResponse;
    }

    /**
     * This method is will used to get all available map files
     *
     * @return it will return list of map file
     * @throws java.io.IOException throws IO Exception
     */
    public List<String> getAvailableMapFiles() throws IOException {
        List<String> l_maps = new ArrayList<>();

        l_maps = getListOfAllFiles(Paths.get(MAP_DEF_PATH), ".map");

        return l_maps;
    }

    /**
     * List all files from this given path and extension
     *
     * @param p_path directory path of files
     * @param p_fileExtension extension of file to be searched
     * @return list of files
     */
    public List<String> getListOfAllFiles(Path p_path, String p_fileExtension)
            throws IOException {
        List<String> l_files;
        try (Stream<Path> l_walk = Files.walk(p_path)) {
            l_files = l_walk.map(filePath -> filePath.toFile().getName())
                    .filter(fileName -> fileName.endsWith(p_fileExtension))
                    .collect(Collectors.toList());
        }

        return l_files;
    }

    /**
     * This method will check savemap command and if it is valid then save map
     * to file
     *
     * @param p_fileName name of file that player want to store
     * @return object of commandResponse
     */
    public CommandResponse checkCommandSaveMap(String p_fileName) {
        boolean l_fileExtension = false;
        if (p_fileName.contains(".")) {
            String l_fileName = p_fileName.split("\\.")[1];

            if (l_fileName.equals("map")) {
                l_fileExtension = true;
            } else {
                l_fileExtension = false;
            }
        } else {
            p_fileName.concat(".map");
            l_fileExtension = true;
        }
        try {
            if (validateIOString(p_fileName, "[a-zA-Z]+.?[a-zA-Z]+") && l_fileExtension) {

                List<String> l_mapFileList = getAvailableMapFiles();
                if (!p_fileName.equalsIgnoreCase((l_mapFileList) + ".map")) {
                    if (validateMap(d_warMap)) {
                        writeMapToFile(d_warMap);
                        prepareResponse(true, "Map file succesfully saved");
                    } else {
                        prepareResponse(false, "Map is not valid");
                    }
                    //call validate function and writeMapToFile function    
                } else {
                    prepareResponse(false, "Map name is already exist please enter another name");
                }

            } else {
                prepareResponse(false, "Please enter valid file name");
            }

        } catch (Exception e) {
            prepareResponse(false, "Exception in savemap");
            e.printStackTrace();
        }

        return d_commandResponse;
    }

    /**
     * This function will validate map file
     *
     * @param p_warMap use to validate
     * @return return true if map is valid
     */
    public boolean validateMap(WarMap p_warMap) {
        boolean l_result = false;
        try {
            // check one or more continent is available in map
            if (p_warMap.getD_continents() != null && p_warMap.getD_continents().size() > 0) {
                ArrayList<Country> l_countries = getAvailableCountries(p_warMap);

                // check one or more country is available in map
                if (!l_countries.isEmpty()) {

                    // check graph is connected or not
                    l_countries = getAvailableCountries(d_warMap);
                    int l_countrySize = l_countries.size();
                    int l_i, l_j;
                    l_countrySize++;
                    Pair<Integer, String[][]> pair = prepareMetricesOfMap(l_countries);
                    String[][] l_mapMetrix = pair.getValue();

                    int[][] l_intMetric = new int[l_countrySize - 1][l_countrySize - 1];
                    for (l_i = 0; l_i < l_countrySize; l_i++) {
                        for (l_j = 0; l_j < l_countrySize; l_j++) {
                            if (l_i == 0 && l_j == 0) {
                                continue;
                            } else if (l_i == 0 && l_j != 0) {
                                continue;
                            } else if (l_j == 0 && l_i != 0) {
                                continue;
                            } else {
                                l_intMetric[l_i - 1][l_j - 1] = Integer.parseInt(l_mapMetrix[l_i][l_j]);
                            }
                        }
                    }

                    if (isConnected(l_intMetric, l_countrySize - 1)) {
                        l_result = true;
                    }
                }
            } else {
                l_result = false;
            }
        } catch (Exception e) {
            l_result = false;
        }

        return l_result;
    }

    /**
     * This method is used to check map is connected or not
     *
     * @param p_metricesOfMap metric of map
     * @param p_noOfCountries total number of map
     * @return returns true if map is connected
     */
    public boolean isConnected(int[][] p_metricesOfMap, int p_noOfCountries) {
        //created visited array
        boolean[] l_visited = new boolean[p_noOfCountries];

        //check if all the vertices are visited, if yes then graph is connected
        for (int l_i = 0; l_i < p_noOfCountries; l_i++) {
            for (int l_j = 0; l_j < p_noOfCountries; l_j++) {
                l_visited[l_j] = false;
            }
            traverse(l_i, l_visited, p_noOfCountries, p_metricesOfMap);
            for (int l_x = 0; l_x < p_noOfCountries; l_x++) {
                if (!l_visited[l_x]) //if there is a node, not visited by traversal, graph is not connected
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * This method is used to traverse metric
     *
     * @param p_source
     * @param p_visited
     * @param p_noOfCountries
     * @param p_metricesOfMap
     */
    public void traverse(int p_source, boolean[] p_visited, int p_noOfCountries, int[][] p_metricesOfMap) {
        p_visited[p_source] = true; //mark v as visited
        for (int l_i = 0; l_i < p_noOfCountries; l_i++) {
            if (p_metricesOfMap[p_source][l_i] == 1) {
                if (!p_visited[l_i]) {
                    traverse(l_i, p_visited, p_noOfCountries, p_metricesOfMap);
                }
            }
        }
    }
}
