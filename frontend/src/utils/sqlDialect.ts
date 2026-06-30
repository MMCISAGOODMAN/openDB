import { MySQL, PostgreSQL, StandardSQL } from '@codemirror/lang-sql'

const TYPE_LABELS: Record<string, string> = {
  MYSQL: 'MySQL',
  POSTGRESQL: 'PostgreSQL',
  ORACLE: 'Oracle',
  H2: 'H2',
  DM: '达梦 DM8',
  KINGBASE: '人大金仓 KingbaseES',
  OPENGAUSS: 'openGauss'
}

export function getDatabaseTypeLabel(type?: string) {
  if (!type) return 'MySQL'
  return TYPE_LABELS[type.toUpperCase()] ?? type
}

export function getCodeMirrorDialect(type?: string) {
  switch (type?.toUpperCase()) {
    case 'POSTGRESQL':
    case 'KINGBASE':
    case 'OPENGAUSS':
      return PostgreSQL
    case 'ORACLE':
    case 'DM':
    case 'H2':
      return StandardSQL
    default:
      return MySQL
  }
}

export function getDefaultPort(type?: string) {
  switch (type?.toUpperCase()) {
    case 'POSTGRESQL':
    case 'OPENGAUSS':
      return 5432
    case 'ORACLE':
      return 1521
    case 'H2':
      return 9092
    case 'DM':
      return 5236
    case 'KINGBASE':
      return 54321
    default:
      return 3306
  }
}

export function getDatabaseFieldHint(type?: string) {
  switch (type?.toUpperCase()) {
    case 'ORACLE':
      return 'Service Name / SID，例如 ORCL'
    case 'H2':
      return '数据库名，本地默认 mem:opendb'
    case 'POSTGRESQL':
    case 'OPENGAUSS':
      return '默认数据库，例如 postgres'
    case 'DM':
      return '实例名，例如 DAMENG'
    case 'KINGBASE':
      return '默认数据库，例如 test'
    default:
      return '可选，连接后可切换'
  }
}

export function quoteIdentifier(type: string | undefined, name: string) {
  switch (type?.toUpperCase()) {
    case 'POSTGRESQL':
    case 'KINGBASE':
    case 'OPENGAUSS':
    case 'H2':
      return `"${name.replace(/"/g, '""')}"`
    case 'ORACLE':
    case 'DM':
      return `"${name.replace(/"/g, '""').toUpperCase()}"`
    default:
      return `\`${name.replace(/`/g, '``')}\``
  }
}
