document.addEventListener('DOMContentLoaded', () => {
    // Burger menu
    const $navbarBurgers = Array.prototype.slice.call(document.querySelectorAll('.navbar-burger'), 0);
    $navbarBurgers.forEach( el => {
        el.addEventListener('click', () => {
            const target = el.dataset.target;
            const $target = document.getElementById(target);
            el.classList.toggle('is-active');
            $target.classList.toggle('is-active');
        });
    });
    
    // Active link highlighting
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.navbar-item.nav-link');
    
    navLinks.forEach(link => {
        const linkPath = new URL(link.href).pathname;
        
        // Correspondance exacte
        if (currentPath === linkPath) {
            link.classList.add('is-active');
        }
        // Pour /recipes, activer aussi sur /recipes/xxx
        else if (linkPath === '/recipes' && currentPath.startsWith('/recipes')) {
            link.classList.add('is-active');
        }
        // Pour /scanner
        else if (linkPath === '/scanner' && currentPath.startsWith('/scanner')) {
            link.classList.add('is-active');
        }
        // Pour /pantry
        else if (linkPath === '/pantry' && currentPath.startsWith('/pantry')) {
            link.classList.add('is-active');
        }
        // Pour /shopping-list
        else if (linkPath === '/shopping-list' && currentPath.startsWith('/shopping-list')) {
            link.classList.add('is-active');
        }
    });
});