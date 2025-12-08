const INGREDIENT_SUGGESTIONS_ENDPOINT = '/api/products/search';
const INGREDIENT_DETAILS_ENDPOINT = '/api/products/details';
const DEFAULT_INGREDIENT_ICON = '/images/logoPannier.svg';

let ingredientIndex = document.querySelectorAll('#ingredientsList > .ingredient-form-item').length;

function addIngredient() {
    const container = document.getElementById('ingredientsList');
    const div = document.createElement('div');
    div.className = 'ingredient-form-item';
    div.innerHTML = `
        <div class="ingredient-form-row">
            <div class="ingredient-icon-cell">
                <img class="ingredient-icon-preview" src="${DEFAULT_INGREDIENT_ICON}" alt="Icône ingrédient" data-default="${DEFAULT_INGREDIENT_ICON}">
            </div>
            <div class="ingredient-input-main">
                <label class="form-label-small">Nom</label>
                <input class="form-input ingredient-name-input" type="text" name="ingredientNames"
                       placeholder="Nom de l'ingrédient" autocomplete="off">
                <div class="ingredient-suggestions"></div>
            </div>
            <div class="ingredient-input-small">
                <label class="form-label-small">Quantité</label>
                <input class="form-input" type="number" name="ingredientQuantities"
                       placeholder="Qté" min="0" step="0.01">
            </div>
            <div class="ingredient-input-small">
                <label class="form-label-small">Unité</label>
                <input class="form-input ingredient-unit-input" type="text" name="ingredientUnits"
                       placeholder="g, kg...">
            </div>
            <button type="button" class="btn-remove-ingredient remove-ingredient">
                <i class="fas fa-trash"></i>
            </button>
        </div>
    `;

    container.appendChild(div);
    ingredientIndex++;
    initializeIngredientRow(div);
}

function removeIngredient(button) {
    const container = document.getElementById('ingredientsList');
    if (container.children.length > 1) {
        const row = button.closest('.ingredient-form-item');
        if (row) {
            row.remove();
            reindexIngredients();
        }
    } else {
        alert('Une recette doit avoir au moins un ingrédient !');
    }
}

function reindexIngredients() {
    const container = document.getElementById('ingredientsList');
    const items = container.querySelectorAll('.ingredient-form-item');
    items.forEach(item => {
        const nameInput = item.querySelector('input[name^="ingredientNames"]');
        const quantityInput = item.querySelector('input[name^="ingredientQuantities"]');
        const unitInput = item.querySelector('input[name^="ingredientUnits"]');

        if (nameInput) nameInput.name = 'ingredientNames';
        if (quantityInput) quantityInput.name = 'ingredientQuantities';
        if (unitInput) unitInput.name = 'ingredientUnits';
    });
    ingredientIndex = items.length;
}

function attachRemoveEvent(button) {
    if (!button || button.dataset.removeBound === 'true') {
        return;
    }
    button.dataset.removeBound = 'true';
    button.addEventListener('click', function() {
        removeIngredient(this);
    });
}

function setupUnitInput(unitInput) {
    if (!unitInput) {
        return;
    }
    const initialValue = unitInput.value ? unitInput.value.trim() : '';
    unitInput.dataset.autofilled = initialValue ? 'initial' : 'false';
    unitInput.addEventListener('input', function() {
        unitInput.dataset.autofilled = 'manual';
        unitInput.style.background = '';
    });
}

function resetUnitAndIcon(unitInput, iconImg) {
    if (unitInput && unitInput.dataset.autofilled !== 'manual') {
        unitInput.value = '';
        unitInput.dataset.autofilled = 'false';
        unitInput.style.background = '';
    }
    if (iconImg && iconImg.dataset.autofilled !== 'manual') {
        iconImg.src = iconImg.dataset.default || DEFAULT_INGREDIENT_ICON;
        iconImg.dataset.autofilled = 'false';
        iconImg.style.background = '';
    }
}

function fetchIngredientDetails(productName, unitInput, iconImg) {
    fetch(`${INGREDIENT_DETAILS_ENDPOINT}?name=${encodeURIComponent(productName)}`)
        .then(response => response.ok ? response.json() : null)
        .then(product => {
            if (!product) {
                resetUnitAndIcon(unitInput, iconImg);
                return;
            }

            if (unitInput && unitInput.dataset.autofilled !== 'manual') {
                if (product.defaultUnit) {
                    unitInput.value = product.defaultUnit;
                    unitInput.dataset.autofilled = 'auto';
                    unitInput.style.background = '#e8f5e9';
                } else {
                    unitInput.value = '';
                    unitInput.dataset.autofilled = 'false';
                    unitInput.style.background = '';
                }
            }

            if (iconImg && iconImg.dataset.autofilled !== 'manual') {
                if (product.iconPath) {
                    iconImg.src = product.iconPath;
                    iconImg.dataset.autofilled = 'auto';
                    iconImg.style.background = '#f0fdf4';
                } else {
                    iconImg.src = iconImg.dataset.default || DEFAULT_INGREDIENT_ICON;
                    iconImg.dataset.autofilled = 'false';
                    iconImg.style.background = '';
                }
            }
        })
        .catch(() => resetUnitAndIcon(unitInput, iconImg));
}

function renderIngredientSuggestions(container, suggestions, onSelect) {
    if (!container) {
        return;
    }

    container.innerHTML = '';
    if (!suggestions || suggestions.length === 0) {
        container.classList.remove('is-active');
        container.dataset.hasSuggestions = 'false';
        return;
    }

    suggestions.forEach(suggestion => {
        const item = document.createElement('button');
        item.type = 'button';
        item.className = 'ingredient-suggestion-item';
        const icon = suggestion.iconPath || DEFAULT_INGREDIENT_ICON;
        item.innerHTML = `
            <img src="${icon}" alt="${suggestion.displayName}">
            <div class="suggestion-info">
                <span>${suggestion.displayName}</span>
                ${suggestion.defaultUnit ? `<small>Unité : ${suggestion.defaultUnit}</small>` : ''}
            </div>
        `;
        item.addEventListener('click', () => onSelect(suggestion));
        container.appendChild(item);
    });

    container.dataset.hasSuggestions = 'true';
    container.classList.add('is-active');
}

function clearIngredientSuggestions(container) {
    if (!container) {
        return;
    }
    container.innerHTML = '';
    container.classList.remove('is-active');
    container.dataset.hasSuggestions = 'false';
}

function setupIngredientAutocomplete(nameInput, unitInput, iconImg, suggestionsContainer) {
    if (!nameInput || !suggestionsContainer || nameInput.dataset.autocompleteBound === 'true') {
        return;
    }

    nameInput.dataset.autocompleteBound = 'true';
    let lastFetchId = 0;

    nameInput.addEventListener('input', function() {
        const query = nameInput.value.trim();
        if (query.length < 2) {
            clearIngredientSuggestions(suggestionsContainer);
            resetUnitAndIcon(unitInput, iconImg);
            return;
        }

        const fetchId = ++lastFetchId;
        fetch(`${INGREDIENT_SUGGESTIONS_ENDPOINT}?query=${encodeURIComponent(query)}`)
            .then(response => response.ok ? response.json() : [])
            .then(suggestions => {
                if (fetchId !== lastFetchId) {
                    return;
                }
                renderIngredientSuggestions(suggestionsContainer, suggestions, suggestion => {
                    nameInput.value = suggestion.displayName;
                    nameInput.dataset.technicalName = suggestion.technicalName;
                    if (iconImg && iconImg.dataset.autofilled !== 'manual') {
                        iconImg.src = suggestion.iconPath || (iconImg.dataset.default || DEFAULT_INGREDIENT_ICON);
                        iconImg.dataset.autofilled = suggestion.iconPath ? 'auto' : 'false';
                        iconImg.style.background = suggestion.iconPath ? '#f0fdf4' : '';
                    }
                    if (unitInput && unitInput.dataset.autofilled !== 'manual' && suggestion.defaultUnit) {
                        unitInput.value = suggestion.defaultUnit;
                        unitInput.dataset.autofilled = 'auto';
                        unitInput.style.background = '#e8f5e9';
                    }
                    clearIngredientSuggestions(suggestionsContainer);
                    fetchIngredientDetails(suggestion.displayName, unitInput, iconImg);
                });
            })
            .catch(() => clearIngredientSuggestions(suggestionsContainer));
    });

    nameInput.addEventListener('focus', function() {
        if (suggestionsContainer.dataset.hasSuggestions === 'true') {
            suggestionsContainer.classList.add('is-active');
        }
    });

    nameInput.addEventListener('blur', function() {
        setTimeout(() => clearIngredientSuggestions(suggestionsContainer), 150);
    });

    nameInput.addEventListener('change', function() {
        const productName = nameInput.value.trim();
        if (!productName) {
            resetUnitAndIcon(unitInput, iconImg);
            clearIngredientSuggestions(suggestionsContainer);
            return;
        }
        fetchIngredientDetails(productName, unitInput, iconImg);
        clearIngredientSuggestions(suggestionsContainer);
    });
}

function initializeIngredientRow(row) {
    const removeButton = row.querySelector('.remove-ingredient');
    attachRemoveEvent(removeButton);

    const unitInput = row.querySelector('.ingredient-unit-input');
    setupUnitInput(unitInput);

    const iconImg = row.querySelector('.ingredient-icon-preview');
    if (iconImg && !iconImg.dataset.default) {
        iconImg.dataset.default = iconImg.getAttribute('data-default') || DEFAULT_INGREDIENT_ICON;
    }

    const nameInput = row.querySelector('.ingredient-name-input');
    const suggestionsContainer = row.querySelector('.ingredient-suggestions');
    if (suggestionsContainer) {
        suggestionsContainer.dataset.hasSuggestions = 'false';
    }
    setupIngredientAutocomplete(nameInput, unitInput, iconImg, suggestionsContainer);

    if (nameInput && nameInput.value && nameInput.value.trim().length >= 2) {
        fetchIngredientDetails(nameInput.value.trim(), unitInput, iconImg);
    }
}

function validateForm(e) {
    const ingredients = document.querySelectorAll('input[name^="ingredientNames"]');
    let hasIngredient = false;
    const validIndexes = [];

    ingredients.forEach((input, index) => {
        const value = input.value.trim();
        if (value !== '') {
            hasIngredient = true;
            validIndexes.push(index);
            input.required = true;
        } else {
            input.required = false;
        }
    });

    if (!hasIngredient) {
        e.preventDefault();
        alert('Veuillez ajouter au moins un ingrédient à la recette !');
        return false;
    }

    const container = document.getElementById('ingredientsList');
    const items = Array.from(container.querySelectorAll('.ingredient-form-item'));
    items.forEach(item => {
        const nameInput = item.querySelector('input[name^="ingredientNames"]');
        if (nameInput && nameInput.value.trim() === '') {
            item.remove();
        }
    });

    reindexIngredients();

    return true;
}

document.addEventListener('DOMContentLoaded', function() {
    if (ingredientIndex === 0) {
        addIngredient();
    }

    document.querySelectorAll('#ingredientsList .ingredient-form-item').forEach(row => {
        initializeIngredientRow(row);
    });

    const addBtn = document.getElementById('addIngredientBtn');
    if (addBtn) {
        addBtn.addEventListener('click', addIngredient);
    }

    const form = document.getElementById('recipeForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            if (!validateForm(e)) {
                console.log('Formulaire bloqué par la validation');
            }
        });
    }

    document.querySelectorAll('.notification .delete').forEach(deleteButton => {
        deleteButton.addEventListener('click', function() {
            const notification = deleteButton.parentElement;
            if (notification) {
                notification.remove();
            }
        });
    });
});

