package io.jzheaux.springsecurity.resolutions;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ResolutionController {

    private final ResolutionRepository resolutions;
    private final UserRepository user;

    public ResolutionController(ResolutionRepository resolutions, UserRepository user) {
        this.resolutions = resolutions;
        this.user = user;
    }

    @CrossOrigin(maxAge = 0, allowCredentials = "true") //maxAge if locally verifying - to avoid caching
    @GetMapping("/resolutions")
    @PreAuthorize("hasAuthority('resolution:read')")
    @PostFilter("@post.filter(#root)")
    public Iterable<Resolution> read() {
        Iterable<Resolution> resolutions = this.resolutions.findAll();
        resolutions.forEach(this::updateText);
        return resolutions;
    }

    private void updateText(Resolution resolution) {
        String fullName = this.user.findByUsername(resolution.getOwner()).map(User::getFullName).orElse("Anonymous");
        resolution.setText(resolution.getText() + " by " + fullName);
    }

    @GetMapping("/resolution/{id}")
    @PreAuthorize("hasAuthority('resolution:read')")
    @PostAuthorize("@post.authorize(#root)")
    public Optional<Resolution> read(@PathVariable("id") UUID id) {
        return this.resolutions.findById(id);
    }

    @PostMapping("/resolution")
    @PreAuthorize("hasAuthority('resolution:write')")
    public Resolution make(@CurrentUsername String owner, @RequestBody String text) {
        Resolution resolution = new Resolution(text, owner);
        return this.resolutions.save(resolution);
    }

    @PutMapping(path = "/resolution/{id}/revise")
    @PreAuthorize("hasAuthority('resolution:write')")
    @PostAuthorize("@post.authorize(#root)")
    @Transactional
    public Optional<Resolution> revise(@PathVariable("id") UUID id, @RequestBody String text) {
        this.resolutions.revise(id, text);
        return read(id);
    }

    @PutMapping("/resolution/{id}/complete")
    @PreAuthorize("hasAuthority('resolution:write')")
    @PostAuthorize("@post.authorize(#root)")
    @Transactional
    public Optional<Resolution> complete(@PathVariable("id") UUID id) {
        this.resolutions.complete(id);
        return read(id);
    }
}
