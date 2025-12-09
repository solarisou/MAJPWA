const DB_NAME = 'ecocook-db'
const DB_VERSION = 2
const PRODUCTS_STORE = 'products'
const RECIPES_STORE = 'recipes'
const SHOPPING_STORE = 'shopping'
const PENDING_SYNC_STORE = 'pending_sync'

let db = null

export function openDB() {
  return new Promise((resolve, reject) => {
    if (db) {
      resolve(db)
      return
    }

    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onerror = () => reject(request.error)
    request.onsuccess = () => {
      db = request.result
      resolve(db)
    }

    request.onupgradeneeded = (event) => {
      const database = event.target.result

      if (!database.objectStoreNames.contains(PRODUCTS_STORE)) {
        const productsStore = database.createObjectStore(PRODUCTS_STORE, { keyPath: 'id', autoIncrement: true })
        productsStore.createIndex('storageType', 'storageType', { unique: false })
        productsStore.createIndex('name', 'name', { unique: false })
        productsStore.createIndex('syncStatus', 'syncStatus', { unique: false })
      }

      if (!database.objectStoreNames.contains(RECIPES_STORE)) {
        const recipesStore = database.createObjectStore(RECIPES_STORE, { keyPath: 'recipeId' })
        recipesStore.createIndex('name', 'name', { unique: false })
      }

      if (!database.objectStoreNames.contains(SHOPPING_STORE)) {
        const shoppingStore = database.createObjectStore(SHOPPING_STORE, { keyPath: 'id', autoIncrement: true })
        shoppingStore.createIndex('checked', 'checked', { unique: false })
        shoppingStore.createIndex('syncStatus', 'syncStatus', { unique: false })
      }

      if (!database.objectStoreNames.contains(PENDING_SYNC_STORE)) {
        const syncStore = database.createObjectStore(PENDING_SYNC_STORE, { keyPath: 'id', autoIncrement: true })
        syncStore.createIndex('action', 'action', { unique: false })
        syncStore.createIndex('timestamp', 'timestamp', { unique: false })
        syncStore.createIndex('type', 'type', { unique: false })
      }
    }
  })
}

// PRODUITS

export async function saveAllProducts(products) {
  const database = await openDB()
  const tx = database.transaction(PRODUCTS_STORE, 'readwrite')
  const store = tx.objectStore(PRODUCTS_STORE)

  await new Promise((resolve, reject) => {
    const clearRequest = store.clear()
    clearRequest.onsuccess = resolve
    clearRequest.onerror = () => reject(clearRequest.error)
  })

  for (const product of products) {
    product.syncStatus = 'synced'
    store.add(product)
  }

  return new Promise((resolve, reject) => {
    tx.oncomplete = resolve
    tx.onerror = () => reject(tx.error)
  })
}

export async function getAllProducts() {
  const database = await openDB()
  const tx = database.transaction(PRODUCTS_STORE, 'readonly')
  const store = tx.objectStore(PRODUCTS_STORE)

  return new Promise((resolve, reject) => {
    const request = store.getAll()
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

export async function getProductsByStorage(storageType) {
  const database = await openDB()
  const tx = database.transaction(PRODUCTS_STORE, 'readonly')
  const store = tx.objectStore(PRODUCTS_STORE)
  const index = store.index('storageType')

  return new Promise((resolve, reject) => {
    const request = index.getAll(storageType)
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

export async function addProduct(product) {
  const database = await openDB()
  const tx = database.transaction([PRODUCTS_STORE, PENDING_SYNC_STORE], 'readwrite')
  const productsStore = tx.objectStore(PRODUCTS_STORE)
  const syncStore = tx.objectStore(PENDING_SYNC_STORE)

  product.syncStatus = 'pending'
  product.localId = Date.now()

  const productRequest = productsStore.add(product)

  return new Promise((resolve, reject) => {
    productRequest.onsuccess = () => {
      const localId = productRequest.result
      syncStore.add({
        type: 'product',
        action: 'add',
        productLocalId: localId,
        product: { ...product, id: localId },
        timestamp: Date.now()
      })
      resolve({ ...product, id: localId })
    }
    productRequest.onerror = () => reject(productRequest.error)
  })
}

export async function deleteProduct(productId) {
  const database = await openDB()
  const tx = database.transaction([PRODUCTS_STORE, PENDING_SYNC_STORE], 'readwrite')
  const productsStore = tx.objectStore(PRODUCTS_STORE)
  const syncStore = tx.objectStore(PENDING_SYNC_STORE)

  const getRequest = productsStore.get(productId)

  return new Promise((resolve, reject) => {
    getRequest.onsuccess = () => {
      const product = getRequest.result
      if (product) {
        productsStore.delete(productId)
        if (product.syncStatus === 'synced' && product.serverId) {
          syncStore.add({
            type: 'product',
            action: 'delete',
            serverId: product.serverId,
            timestamp: Date.now()
          })
        }
      }
      resolve()
    }
    getRequest.onerror = () => reject(getRequest.error)
  })
}

export async function updateProductServerId(localId, serverId) {
  const database = await openDB()
  const tx = database.transaction(PRODUCTS_STORE, 'readwrite')
  const store = tx.objectStore(PRODUCTS_STORE)

  return new Promise((resolve, reject) => {
    const getRequest = store.get(localId)
    getRequest.onsuccess = () => {
      const product = getRequest.result
      if (product) {
        product.serverId = serverId
        product.syncStatus = 'synced'
        const updateRequest = store.put(product)
        updateRequest.onsuccess = resolve
        updateRequest.onerror = () => reject(updateRequest.error)
      } else {
        resolve()
      }
    }
    getRequest.onerror = () => reject(getRequest.error)
  })
}

// RECETTES

export async function saveAllRecipes(recipes) {
  const database = await openDB()
  const tx = database.transaction(RECIPES_STORE, 'readwrite')
  const store = tx.objectStore(RECIPES_STORE)

  await new Promise((resolve, reject) => {
    const clearRequest = store.clear()
    clearRequest.onsuccess = resolve
    clearRequest.onerror = () => reject(clearRequest.error)
  })

  for (const recipe of recipes) {
    const cleanRecipe = JSON.parse(JSON.stringify(recipe))
    store.put(cleanRecipe)
  }

  return new Promise((resolve, reject) => {
    tx.oncomplete = resolve
    tx.onerror = () => reject(tx.error)
  })
}

export async function getAllRecipes() {
  const database = await openDB()
  const tx = database.transaction(RECIPES_STORE, 'readonly')
  const store = tx.objectStore(RECIPES_STORE)

  return new Promise((resolve, reject) => {
    const request = store.getAll()
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

export async function deleteRecipe(recipeId) {
  const database = await openDB()
  const tx = database.transaction([RECIPES_STORE, PENDING_SYNC_STORE], 'readwrite')
  const recipesStore = tx.objectStore(RECIPES_STORE)
  const syncStore = tx.objectStore(PENDING_SYNC_STORE)

  recipesStore.delete(recipeId)
  syncStore.add({
    type: 'recipe',
    action: 'delete',
    recipeId: recipeId,
    timestamp: Date.now()
  })

  return new Promise((resolve, reject) => {
    tx.oncomplete = resolve
    tx.onerror = () => reject(tx.error)
  })
}

// LISTE DE COURSES

export async function saveAllShoppingItems(items) {
  const database = await openDB()
  const tx = database.transaction(SHOPPING_STORE, 'readwrite')
  const store = tx.objectStore(SHOPPING_STORE)

  await new Promise((resolve, reject) => {
    const clearRequest = store.clear()
    clearRequest.onsuccess = resolve
    clearRequest.onerror = () => reject(clearRequest.error)
  })

  for (const item of items) {
    const cleanItem = JSON.parse(JSON.stringify(item))
    cleanItem.syncStatus = 'synced'
    store.add(cleanItem)
  }

  return new Promise((resolve, reject) => {
    tx.oncomplete = resolve
    tx.onerror = () => reject(tx.error)
  })
}

export async function getAllShoppingItems() {
  const database = await openDB()
  const tx = database.transaction(SHOPPING_STORE, 'readonly')
  const store = tx.objectStore(SHOPPING_STORE)

  return new Promise((resolve, reject) => {
    const request = store.getAll()
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

export async function addShoppingItem(item) {
  const database = await openDB()
  const tx = database.transaction([SHOPPING_STORE, PENDING_SYNC_STORE], 'readwrite')
  const shoppingStore = tx.objectStore(SHOPPING_STORE)
  const syncStore = tx.objectStore(PENDING_SYNC_STORE)

  item.syncStatus = 'pending'
  item.localId = Date.now()
  item.checked = false

  const itemRequest = shoppingStore.add(item)

  return new Promise((resolve, reject) => {
    itemRequest.onsuccess = () => {
      const localId = itemRequest.result
      syncStore.add({
        type: 'shopping',
        action: 'add',
        itemLocalId: localId,
        item: { ...item, id: localId },
        timestamp: Date.now()
      })
      resolve({ ...item, id: localId })
    }
    itemRequest.onerror = () => reject(itemRequest.error)
  })
}

export async function toggleShoppingItem(itemId) {
  const database = await openDB()
  const tx = database.transaction([SHOPPING_STORE, PENDING_SYNC_STORE], 'readwrite')
  const shoppingStore = tx.objectStore(SHOPPING_STORE)
  const syncStore = tx.objectStore(PENDING_SYNC_STORE)

  const getRequest = shoppingStore.get(itemId)

  return new Promise((resolve, reject) => {
    getRequest.onsuccess = () => {
      const item = getRequest.result
      if (item) {
        item.checked = !item.checked
        shoppingStore.put(item)
        if (item.serverId) {
          syncStore.add({
            type: 'shopping',
            action: 'toggle',
            serverId: item.serverId,
            timestamp: Date.now()
          })
        }
        resolve(item)
      } else {
        resolve(null)
      }
    }
    getRequest.onerror = () => reject(getRequest.error)
  })
}

export async function deleteShoppingItem(itemId) {
  const database = await openDB()
  const tx = database.transaction([SHOPPING_STORE, PENDING_SYNC_STORE], 'readwrite')
  const shoppingStore = tx.objectStore(SHOPPING_STORE)
  const syncStore = tx.objectStore(PENDING_SYNC_STORE)

  const getRequest = shoppingStore.get(itemId)

  return new Promise((resolve, reject) => {
    getRequest.onsuccess = () => {
      const item = getRequest.result
      if (item) {
        shoppingStore.delete(itemId)
        if (item.syncStatus === 'synced' && item.serverId) {
          syncStore.add({
            type: 'shopping',
            action: 'delete',
            serverId: item.serverId,
            timestamp: Date.now()
          })
        }
      }
      resolve()
    }
    getRequest.onerror = () => reject(getRequest.error)
  })
}

export async function updateShoppingItemServerId(localId, serverId) {
  const database = await openDB()
  const tx = database.transaction(SHOPPING_STORE, 'readwrite')
  const store = tx.objectStore(SHOPPING_STORE)

  return new Promise((resolve, reject) => {
    const getRequest = store.get(localId)
    getRequest.onsuccess = () => {
      const item = getRequest.result
      if (item) {
        item.serverId = serverId
        item.syncStatus = 'synced'
        const updateRequest = store.put(item)
        updateRequest.onsuccess = resolve
        updateRequest.onerror = () => reject(updateRequest.error)
      } else {
        resolve()
      }
    }
    getRequest.onerror = () => reject(getRequest.error)
  })
}

// SYNC

export async function getPendingSync() {
  const database = await openDB()
  const tx = database.transaction(PENDING_SYNC_STORE, 'readonly')
  const store = tx.objectStore(PENDING_SYNC_STORE)

  return new Promise((resolve, reject) => {
    const request = store.getAll()
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

export async function removePendingSync(syncId) {
  const database = await openDB()
  const tx = database.transaction(PENDING_SYNC_STORE, 'readwrite')
  const store = tx.objectStore(PENDING_SYNC_STORE)

  return new Promise((resolve, reject) => {
    const request = store.delete(syncId)
    request.onsuccess = resolve
    request.onerror = () => reject(request.error)
  })
}

export async function clearAllData() {
  const database = await openDB()
  const tx = database.transaction([PRODUCTS_STORE, RECIPES_STORE, SHOPPING_STORE, PENDING_SYNC_STORE], 'readwrite')

  tx.objectStore(PRODUCTS_STORE).clear()
  tx.objectStore(RECIPES_STORE).clear()
  tx.objectStore(SHOPPING_STORE).clear()
  tx.objectStore(PENDING_SYNC_STORE).clear()

  return new Promise((resolve, reject) => {
    tx.oncomplete = resolve
    tx.onerror = () => reject(tx.error)
  })
}
