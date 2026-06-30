import type { ConnectionProfile } from '@/types'

export interface ConnectionLatencyResult {
  profileId: string
  success: boolean
  latencyMs: number | null
  error?: string
}

export interface ConnectionHealthSummary {
  total: number
  online: number
  avgLatencyMs: number
  fastest: ConnectionLatencyResult | null
  slowest: ConnectionLatencyResult | null
}

const LATENCY_STORAGE_KEY = 'opendb-connection-latency'

export function loadLatencyCache(): Record<string, { latencyMs: number; testedAt: number; success: boolean }> {
  try {
    const raw = localStorage.getItem(LATENCY_STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

export function saveLatencyResult(profileId: string, latencyMs: number, success: boolean) {
  const cache = loadLatencyCache()
  cache[profileId] = { latencyMs, testedAt: Date.now(), success }
  localStorage.setItem(LATENCY_STORAGE_KEY, JSON.stringify(cache))
}

export function getCachedLatency(profileId: string) {
  return loadLatencyCache()[profileId] ?? null
}

export function latencyLabel(ms: number | null) {
  if (ms == null) return '未测试'
  if (ms < 50) return '极快'
  if (ms < 150) return '良好'
  if (ms < 500) return '一般'
  if (ms < 2000) return '偏慢'
  return '很慢'
}

export function latencyClass(ms: number | null) {
  if (ms == null) return 'unknown'
  if (ms < 150) return 'good'
  if (ms < 500) return 'ok'
  if (ms < 2000) return 'slow'
  return 'bad'
}

export function summarizeConnectionHealth(
  profiles: ConnectionProfile[],
  results: ConnectionLatencyResult[]
): ConnectionHealthSummary {
  const successResults = results.filter(r => r.success && r.latencyMs != null)
  const latencies = successResults.map(r => r.latencyMs!)
  const avgLatencyMs = latencies.length ? latencies.reduce((s, v) => s + v, 0) / latencies.length : 0
  const sorted = [...successResults].sort((a, b) => (a.latencyMs ?? 0) - (b.latencyMs ?? 0))

  return {
    total: profiles.length,
    online: successResults.length,
    avgLatencyMs,
    fastest: sorted[0] ?? null,
    slowest: sorted[sorted.length - 1] ?? null
  }
}

export function formatLatency(ms: number | null) {
  if (ms == null) return '-'
  return ms < 1000 ? `${Math.round(ms)} ms` : `${(ms / 1000).toFixed(2)} s`
}

export function profileTypeLabel(type: string) {
  const map: Record<string, string> = {
    MYSQL: 'MySQL',
    POSTGRESQL: 'PostgreSQL',
    ORACLE: 'Oracle',
    H2: 'H2',
    DM: '达梦 DM8',
    KINGBASE: 'KingbaseES',
    OPENGAUSS: 'openGauss'
  }
  return map[type] ?? type
}
