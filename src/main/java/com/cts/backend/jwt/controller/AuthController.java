package com.cts.backend.jwt.controller;


import com.cts.backend.jwt.entity.AppUser;
import com.cts.backend.jwt.jwtprovider.JwtUtil;
import com.cts.backend.jwt.repo.UserRepository;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public String register(@RequestBody AppUser newUser) {
        if (userRepository.findByUsername(newUser.getUsername()) != null) {
            throw new RuntimeException("User already exists");
        }
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        userRepository.save(newUser);
        return "User registered successfully!";
    }

    @PostMapping("/login")
    public String login(@RequestBody AppUser loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        AppUser user = userRepository.findByUsername(loginRequest.getUsername());
        return JwtUtil.generateToken(user.getUsername(), user.getRole());
    }

    @GetMapping("/get")
    public String get(){
        return "Hello from jwt";
    }
}
