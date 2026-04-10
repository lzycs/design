const { app, BrowserWindow, dialog } = require('electron')
const { spawn } = require('node:child_process')
const fs = require('node:fs')
const http = require('node:http')
const net = require('node:net')
const path = require('node:path')

const BACKEND_PORT = 8080
const FRONTEND_PORT = 4173
const STATIC_HOST = '127.0.0.1'

let mainWindow = null
let backendProcess = null
let staticServer = null
let ownsBackendProcess = false

const isWindows = process.platform === 'win32'

function getFrontendDistPath() {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'app.asar', 'dist')
    : path.join(app.getAppPath(), 'dist')
}

function getDevelopmentJarPath() {
  const backendTargetDir = path.join(app.getAppPath(), '..', 'backend', 'target')
  if (!fileExists(backendTargetDir)) {
    return path.join(backendTargetDir, 'campus-learning-space.jar')
  }

  const jarFiles = fs.readdirSync(backendTargetDir)
    .filter(file => file.endsWith('.jar'))
    .filter(file => !file.endsWith('-sources.jar'))
    .filter(file => !file.endsWith('-javadoc.jar'))
    .filter(file => !file.startsWith('original-'))
    .sort((left, right) => left.localeCompare(right))

  return jarFiles.length > 0
    ? path.join(backendTargetDir, jarFiles[0])
    : path.join(backendTargetDir, 'campus-learning-space.jar')
}

function getBackendJarPath() {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'backend', 'app.jar')
    : getDevelopmentJarPath()
}

function fileExists(targetPath) {
  try {
    fs.accessSync(targetPath, fs.constants.F_OK)
    return true
  } catch {
    return false
  }
}

function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function isPortOpen(port, host = STATIC_HOST) {
  return new Promise(resolve => {
    const socket = new net.Socket()

    const finalize = result => {
      socket.destroy()
      resolve(result)
    }

    socket.setTimeout(800)
    socket.once('connect', () => finalize(true))
    socket.once('timeout', () => finalize(false))
    socket.once('error', () => finalize(false))
    socket.connect(port, host)
  })
}

async function waitForPort(port, host = STATIC_HOST, timeoutMs = 30000) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    if (await isPortOpen(port, host)) {
      return true
    }
    await wait(500)
  }
  return false
}

function getContentType(filePath) {
  const ext = path.extname(filePath).toLowerCase()
  const contentTypes = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.mjs': 'text/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.webp': 'image/webp',
    '.ico': 'image/x-icon',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.ttf': 'font/ttf',
  }

  return contentTypes[ext] || 'application/octet-stream'
}

function createStaticServer(distPath) {
  return http.createServer((req, res) => {
    const requestUrl = new URL(req.url || '/', `http://${STATIC_HOST}:${FRONTEND_PORT}`)
    const requestedPath = decodeURIComponent(requestUrl.pathname)
    const safeRelativePath = requestedPath.replace(/^\/+/, '') || 'index.html'

    let filePath = path.join(distPath, safeRelativePath)
    if (requestedPath.endsWith('/')) {
      filePath = path.join(distPath, safeRelativePath, 'index.html')
    }

    const normalizedPath = path.normalize(filePath)
    if (!normalizedPath.startsWith(path.normalize(distPath))) {
      res.writeHead(403)
      res.end('Forbidden')
      return
    }

    const servePath = fileExists(normalizedPath)
      ? normalizedPath
      : path.join(distPath, 'index.html')

    fs.readFile(servePath, (error, data) => {
      if (error) {
        res.writeHead(500)
        res.end('Failed to load app')
        return
      }

      res.writeHead(200, {
        'Content-Type': getContentType(servePath),
        'Cache-Control': 'no-cache',
      })
      res.end(data)
    })
  })
}

async function ensureStaticServer() {
  if (staticServer) {
    return `http://${STATIC_HOST}:${FRONTEND_PORT}`
  }

  const distPath = getFrontendDistPath()
  const indexPath = path.join(distPath, 'index.html')
  if (!fileExists(indexPath)) {
    throw new Error(`未找到前端构建产物：${indexPath}`)
  }

  staticServer = createStaticServer(distPath)

  await new Promise((resolve, reject) => {
    staticServer.once('error', reject)
    staticServer.listen(FRONTEND_PORT, STATIC_HOST, () => resolve())
  })

  return `http://${STATIC_HOST}:${FRONTEND_PORT}`
}

function resolveJavaCommand() {
  const javaHome = process.env.JAVA_HOME
  if (javaHome) {
    const candidate = path.join(javaHome, 'bin', isWindows ? 'java.exe' : 'java')
    if (fileExists(candidate)) {
      return candidate
    }
  }

  return isWindows ? 'java.exe' : 'java'
}

async function ensureBackendReady() {
  if (await isPortOpen(BACKEND_PORT)) {
    return
  }

  const jarPath = getBackendJarPath()
  if (!fileExists(jarPath)) {
    throw new Error(`未找到后端 JAR：${jarPath}`)
  }

  const javaCommand = resolveJavaCommand()
  backendProcess = spawn(javaCommand, ['-jar', jarPath], {
    cwd: path.dirname(jarPath),
    stdio: 'ignore',
    windowsHide: true,
  })
  ownsBackendProcess = true

  backendProcess.once('error', error => {
    dialog.showErrorBox('后端启动失败', `无法启动 Java 后端：${error.message}`)
  })

  const ready = await waitForPort(BACKEND_PORT)
  if (!ready) {
    throw new Error('后端未能在 30 秒内启动，请确认本机已安装 Java 21，并检查数据库与 Redis 配置。')
  }
}

async function createMainWindow() {
  await ensureBackendReady()
  const frontendUrl = await ensureStaticServer()

  mainWindow = new BrowserWindow({
    width: 1440,
    height: 960,
    minWidth: 1200,
    minHeight: 760,
    autoHideMenuBar: true,
    backgroundColor: '#f5f7fb',
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  })

  await mainWindow.loadURL(frontendUrl)

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

async function bootstrap() {
  try {
    await createMainWindow()
  } catch (error) {
    dialog.showErrorBox('桌面端启动失败', error instanceof Error ? error.message : String(error))
    app.quit()
  }
}

app.whenReady().then(bootstrap)

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('before-quit', () => {
  if (staticServer) {
    staticServer.close()
    staticServer = null
  }

  if (ownsBackendProcess && backendProcess && !backendProcess.killed) {
    backendProcess.kill()
  }
})

app.on('activate', async () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    await bootstrap()
  }
})
