package com.soen6441.warzone.serviceImplTest;

import com.soen6441.warzone.model.CommandResponse;
import com.soen6441.warzone.model.GameData;
import com.soen6441.warzone.model.Player;
import com.soen6441.warzone.service.GameConfigService;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

/**
 * This Class will test business logic of GameConfigService.
 *
 * @author <a href="mailto:g_dobari@encs.concordia.ca">Gaurang Dobariya</a>
 * @version 1.0.0
 * @see com.soen6441.warzone.service.GameConfigService
 * @see com.soen6441.warzone.service.impl.GameConfigServiceImpl
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class GameConfigServiceTest {

    @Autowired
    GameConfigService d_gameConfigService;

    @Autowired
    GameData d_gameData;

    /**
     * This method is used to load SpringBoot Application Context
     */
    @Test
    public void contextLoads() {

    }

    public GameConfigServiceTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        d_gameData = new GameData();
    }

    @After
    public void tearDown() {
    }

    /**
     * This method is used to test whether player is added or not
     *
     */
    @Test
    public void testForUpdatePlayer() {

        Player l_expectedPlayer = new Player();
        l_expectedPlayer.setD_playerName("user");
        Player l_actualPlayer = new Player();

        Map.Entry<GameData, CommandResponse> l_gamePlayCommandResponseEntry = d_gameConfigService.updatePlayer(d_gameData, "gameplayer -add " + l_expectedPlayer.getD_playerName());
        if (l_gamePlayCommandResponseEntry.getValue().isD_isValid()) {
            GameData l_gameData = l_gamePlayCommandResponseEntry.getKey();
            if (!l_gameData.getD_playerList().isEmpty()) {
                l_actualPlayer = l_gameData.getD_playerList().get(0);
            }
        }
        assertEquals(l_expectedPlayer, l_actualPlayer);
    }
}
