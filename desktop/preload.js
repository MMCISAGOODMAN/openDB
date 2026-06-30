const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('opendbDesktop', {
  getVersion: () => ipcRenderer.invoke('app:get-version'),
  openExternal: (url) => ipcRenderer.invoke('shell:open-external', url)
})
