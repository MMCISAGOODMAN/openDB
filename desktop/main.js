const { app, BrowserWindow, ipcMain, shell } = require('electron')
const path = require('path')
const fs = require('fs')
const { spawn } = require('child_process')
const http = require('http')

const SERVER_PORT = process.env.OPENDDB_SERVER_PORT || '18080'
const HEALTH_URL = `http://127.0.0.1:${SERVER_PORT}/api/health`
const STARTUP_TIMEOUT_MS = 120_000
const POLL_INTERVAL_MS = 500

let mainWindow = null
let javaProcess = null
let quitting = false

function resourcesRoot() {
  if (app.isPackaged) {
    return process.resourcesPath
  }
  return path.join(__dirname, '..')
}

function resolveJavaBinary() {
  const root = resourcesRoot()
  const candidates = []
  if (process.platform === 'win32') {
    candidates.push(path.join(root, 'jre', 'bin', 'java.exe'))
    candidates.push(path.join(root, 'jre', 'bin', 'java'))
  } else {
    candidates.push(path.join(root, 'jre', 'bin', 'java'))
    candidates.push(path.join(root, 'jre', 'Contents', 'Home', 'bin', 'java'))
  }
  if (process.env.JAVA_HOME) {
    candidates.unshift(path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java'))
  }
  candidates.push('java')
  for (const candidate of candidates) {
    if (candidate === 'java' || fs.existsSync(candidate)) {
      return candidate
    }
  }
  return 'java'
}

function resolveJarPath() {
  const root = resourcesRoot()
  const packaged = path.join(root, 'backend', 'app.jar')
  if (fs.existsSync(packaged)) {
    return packaged
  }
  const devJar = path.join(root, 'backend', 'target', 'opendb-server-0.2.0.jar')
  if (fs.existsSync(devJar)) {
    return devJar
  }
  throw new Error('Backend JAR not found. Run ./scripts/build-desktop.sh first.')
}

function userDataDir() {
  return path.join(app.getPath('userData'), 'opendb-data')
}

function startBackend() {
  const jarPath = resolveJarPath()
  const javaBinary = resolveJavaBinary()
  const dataDir = userDataDir()
  fs.mkdirSync(dataDir, { recursive: true })

  const args = [
    '-jar',
    jarPath,
    `--server.port=${SERVER_PORT}`,
    `--opendb.data-dir=${dataDir}`
  ]

  console.log('Starting backend:', javaBinary, args.join(' '))
  javaProcess = spawn(javaBinary, args, {
    stdio: ['ignore', 'pipe', 'pipe'],
    env: {
      ...process.env,
      OPENDDB_SERVER_PORT: SERVER_PORT,
      OPENDDB_DATA_DIR: dataDir
    }
  })

  javaProcess.stdout.on('data', chunk => console.log('[backend]', chunk.toString().trim()))
  javaProcess.stderr.on('data', chunk => console.error('[backend]', chunk.toString().trim()))
  javaProcess.on('exit', (code, signal) => {
    console.log('Backend exited', { code, signal })
    javaProcess = null
    if (!quitting && mainWindow) {
      mainWindow.webContents.executeJavaScript(
        `alert('openDB backend stopped unexpectedly (code ${code ?? signal}).')`
      ).catch(() => {})
    }
  })
}

function stopBackend() {
  if (javaProcess && !javaProcess.killed) {
    javaProcess.kill('SIGTERM')
    setTimeout(() => {
      if (javaProcess && !javaProcess.killed) {
        javaProcess.kill('SIGKILL')
      }
    }, 3000)
  }
  javaProcess = null
}

function waitForHealth() {
  const deadline = Date.now() + STARTUP_TIMEOUT_MS
  return new Promise((resolve, reject) => {
    const poll = () => {
      const req = http.get(HEALTH_URL, res => {
        res.resume()
        if (res.statusCode === 200) {
          resolve()
          return
        }
        schedule()
      })
      req.on('error', schedule)
      req.setTimeout(2000, () => {
        req.destroy()
        schedule()
      })
    }
    const schedule = () => {
      if (Date.now() > deadline) {
        reject(new Error(`Backend did not become ready at ${HEALTH_URL}`))
        return
      }
      setTimeout(poll, POLL_INTERVAL_MS)
    }
    poll()
  })
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 960,
    minHeight: 640,
    title: 'openDB',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  mainWindow.loadURL(`http://127.0.0.1:${SERVER_PORT}/`)
  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

ipcMain.handle('app:get-version', () => app.getVersion())
ipcMain.handle('shell:open-external', (_event, url) => shell.openExternal(url))

app.whenReady().then(async () => {
  try {
    startBackend()
    await waitForHealth()
    createWindow()
  } catch (error) {
    console.error(error)
    app.exit(1)
  }
})

app.on('before-quit', () => {
  quitting = true
  stopBackend()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', async () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    try {
      if (!javaProcess) {
        startBackend()
        await waitForHealth()
      }
      createWindow()
    } catch (error) {
      console.error(error)
    }
  }
})
