package bdp.sample.notebookmanager.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import bdp.sample.notebookmanager.entities.NoteBook;

@Repository
public interface NoteBookRepository extends JpaRepository<NoteBook,Integer> {

    public Page<NoteBook> findAll(Pageable page);
}
