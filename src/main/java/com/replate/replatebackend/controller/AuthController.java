package com.replate.replatebackend.controller;

import com.replate.replatebackend.enums.ERole;
import com.replate.replatebackend.model.Admin; // Import
import com.replate.replatebackend.model.Merchant; // Import
import com.replate.replatebackend.model.User;
import com.replate.replatebackend.payload.JwtResponse;
import com.replate.replatebackend.payload.LoginRequest;
import com.replate.replatebackend.payload.SignupRequest;
import com.replate.replatebackend.repository.UserRepository;
import com.replate.replatebackend.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Error: Role not found."));

        User user = userRepository.findByEmail(userDetails.getUsername()) // On cherche par email
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new JwtResponse(jwt,
                user.getId(),
                user.getEmail(),
                role));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user;
        String strRole = signUpRequest.getRole();
        ERole role;

        if (strRole == null) {
            return ResponseEntity.badRequest().body("Error: Role is not specified!");
        }

        try {
            role = ERole.valueOf(strRole.toUpperCase());

            if (role == ERole.ADMIN) {
                user = new Admin(
                        signUpRequest.getName(),
                        signUpRequest.getEmail(),
                        encoder.encode(signUpRequest.getPassword()),
                        signUpRequest.getPhone(),
                        signUpRequest.getAddress()
                );
            } else if (role == ERole.MERCHANT) {
                user = new Merchant(
                        signUpRequest.getName(),
                        signUpRequest.getEmail(),
                        encoder.encode(signUpRequest.getPassword()),
                        signUpRequest.getPhone(),
                        signUpRequest.getAddress()
                );
            } else {
                return ResponseEntity.badRequest().body("Error: Specified role is not supported!");
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: Role is invalid!");
        }


        userRepository.save(user);

        if (role == ERole.MERCHANT){
            return ResponseEntity.ok("Inscription réussie. Votre compte est en attente de vérification par un administrateur qui validera vos documents.");
        }

        return ResponseEntity.ok("User registered successfully!");
    }
}
