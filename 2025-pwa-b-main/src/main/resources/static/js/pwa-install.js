// PWA Install Handler pour installer la PWA Vue depuis le site classique
let deferredPrompt;
let isUserAuthenticated = false;

// Verifier si l'utilisateur est connecte
async function checkAuthentication() {
  try {
    const response = await fetch('/api/vue/user', { credentials: 'include' });
    isUserAuthenticated = response.ok;
    return isUserAuthenticated;
  } catch (e) {
    isUserAuthenticated = false;
    return false;
  }
}

// Injecter le manifest dynamiquement pour la PWA Vue (seulement si connecte)
function injectVueManifest() {
  // Verifier si le manifest n'est pas deja present
  if (!document.querySelector('link[rel="manifest"]')) {
    const link = document.createElement('link');
    link.rel = 'manifest';
    link.href = '/manifest.json';
    document.head.appendChild(link);
  }
}

// Afficher les elements d'installation si l'utilisateur est connecte
function showInstallUI() {
  if (!isUserAuthenticated) {
    console.log('PWA: Installation non disponible - utilisateur non connecte');
    return;
  }

  const installBtn = document.getElementById('pwa-install-btn');
  const installBanner = document.getElementById('pwa-install-banner');

  if (installBtn) {
    installBtn.style.display = 'inline-flex';
  }
  if (installBanner) {
    installBanner.style.display = 'flex';
  }

  console.log('PWA installable - prompt disponible pour utilisateur connecte');
}

// Initialiser au chargement de la page
document.addEventListener('DOMContentLoaded', async () => {
  await checkAuthentication();
  // N'injecter le manifest que si l'utilisateur est connecte
  if (isUserAuthenticated) {
    injectVueManifest();
  }
});

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPrompt = e;

  // Verifier l'authentification avant d'afficher les options d'installation
  checkAuthentication().then(() => {
    showInstallUI();
  });
});

// Fonction pour installer la PWA
async function installPWA() {
  // Verifier que l'utilisateur est connecte
  const isAuth = await checkAuthentication();
  if (!isAuth) {
    alert('Vous devez etre connecte pour installer l\'application.');
    return;
  }

  if (!deferredPrompt) {
    // PWA deja installee ou non disponible
    alert('L\'application est deja installee ou non disponible sur ce navigateur.');
    return;
  }

  deferredPrompt.prompt();

  deferredPrompt.userChoice.then((choiceResult) => {
    if (choiceResult.outcome === 'accepted') {
      console.log('PWA installee');
      // Cacher le bouton apres installation
      const installBtn = document.getElementById('pwa-install-btn');
      const installBanner = document.getElementById('pwa-install-banner');
      if (installBtn) installBtn.style.display = 'none';
      if (installBanner) installBanner.style.display = 'none';
    }
    deferredPrompt = null;
  });
}

// Cacher le bouton si deja installe et rediriger vers Vue
window.addEventListener('appinstalled', () => {
  console.log('PWA installee avec succes');
  deferredPrompt = null;
  const installBtn = document.getElementById('pwa-install-btn');
  const installBanner = document.getElementById('pwa-install-banner');
  if (installBtn) installBtn.style.display = 'none';
  if (installBanner) installBanner.style.display = 'none';

  // Rediriger vers l'app Vue apres installation
  setTimeout(() => {
    window.location.href = '/vue/';
  }, 500);
});
