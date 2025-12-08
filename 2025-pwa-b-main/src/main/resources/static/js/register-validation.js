// Validation email côté client
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Validation du formulaire d'inscription
document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.querySelector('form[th\\:action="@{/register}"]');
    if (registerForm) {
        const emailInput = registerForm.querySelector('input[th\\:field="*{userName}"]');
        const passwordInput = registerForm.querySelector('input[th\\:field="*{password}"]');
        const confirmPasswordInput = registerForm.querySelector('input[th\\:field="*{confirmPassword}"]');
        
        if (emailInput) {
            emailInput.addEventListener('blur', function() {
                const email = this.value.trim();
                if (email && !validateEmail(email)) {
                    this.setCustomValidity('Veuillez entrer une adresse email valide (ex: exemple@email.com)');
                    this.classList.add('is-danger');
                } else {
                    this.setCustomValidity('');
                    this.classList.remove('is-danger');
                }
            });
            
            emailInput.addEventListener('input', function() {
                if (this.classList.contains('is-danger') && validateEmail(this.value.trim())) {
                    this.classList.remove('is-danger');
                    this.setCustomValidity('');
                }
            });
        }
        
        if (passwordInput && confirmPasswordInput) {
            function validatePasswords() {
                if (confirmPasswordInput.value && passwordInput.value !== confirmPasswordInput.value) {
                    confirmPasswordInput.setCustomValidity('Les mots de passe ne correspondent pas');
                    confirmPasswordInput.classList.add('is-danger');
                } else {
                    confirmPasswordInput.setCustomValidity('');
                    confirmPasswordInput.classList.remove('is-danger');
                }
            }
            
            passwordInput.addEventListener('input', validatePasswords);
            confirmPasswordInput.addEventListener('input', validatePasswords);
        }
    }
    
    // Validation du formulaire de connexion
    const loginForm = document.querySelector('form[th\\:action="@{/login}"]');
    if (loginForm) {
        const usernameInput = loginForm.querySelector('input[name="username"]');
        
        if (usernameInput) {
            usernameInput.addEventListener('blur', function() {
                const value = this.value.trim();
                // Si ça ressemble à un email, on valide le format
                if (value && value.includes('@') && !validateEmail(value)) {
                    this.setCustomValidity('Veuillez entrer une adresse email valide');
                    this.classList.add('is-danger');
                } else {
                    this.setCustomValidity('');
                    this.classList.remove('is-danger');
                }
            });
        }
    }
});

