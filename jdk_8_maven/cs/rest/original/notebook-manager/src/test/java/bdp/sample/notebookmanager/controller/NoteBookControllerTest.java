package bdp.sample.notebookmanager.controller;

import bdp.sample.notebookmanager.entities.NoteBook;
import bdp.sample.notebookmanager.repositories.NoteBookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.Resource;
import javax.sql.DataSource;

import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/*
//@WebAppConfiguration() //"file:src/test/resources"
@ContextConfiguration( classes = { JpaConfig.class }) // loader = AnnotationConfigWebContextLoader.class,

*/

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureWebClient
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:beforeTestRun.sql")
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:afterTestRun.sql")
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NoteBookControllerTest {


    @Autowired
    MockMvc mockMvc;

    @Autowired
    DataSource dataSource;





    @Test
    void getListOfnotebooksWithConfigurablePageSize() throws Exception {
        // First page with page size of 2
        mockMvc.perform(get("/api/notebooks?page=0&pageSize=2")).andExpect(status().isOk())
                .andExpect(content().contentType("application/json")).andExpect(jsonPath("$._embedded.noteBookList[0].id").value("1"))
                .andExpect(jsonPath("$._embedded.noteBookList[1].id").value("2"));

        // Second page with page size of 3
        mockMvc.perform(get("/api/notebooks?page=1&pageSize=3")).andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$._embedded.noteBookList[0].id").value("4"))
                .andExpect(jsonPath("$._embedded.noteBookList[1].id").value("5"));

    }

    @Test
    void getSingleExistingnotebookById() throws Exception {

        // Get notebook with id=2
        mockMvc.perform(get("/api/notebooks/2")).andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value("2"))
                .andExpect(jsonPath("$.name").value("HP Inspiron"));

    }


    @Test
    void getSingleNotExistingnotebookById() throws Exception {

        // Get notebook with id=22
        mockMvc.perform(get("/api/notebooks/22"))
                .andExpect(status().isNotFound());

    }

    @Test
    void getSinglenotebookByInvalidnotebookId() throws Exception {

        // Get notebook with id=i
        mockMvc.perform(get("/api/notebooks/i"))
                .andExpect(status().isBadRequest());

        // Get notebook with id=*
        mockMvc.perform(get("/api/notebooks/*"))
                .andExpect(status().isBadRequest());

    }

    @Test
    void updatenotebookWithExistingnotebookId() throws Exception{

        NoteBook notebook = new NoteBook(2,"HP FitBook",305.99);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(notebook);
        // Update notebook with id = 2
        mockMvc.perform(patch("/api/notebooks/2").content(json).characterEncoding("utf-8")
                        .contentType("application/json"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.name").value("HP FitBook"))
                .andExpect(jsonPath("$.currentPrice").value(305.99));
    }


    @Test
    void updatenotebookWithNotExistingnotebookId() throws Exception{

        NoteBook notebook = new NoteBook(77,"HP FitBook",305.99);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(notebook);
        // Update notebook with id = 77 which is not existed
        mockMvc.perform(patch("/api/notebooks/77").content(json).characterEncoding("utf-8")
                        .contentType("application/json"))
                .andExpect(status().isNotFound());

    }


    @Test
    void createnotebook() throws Exception{

        NoteBook notebook = new NoteBook("HP FitBook",305.99);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(notebook);

        // Create new notebook
        mockMvc.perform(post("/api/notebooks").content(json).characterEncoding("utf-8")
                .contentType("application/json"))
                .andExpect(status().isCreated());

    }

    @Test
    void createNoteBookWithDuplicateName() throws Exception{

        NoteBook notebook = new NoteBook("Dell B3",305.99);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(notebook);

        // Create new notebook with duplicate name
        mockMvc.perform(post("/api/notebooks").content(json).characterEncoding("utf-8")
                        .contentType("application/json"))
                        .andExpect(status().isConflict());

    }

    @Test
    void deleteNoteBookWithExistingNoteBookId() throws Exception{

        // Delete notebook with id = 2
        mockMvc.perform(delete("/api/notebooks/2"))
                .andExpect(status().isAccepted());
    }


}