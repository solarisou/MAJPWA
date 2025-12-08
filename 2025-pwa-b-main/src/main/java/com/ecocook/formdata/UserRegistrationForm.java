package com.ecocook.formdata;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationForm {
    
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    String userName;
    
    @NotBlank(message = "Le nom est obligatoire")
    String displayName;
    
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Minimum 6 caractères")
    String password;
    
    @NotBlank(message = "Confirmez le mot de passe")
    String confirmPassword;
}

