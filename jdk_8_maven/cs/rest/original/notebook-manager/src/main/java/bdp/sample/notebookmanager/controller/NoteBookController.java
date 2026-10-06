package bdp.sample.notebookmanager.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import bdp.sample.notebookmanager.entities.NoteBook;
import bdp.sample.notebookmanager.services.NoteBookService;

import java.security.InvalidParameterException;
import java.util.List;

@RestController
@ExposesResourceFor(NoteBook.class)
@RequestMapping("/api/notebooks")
public class NoteBookController {

    @Autowired
    private EntityLinks entityLinks;

    @Autowired
    private NoteBookService noteBookService;


    // GET /api/notebooks (get a list of notebooks)
    @Operation(summary = "Get list of notebooks (Ordered by Id in ascending order), you must specify page number and page size")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful Operation",
                    content = { @Content(mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content)})
    @GetMapping(produces = { "application/json" })
    public ResponseEntity<CollectionModel<NoteBook>> getnotebooks(@Parameter(description = "Page index (start from 0)") @RequestParam(value = "page") Integer page, @Parameter(description = "Number of records per page") @RequestParam(value = "pageSize") Integer pageSize) {

        // Retrieve requested portion of notebooks from database
        List<NoteBook> notebookList = noteBookService.listnotebooks(page,pageSize);

        // Add self Link to each record as url for retrieving the record
        for (NoteBook notebook : notebookList) {
            Link recordSelfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(NoteBookController.class)
                    .getnotebookById(notebook.getID())).withSelfRel();
            notebook.add(recordSelfLink);
        }
        CollectionModel<NoteBook> resources = CollectionModel.of(notebookList);
        // selfLink to api according to HATEOS
        Link selfLink = entityLinks.linkToCollectionResource(NoteBook.class);
        resources.add(selfLink);

        // Send data to client as a response
        return new ResponseEntity(EntityModel.of(resources),HttpStatus.OK);
    }


    //GET /api/notebooks/1 (get one notebook from the list)
    @Operation(summary = "Get a notebook by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the notebook",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NoteBook.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "notebook not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content) })
    @GetMapping(value = "/{notebookId}", produces = { "application/json" })
    public ResponseEntity<EntityModel<NoteBook>> getnotebookById(@Parameter(description = "id of notebook to be retrieved")  @PathVariable int notebookId){

        // selfLink to api according to HATEOS
        Link selfLink = entityLinks.linkToItemResource(NoteBook.class, notebookId);
        // Retrieve requested notebook from database
        NoteBook notebook = noteBookService.getnotebookById(notebookId);
        //Check whether the notebook exist or not
        if(notebook!=null) {
            EntityModel<NoteBook> resource = EntityModel.of(notebook);
            resource.add(selfLink);
            // Send data to client as a response
            return new ResponseEntity(EntityModel.of(resource),HttpStatus.OK);
        }

        // If no notebook is found send 404 status code as response
        return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }


    // POST /api/notebooks (create a notebook)
    @Operation(summary = "Create a new notebook")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "notebook created",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NoteBook.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "notebook not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate notebook name",
                    content = @Content) })
    @PostMapping
    public ResponseEntity<EntityModel<NoteBook>> createnotebook(@RequestBody NoteBook notebook){

        // Create and Save notebook in database
        NoteBook storednotebook = noteBookService.createnotebook(notebook);

        // Check whether the notebook is saved or not
        if(notebook!=null) {
            // selfLink to api that retrieves the notebook according to HATEOS
            Link recordSelfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(NoteBookController.class)
                    .getnotebookById(notebook.getID())).withSelfRel();
            notebook.add(recordSelfLink);
            // Send created notebook with 201 status code to client
            return new ResponseEntity(EntityModel.of(storednotebook), HttpStatus.CREATED);
        }
        // If no notebook is saved send 304 status code
        return new ResponseEntity(null, HttpStatus.NOT_MODIFIED);
    }


    //PATCH /api/notebooks/1 (update the price of a single notebook)
    @Operation(summary = "Update a notebook by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "notebook updated",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NoteBook.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "notebook not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate notebook name",
                    content = @Content) })
    @PatchMapping(value = "/{notebookId}", produces = { "application/json" })
    public ResponseEntity<EntityModel<NoteBook>> updatenotebook(@Parameter(description = "id of notebook to be updated")  @PathVariable int notebookId, @Parameter(description = "notebook updated information")  @RequestBody NoteBook notebook){

        // Update notebook in the database
        NoteBook storednotebook = noteBookService.updatenotebook(notebookId,notebook);

        // Check whether the notebook exist and is updated or not
        if(storednotebook!=null) {
            // selfLink to api that retrieves the notebook according to HATEOS
            Link recordSelfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(NoteBookController.class)
                    .getnotebookById(notebook.getID())).withSelfRel();
            notebook.add(recordSelfLink);
            // Send updated notebook with 202 status code to client
            return new ResponseEntity(EntityModel.of(storednotebook), HttpStatus.ACCEPTED);
        }

        // If no notebook is found send 404 status code as response
        return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }

    // DELETE/api/notebooks/1 (delete a single notebook)
    @Operation(summary = "Delete a notebook by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "notebook deleted",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "notebook not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content) })
    @DeleteMapping(value = "/{notebookId}")
    public ResponseEntity<EntityModel<NoteBook>> deletenotebook(@Parameter(description = "id of notebook to be deleted")  @PathVariable int notebookId){

        // Delete the notebook by its Id and check for the result
        if(noteBookService.deletenotebook(notebookId))
            // Sent empty response with 202 status code
            return new ResponseEntity(null, HttpStatus.ACCEPTED);
        else
            // If no notebook is found send 404 status code as response
            return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }




        // Handling exceptions related to user input
        @ExceptionHandler({ MissingServletRequestParameterException.class, InvalidParameterException.class, MethodArgumentTypeMismatchException.class })
        public ResponseEntity<String> handleUserInputException() {

            return new ResponseEntity("Bad Request", HttpStatus.BAD_REQUEST);
        }

    // Handling runtime exception
    @ExceptionHandler({ RuntimeException.class })
    public ResponseEntity<String> handleRuntimeException() {

        return new ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @ExceptionHandler({ DataIntegrityViolationException.class })
    public ResponseEntity<String> handleDuplicateException() {

        return new ResponseEntity(null, HttpStatus.CONFLICT);
    }
}
