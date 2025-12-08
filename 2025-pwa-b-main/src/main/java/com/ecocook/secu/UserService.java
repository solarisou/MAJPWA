package com.ecocook.secu;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ecocook.model.user.User;
import com.ecocook.model.user.UserRepository;

@Component
public class UserService implements UserDetailsService {

    @Inject
    UserRepository repo;

    @Inject
    PasswordEncoder encoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> opt = repo.findById(username);
        if (opt.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }
        User u = opt.get();
        return new org.springframework.security.core.userdetails.User(
            u.getUserName(), 
            u.getDerivedPassword(), 
            u.getRoles()
        );
    }

    public User saveUserComputingDerivedPassword(User u, String rawPassword) {
        String codedPassword = encoder.encode(rawPassword);
        u.setDerivedPassword(codedPassword);
        repo.save(u);
        return u;
    }

    public boolean userExists(String username) {
        return repo.existsById(username);
    }
}

