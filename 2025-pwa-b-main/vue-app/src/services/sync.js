import * as db from './db.js'

const API_URL = '/pantry/api/products'

export function isOnline() {
  return navigator.onLine
}

export async function fetchAndSaveProducts() {
  if (!isOnline()) {
    return await db.getAllProducts()
  }

  try {
    const response = await fetch(API_URL, { credentials: 'include' })
    if (!response.ok) {
      throw new Error('Erreur serveur')
    }

    const data = await response.json()
    const allProducts = [
      ...(data.frigo || []).map(p => ({ ...p, storageType: 'frigo', serverId: p.id })),
      ...(data.congelateur || []).map(p => ({ ...p, storageType: 'congelateur', serverId: p.id })),
      ...(data.placard || []).map(p => ({ ...p, storageType: 'placard', serverId: p.id })),
      ...(data.panier || []).map(p => ({ ...p, storageType: 'panier', serverId: p.id }))
    ]

    await db.saveAllProducts(allProducts)
    return allProducts
  } catch (error) {
    console.error('Erreur fetch produits:', error)
    return await db.getAllProducts()
  }
}

export async function getProducts() {
  if (isOnline()) {
    await syncPendingChanges()
    return await fetchAndSaveProducts()
  } else {
    return await db.getAllProducts()
  }
}

export async function getProductsGrouped() {
  const products = await getProducts()
  return {
    frigo: products.filter(p => p.storageType === 'frigo'),
    congelateur: products.filter(p => p.storageType === 'congelateur'),
    placard: products.filter(p => p.storageType === 'placard'),
    panier: products.filter(p => p.storageType === 'panier')
  }
}

export async function addProduct(product) {
  const savedProduct = await db.addProduct(product)

  if (isOnline()) {
    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          name: product.name,
          quantity: product.quantity,
          unit: product.unit,
          storageType: product.storageType,
          expiryDate: product.expiryDate
        })
      })

      if (response.ok) {
        const serverProduct = await response.json()
        await db.updateProductServerId(savedProduct.id, serverProduct.id)
        const pending = await db.getPendingSync()
        const syncItem = pending.find(s => s.productLocalId === savedProduct.id)
        if (syncItem) {
          await db.removePendingSync(syncItem.id)
        }
        return { ...serverProduct, storageType: product.storageType }
      }
    } catch (error) {
      console.warn('Sauvegarde locale, sync plus tard:', error)
    }
  }

  return savedProduct
}

export async function deleteProduct(productId, serverId) {
  await db.deleteProduct(productId)

  if (isOnline() && serverId) {
    try {
      await fetch(`${API_URL}/${serverId}`, {
        method: 'DELETE',
        credentials: 'include'
      })
      const pending = await db.getPendingSync()
      const syncItem = pending.find(s => s.serverId === serverId && s.action === 'delete')
      if (syncItem) {
        await db.removePendingSync(syncItem.id)
      }
    } catch (error) {
      console.warn('Suppression locale, sync plus tard:', error)
    }
  }
}

export async function syncPendingChanges() {
  if (!isOnline()) return

  const pendingItems = await db.getPendingSync()

  for (const item of pendingItems) {
    try {
      if (item.action === 'add') {
        const response = await fetch(API_URL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({
            name: item.product.name,
            quantity: item.product.quantity,
            unit: item.product.unit,
            storageType: item.product.storageType,
            expiryDate: item.product.expiryDate
          })
        })

        if (response.ok) {
          const serverProduct = await response.json()
          await db.updateProductServerId(item.productLocalId, serverProduct.id)
          await db.removePendingSync(item.id)
        }
      } else if (item.action === 'delete' && item.serverId) {
        const response = await fetch(`${API_URL}/${item.serverId}`, {
          method: 'DELETE',
          credentials: 'include'
        })

        if (response.ok || response.status === 404) {
          await db.removePendingSync(item.id)
        }
      }
    } catch (error) {
      console.error('Erreur sync:', error)
    }
  }
}

export function setupConnectivityListeners(onOnline, onOffline) {
  window.addEventListener('online', async () => {
    await syncPendingChanges()
    if (onOnline) onOnline()
  })

  window.addEventListener('offline', () => {
    if (onOffline) onOffline()
  })
}

export async function getPendingCount() {
  const pending = await db.getPendingSync()
  return pending.length
}
