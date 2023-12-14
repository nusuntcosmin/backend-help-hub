package com.donatii.donatiiapi.controller;

import com.donatii.donatiiapi.model.Cauza;
import com.donatii.donatiiapi.model.User;
import com.donatii.donatiiapi.service.exceptions.EmptyObjectException;
import com.donatii.donatiiapi.service.exceptions.NotFoundException;
import com.donatii.donatiiapi.service.interfaces.ICauzaService;
import com.donatii.donatiiapi.service.interfaces.IUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/cauza")
@Tag(name = "Cauza")
public class CauzaController {
    private final ICauzaService cauzaService;
    private final IUserService userService;

    @Autowired
    public CauzaController(ICauzaService cauzaService, IUserService userService) {
        this.cauzaService = cauzaService;
        this.userService = userService;
    }

    @PostMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> save(@PathVariable("userId") Long userId, @RequestBody Cauza cauza) {
        try {
            try {
                cauza.setPoze(new HashSet<>());

                User user = userService.findById(userId);
                cauza = cauzaService.save(cauza);
                user.getCauze().add(cauza);
                userService.save(user);
            }
            catch (NotFoundException e) {
                return ResponseEntity.badRequest().body("User not found!");
            }
            return ResponseEntity.ok().body(cauza);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findById(@PathVariable("id") Long id) {
        try {
            Cauza cauza = cauzaService.findById(id);
            return ResponseEntity.ok().body(cauza);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findAll() {
        try {
            List<Cauza> cauze = cauzaService.findAll();
            return ResponseEntity.ok().body(cauze);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/sorted", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findAllSorted() {
        try {
            List<Cauza> cauze = cauzaService.findAllSorted();
            return ResponseEntity.ok().body(cauze);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findAllByUser(@PathVariable("userId") Long userId) {
        try {
            User user = userService.findById(userId);
            List<Cauza> cauze = cauzaService.findAllByUser(user);
            return ResponseEntity.ok().body(cauze);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable("id") Long id, @RequestBody Cauza cauza) {
        try {
            cauzaService.update(id, cauza);
            return ResponseEntity.ok().body("Cauza updated!");
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable("id") Long id) {
        try {
            Cauza cauza = cauzaService.findById(id);
            userService.deleteCauzaFromUser(cauza);
            return ResponseEntity.ok().body("Cauza deleted!");
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<Object> filter(@RequestParam String locatie,
                                         @RequestParam Integer sumMin,
                                         @RequestParam Integer sumMax,
                                         @RequestParam Boolean rezolvate,
                                         @RequestParam Boolean adaposturi) {
        try {
            List<Cauza> cauze = cauzaService.filter(locatie, sumMin, sumMax, rezolvate, adaposturi);
            return ResponseEntity.ok().body(cauze);

        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @GetMapping(value = "/image/{url}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Object> image(@PathVariable("url") String url) {
        ByteArrayResource inputStream;
        try {
            inputStream = new ByteArrayResource(Files.readAllBytes(Paths.get(
                    "assets/images/cases/" + url
            )));
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentLength(inputStream.contentLength())
                    .body(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nonexistent image!");
    }


    @PostMapping("/saveImages/{id}")
    public ResponseEntity<String> savePictures(@PathVariable("id") Long id, @RequestParam("pictures") MultipartFile[] pictures) {
        try {
            Cauza cauza = cauzaService.findById(id);
            cauza.getPoze().clear();
        } catch (NotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        for (MultipartFile picture : pictures) {
            try {
                Files.write(Paths.get("assets/images/cases/" + id + picture.getOriginalFilename()), picture.getBytes());
                cauzaService.savePicture("/cauza/image/" + id + picture.getOriginalFilename(), id);
            } catch (IOException | NotFoundException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body(e.getMessage());
            } catch (EmptyObjectException e) {
                throw new RuntimeException(e);
            }
        }
        return ResponseEntity.ok("Pictures saved successfully");
    }

    @PutMapping("/donate/{cauzaId}/{userId}/{sum}/{currency}")
    public ResponseEntity<Object> donate(@PathVariable("cauzaId") Long cauzaId,
                                         @PathVariable("userId") Long userId,
                                         @PathVariable("sum") Integer sum,
                                         @PathVariable("currency") String currency) {
        try {
            cauzaService.donate(cauzaId, sum);
            return ResponseEntity.ok().body(userService.donate(userId, sum, currency, cauzaService.findById(cauzaId)));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
