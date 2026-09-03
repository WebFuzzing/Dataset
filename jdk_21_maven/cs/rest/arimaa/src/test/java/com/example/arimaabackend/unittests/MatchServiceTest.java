package com.example.arimaabackend.unittests;

import com.example.arimaabackend.dto.MatchResponse;
import com.example.arimaabackend.model.sql.*;
import com.example.arimaabackend.repository.sql.*;
import com.example.arimaabackend.services.MatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchServiceTest {

    @Mock
    private MatchJpaRepository matchRepository;
    @Mock
    private PlayerJpaRepository playerRepository;
    @Mock
    private UserJpaRepository userRepository;
    @Mock
    private EventJpaRepository eventRepository;
    @Mock
    private GameTypeJpaRepository gameTypeRepository;
    @Mock
    private CountryJpaRepository countryRepository;
    @Mock
    private MoveJpaRepository moveRepository;
    @Mock
    private PositionJpaRepository positionRepository;

    @InjectMocks
    private MatchServiceImpl matchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deleteMatch_validId_deletesMatchMovesAndPositions() {
        final Integer MATCH_ID = 669178;
        when(matchRepository.existsById(MATCH_ID)).thenReturn(true);

        matchService.deleteMatch(MATCH_ID);

        verify(positionRepository).deleteUnusedByMatchId(MATCH_ID);
        verify(moveRepository).deleteByMatch_Id(MATCH_ID);
        verify(matchRepository).deleteById(MATCH_ID);
    }

    @Test
    void deleteMatch_invalidId_throwsNotFound() {
        final Integer MATCH_ID = 999;
        when(matchRepository.existsById(MATCH_ID)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> matchService.deleteMatch(MATCH_ID));
    }

    @Test
    void createMatch_validString_createsAndSavesMatch() {
        String matchId = "27557";
        String goldId = "4803";
        String goldusername = "Matthias";
        String silverId = "4613";
        String silverUsername = "bot_GnoBot2006P1";

        String matchData =
                matchId + "\t" + goldId + "\t" +silverId + "\t"+goldusername+"\t"+silverUsername+
                        "\t\t\tUS\tAU\t1258\t1238\t98\t30\th\tb\tCasual game\tOver the Net" +
                        "\t2/2/100/10/8\t0\t1143867610\t1143867977\tb\tr\t11\tIGS\t1\t0\t1w Ra1 Rb1 Rc1 Rd1 Re1 Rf1 Rg1 Ch1 Ha2 Mb2 Dc2 Dd2 Ee2 Cf2 Hg2 Rh2\\n1b ra8 rb8 rc8 dd8 de8 rf8 rg8 rh8 hb7 ra7 cc7 ed7 me7 cf7 rh7 hg7\\n2w Ee2n Ee3n Ee4n Ee5n\\n2b ed7s me7w de8s dd8e\\n3w Ee6s de7s Ee5s de6s\\n3b ed6s de8s de5n ed5s\\n4w Ee4s Ee3w Rh2n Rh3w\\n4b hg7s rh7w hb7s ra7e\\n5w Rg3w Rf3w Ch1n Ch2n\\n5b ed4e md7s ee4w Re3n\\n6w Hg2n Hg3w Hf3n Ch3w\\n6b Re4n ed4e rf8w re8w\\n7w Cg3n Cg4n Mb2n Mb3n\t7b Hf4n ee4e Hf5n Hf6x ef4n\\n8w Ed3n Ed4n Ed5s md6s\\n8b ef5s Cg5w Cf5n Cf6x ef4n\\n9w Ed4w md5s md4s Ec4e\\n9b de6w Re5n Re6e Rf6x de7s\\n10w md3w mc3x Ed4s Ha2e Hb2n\\n10b ef5w ee5s ee4w ed4w\\n11w\tevents";

        when(playerRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(countryRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(eventRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(gameTypeRepository.findByName(anyString())).thenReturn(Optional.empty());

        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(countryRepository.save(any(CountryEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(playerRepository.save(any(PlayerEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(eventRepository.save(any(EventEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(gameTypeRepository.save(any(GameTypeEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(matchRepository.save(any(MatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        MatchResponse created = matchService.createMatch(matchData);

        assertNotNull(created);
        assertEquals(27557, created.id());
        assertEquals("b", created.matchResult());
        assertEquals("r", created.terminationType());
        assertEquals(4803, created.goldPlayer().id());
        assertEquals(4613, created.silverPlayer().id());
        assertEquals("Matthias", created.goldPlayer().username());
        assertEquals("bot_GnoBot2006P1", created.silverPlayer().username());
        assertEquals("Casual game", created.event().name());
        assertEquals("2/2/100/10/8", created.gameType().name());

        verify(matchRepository).save(any(MatchEntity.class));
    }
}
