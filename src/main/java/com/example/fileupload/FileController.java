package com.example.fileupload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class FileController {

    @Autowired
    private FileStorageService storageService;

    @Autowired
    private FileDBUsuariosRepository repository;

    @Autowired
    private UserModelAssembler assembler;

    @PostMapping("/upload")
    public ResponseEntity<com.example.fileupload.ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file) {
        String message = "";
        try {
            FileDB a = storageService.store(file);

            message = "Uploaded the file successfully: " + file.getOriginalFilename() + ". Share via id: " + a.getId();
            return ResponseEntity.status(HttpStatus.OK).body(new com.example.fileupload.ResponseMessage(message));
        } catch (Exception e) {
            message = "Could not upload the file: " + file.getOriginalFilename() + "!";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new com.example.fileupload.ResponseMessage(message));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<com.example.fileupload.ResponseFile>> getListFiles() {
        List<com.example.fileupload.ResponseFile> files = storageService.getAllFiles().map(dbFile -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/files/")
                    .path(dbFile.getId())
                    .toUriString();

            return new com.example.fileupload.ResponseFile(
                    dbFile.getName(),
                    fileDownloadUri,
                    dbFile.getType(),
                    dbFile.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(files);
    }


    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        FileDB fileDB = storageService.getFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDB.getName() + "\"")
                .body(fileDB.getData());
    }

    @GetMapping("/Usuarios")
    CollectionModel<EntityModel<FileDbUsuarios>> all() {

        List<EntityModel<FileDbUsuarios>> usuarios = repository.findAll().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return CollectionModel.of(usuarios, linkTo(methodOn(FileController.class).all()).withSelfRel());
    }

    @PostMapping("/Usuarios")
    ResponseEntity<ResponseMessage> newUser(@RequestBody String newUser) {
        String message = "";
       try {
           repository.save(new FileDbUsuarios(newUser.substring(5), "Cualquiera"));
           message = "User sing in successfully: " + newUser;
           return ResponseEntity.status(HttpStatus.OK).body(new com.example.fileupload.ResponseMessage(message));
       }
       catch (Exception e) {
           message = "Could not sign in: " + newUser;
           return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new com.example.fileupload.ResponseMessage(message));
       }
    }

    @GetMapping("/Usuarios/{id}")
    EntityModel<FileDbUsuarios> one(@PathVariable("id") Long id) {
        System.out.println(id);
        FileDbUsuarios Usuario =  repository.findById(id)
                .orElseThrow(() -> new com.example.fileupload.EmployeeNotFoundException(id));

        return assembler.toModel(Usuario);
    }

    @PutMapping("/Usuarios/{id}")
    ResponseEntity<?> replaceUser(@RequestBody FileDbUsuarios Usuario, @PathVariable Long id) {

        FileDbUsuarios updatedUsuario = repository.findById(id) //
                .map(employee -> {
                    employee.setName(Usuario.getName());
                    employee.setGrupo(Usuario.getGrupo());
                    return repository.save(employee);
                }) //
                .orElseGet(() -> {
                    Usuario.setId(id);
                    return repository.save(Usuario);
                });

        EntityModel<FileDbUsuarios> entityModel = assembler.toModel(updatedUsuario);

        return ResponseEntity //
                .created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()) //
                .body(entityModel);
    }

    @DeleteMapping("/Usuarios/{id}")
    ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        repository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
