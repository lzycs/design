const fs = require('node:fs')
const path = require('node:path')

const backendTargetDir = path.resolve(__dirname, '../../backend/target')
const outputDir = path.resolve(__dirname, '../.electron/backend')
const outputPath = path.join(outputDir, 'app.jar')

function selectJarFile(files) {
  const jarFiles = files
    .filter(file => file.endsWith('.jar'))
    .filter(file => !file.endsWith('-sources.jar'))
    .filter(file => !file.endsWith('-javadoc.jar'))
    .filter(file => !file.startsWith('original-'))

  if (jarFiles.length === 0) {
    return null
  }

  jarFiles.sort((left, right) => left.localeCompare(right))
  return jarFiles[0]
}

if (!fs.existsSync(backendTargetDir)) {
  throw new Error(`未找到后端构建目录：${backendTargetDir}，请先执行后端打包。`)
}

const jarFile = selectJarFile(fs.readdirSync(backendTargetDir))
if (!jarFile) {
  throw new Error(`未在 ${backendTargetDir} 中找到可用的 JAR 文件。`)
}

fs.mkdirSync(outputDir, { recursive: true })
fs.copyFileSync(path.join(backendTargetDir, jarFile), outputPath)
console.log(`已复制后端 JAR：${jarFile} -> ${outputPath}`)
