package com.ecocook.model.user;

import org.springframework.data.repository.CrudRepository;

// on crée le repository pour les utilisateurs
public interface UserRepository extends CrudRepository<User, String> {
}

