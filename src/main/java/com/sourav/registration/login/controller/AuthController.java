package com.sourav.registration.login.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourav.registration.login.model.RoleEnum;
import com.sourav.registration.login.model.User;
import com.sourav.registration.login.model.UserRole;
import com.sourav.registration.login.payload.request.LoginRequest;
import com.sourav.registration.login.payload.request.SignupRequest;
import com.sourav.registration.login.payload.response.JwtResponse;
import com.sourav.registration.login.payload.response.MessageResponse;
import com.sourav.registration.login.repository.UserRepository;
import com.sourav.registration.login.repository.UserRoleRepository;
import com.sourav.registration.login.security.jwt.JwtUtils;
import com.sourav.registration.login.security.service.UserDetailsImpl;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserRoleRepository roleRepository;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/signin")
	public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok(
				new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles));
	}

	@PostMapping("/signup")
	public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		MessageResponse messageResponse = new MessageResponse(null);
		if (null != signUpRequest) {
			if (Boolean.TRUE.equals(userRepository.existsByUsername(signUpRequest.getUsername()))) {
				return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
			}

			if (Boolean.TRUE.equals(userRepository.existsByEmail(signUpRequest.getEmail()))) {
				return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
			}

			// Create new user's account
			User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(),
					encoder.encode(signUpRequest.getPassword()));

			Set<String> strRoles = signUpRequest.getRoles();
			Set<UserRole> roles = new HashSet<>();

			if (strRoles == null) {
				UserRole userRole = roleRepository.findByName(RoleEnum.ROLE_USER)
						.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
				roles.add(userRole);
			} else {
				strRoles.forEach(role -> {
					if (role.equals("admin")) {
						UserRole adminRole = roleRepository.findByName(RoleEnum.ROLE_ADMIN)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(adminRole);
					} else {
						UserRole userRole = roleRepository.findByName(RoleEnum.ROLE_USER)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(userRole);
					}
				});
			}

			user.setRoles(roles);
			userRepository.save(user);

			messageResponse = new MessageResponse("User registered successfully!");
		}
		return ResponseEntity.ok(messageResponse);
	}
}