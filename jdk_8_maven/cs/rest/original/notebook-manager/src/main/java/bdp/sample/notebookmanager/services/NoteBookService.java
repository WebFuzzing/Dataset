package bdp.sample.notebookmanager.services;


import bdp.sample.notebookmanager.repositories.NoteBookRepository;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import bdp.sample.notebookmanager.entities.NoteBook;

import java.util.List;
import java.util.Optional;

@Service
public class NoteBookService {

    @Autowired
    private NoteBookRepository noteBookRepository;

    //This method gets page index and page size and returns records from database accordingly
    public List<NoteBook> listnotebooks(int page, int size){
        return noteBookRepository.findAll(PageRequest.of(page, size, Sort.by("ID").ascending())).toList();
    }

    // This method gets an Id and returns corresponding notebook if it exists, otherwise it returns null
    public NoteBook getnotebookById(int notebookId){
        Optional<NoteBook> notebook = noteBookRepository.findById(notebookId);
        if (notebook.isPresent())
            return notebook.get();
        // returns null if the given Id doesn't exist
        return null;
    }

    // This method creates given notebook object in the database and returns it with its Id
    public NoteBook createnotebook(NoteBook notebook){

        return noteBookRepository.save(notebook);
    }

    //This method gets a notebook object along with its Id, updates the name and currentPrice and returns it back
    //if the notebook Id doesn't exist, it returns null
    public NoteBook updatenotebook(int notebookId, NoteBook changednotebook){
        Optional<NoteBook> notebook = noteBookRepository.findById(notebookId);
        //Check if the notebook exists
        if (notebook.isPresent()) {
            NoteBook tempnotebook= notebook.get();
            tempnotebook.setName(changednotebook.getName());
            tempnotebook.setCurrentPrice(changednotebook.getCurrentPrice());
            // Save and return updated notebook object
            return noteBookRepository.save(tempnotebook);
        }

        // returns null if the given Id doesn't exist
        return null;
    }

    //This method gets a notebook Id and delete the corresponding notebook record in databse
    //if the notebook Id doesn't exist, it returns null
    public boolean deletenotebook(int notebookId){
        Optional<NoteBook> notebook = noteBookRepository.findById(notebookId);
        //Check if the notebook exists
        if (notebook.isPresent()) {
            noteBookRepository.delete(notebook.get());
            return true;
        }

        // returns null if the given Id doesn't exist
        return false;
    }

}
