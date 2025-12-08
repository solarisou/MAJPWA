// Fonction pour récupérer le token CSRF depuis la page
function getCsrfToken() {
    const token = document.querySelector('meta[name="_csrf"]');
    if (token) {
        return token.getAttribute('content');
    }
    // Si on ne trouve pas le token dans les meta tags, on cherche dans un input caché
    const input = document.querySelector('input[name="_csrf"]');
    return input ? input.value : null;
}

document.addEventListener('DOMContentLoaded', function() {
    
    // On gère tous les boutons de suppression
    document.querySelectorAll('.delete-btn').forEach(deleteBtn => {
        deleteBtn.addEventListener('click', function(e) {
            e.preventDefault();
            
            // On récupère le formulaire parent
            const form = this.closest('form');
            const itemId = form.querySelector('input[name="id"]').value;
            
            // On demande confirmation
            if (!confirm('Supprimer cet article ?')) {
                return;
            }
            
            // On envoie la requête AJAX pour supprimer l'article
            const formData = new FormData();
            formData.append('id', itemId);
            
            // On ajoute le token CSRF si disponible
            const csrfToken = getCsrfToken();
            if (csrfToken) {
                formData.append('_csrf', csrfToken);
            }
            
            fetch('/shopping-list/delete', {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(response => {
                if (response.ok || response.redirected) {
                    // On supprime l'élément de la page sans recharger
                    const itemElement = form.closest('.shopping-list-item');
                    if (itemElement) {
                        // Animation de disparition
                        itemElement.style.transition = 'opacity 0.3s, transform 0.3s';
                        itemElement.style.opacity = '0';
                        itemElement.style.transform = 'translateX(-20px)';
                        setTimeout(() => {
                            itemElement.remove();
                            
                            // Si la liste est maintenant vide, on recharge la page pour afficher le message
                            const remainingItems = document.querySelectorAll('.shopping-list-item').length;
                            if (remainingItems === 0) {
                                window.location.reload();
                            }
                        }, 300);
                    } else {
                        // Si on ne trouve pas l'élément, on recharge la page
                        window.location.reload();
                    }
                } else {
                    alert('Erreur lors de la suppression');
                }
            })
            .catch(error => {
                console.error('Erreur:', error);
                alert('Erreur lors de la suppression');
            });
        });
    });
    
    // On gère tous les checkboxes pour cocher/décocher
    document.querySelectorAll('.checkbox-custom').forEach(checkbox => {
        checkbox.addEventListener('change', function(e) {
            e.preventDefault();
            
            // On récupère le formulaire parent
            const form = this.closest('form');
            const itemId = form.querySelector('input[name="id"]').value;
            const isChecked = this.checked;
            
            // On envoie la requête AJAX pour changer l'état
            const formData = new FormData();
            formData.append('id', itemId);
            
            // On ajoute le token CSRF si disponible
            const csrfToken = getCsrfToken();
            if (csrfToken) {
                formData.append('_csrf', csrfToken);
            }
            
            fetch('/shopping-list/toggle', {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(response => {
                if (response.ok || response.redirected) {
                    // On met à jour l'apparence visuelle de l'élément
                    const itemElement = form.closest('.shopping-list-item');
                    if (itemElement) {
                        if (isChecked) {
                            itemElement.classList.add('checked');
                        } else {
                            itemElement.classList.remove('checked');
                        }
                    }
                } else {
                    // En cas d'erreur, on remet la checkbox dans son état précédent
                    this.checked = !isChecked;
                    alert('Erreur lors de la modification');
                }
            })
            .catch(error => {
                console.error('Erreur:', error);
                // On remet la checkbox dans son état précédent
                this.checked = !isChecked;
                alert('Erreur lors de la modification');
            });
        });
    });
    
    // On gère le bouton "Nettoyer les articles cochés"
    const clearCheckedBtn = document.querySelector('form[action="/shopping-list/clear-checked"] button');
    if (clearCheckedBtn) {
        clearCheckedBtn.addEventListener('click', function(e) {
            e.preventDefault();
            
            if (!confirm('Supprimer tous les articles cochés ?')) {
                return;
            }
            
            // On envoie la requête AJAX
            const formData = new FormData();
            
            // On ajoute le token CSRF si disponible
            const csrfToken = getCsrfToken();
            if (csrfToken) {
                formData.append('_csrf', csrfToken);
            }
            
            fetch('/shopping-list/clear-checked', {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(response => {
                if (response.ok || response.redirected) {
                    // On recharge la page pour voir les changements
                    window.location.reload();
                } else {
                    alert('Erreur lors du nettoyage');
                }
            })
            .catch(error => {
                console.error('Erreur:', error);
                alert('Erreur lors du nettoyage');
            });
        });
    }
});

