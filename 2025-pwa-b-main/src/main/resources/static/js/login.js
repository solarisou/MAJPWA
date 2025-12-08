/*
 Fonction pour afficher/masquer un ou plusieurs champs mot de passe
 */
function togglePassword(inputId = 'password', iconId = 'toggleIcon') {
    const passwordInput = document.getElementById(inputId);
    const toggleIcon = document.getElementById(iconId);

    if (!passwordInput || !toggleIcon) return;

    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleIcon.classList.remove('fa-eye');
        toggleIcon.classList.add('fa-eye-slash');
    } else {
        passwordInput.type = 'password';
        toggleIcon.classList.remove('fa-eye-slash');
        toggleIcon.classList.add('fa-eye');
    }
}

/*
 Animation de la carte principale au chargement
 */
document.addEventListener('DOMContentLoaded', function() {
    const box = document.querySelector('.box');
    if (box) {
        box.style.opacity = '0';
        box.style.transform = 'translateY(20px)';
        
        setTimeout(() => {
            box.style.transition = 'all 0.5s ease';
            box.style.opacity = '1';
            box.style.transform = 'translateY(0)';
        }, 100);
    }
});
