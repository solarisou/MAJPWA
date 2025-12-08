package com.ecocook.model.user;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "allusers")
public class User {
    
    @Id
    String userName;
    
    String displayName;
    String derivedPassword;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    Set<UserRole> roles = new HashSet<>();
    
    public User() {
    }
    
    public User(String userName) {
        this.userName = userName;
        this.displayName = userName;
        this.roles.add(UserRole.USER);
    }
}

