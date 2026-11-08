import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { BrainCircuit, RefreshCw, AlertCircle } from 'lucide-react';
import { useApp } from '../contexts/AppContext';
import { C } from '../constants/colors';
import { Card, CardHead } from '../components/ui';
import { RiskBadge } from '../components/badges/RiskBadge';
import { useTripList } from '../hooks/useTripList';

type Risk = 'SAFE' | 'WARNING' | 'DANGER';
type AiRow = {
  id: number;
  measurementId: string;
  tripId: number | null;
  modelType: string;
  modelVersion: string | null;
  predictedRisk: Risk | null;
  predictedProbability: number | null;
  ruleRisk: Risk | null;
  status: string;
  errorMessage: string | null;
  warnings: string | null;
  predictedAt: string | null;
};
type QueryMode = 'trip' | 'measurement';
const ENDPOINT = '/api/ai-predictions';
const isRisk = (v: unknown): v is Risk => v === 'SAFE' || v === 'WARNING' || v === 'DANGER';
const localTime = (v: string | null) => {
  if (!v) return '—';
  // Backend emits LocalDateTime without UTC suffix. Do not assume UTC or shift timezone.
  return v.replace('T', ' ').slice(0, 23);
};
const scoreText = (v: number | null) => typeof v === 'number' && Number.isFinite(v) ? v.toFixed(4) : '—';

function AiModelCard({ row }: { row: AiRow }) {
  const { t } = useApp();
  const usable = row.status === 'SUCCESS' && isRisk(row.predictedRisk);
  return <Card className="p-5">
    <div className="flex items-center justify-between gap-3 mb-4">
      <p className="font-bold text-sm" style={{ color: t.text }}>{row.modelType === 'TILT' ? '기울기 (TILT)' : row.modelType === 'CAMERA' ? '카메라 (CAMERA)' : row.modelType}</p>
      <span className="text-xs" style={{ color: usable ? C.safe : C.warn }}>{row.status}</span>
    </div>
    <div className="mb-3">{usable ? <RiskBadge level={row.predictedRisk as Risk} /> : <span style={{ color: C.warn }}>AI 분석 불가</span>}</div>
    <p className="text-xs mb-2" style={{ color: t.muted }}>모델 점수: {usable ? scoreText(row.predictedProbability) : '—'} (사고 확률 아님)</p>
    <p className="text-xs mb-2" style={{ color: t.muted }}>규칙 기반: {row.ruleRisk ?? '확인 불가'}</p>
    <p className="text-xs mb-2" style={{ color: t.muted }}>예측 시각(서버 기준): {localTime(row.predictedAt)}</p>
    <p className="text-xs mb-2" style={{ color: t.muted }}>모델 버전: {row.modelVersion ?? '—'}</p>
    <p className="text-xs break-all" style={{ color: t.muted }}>측정 ID: {row.measurementId}</p>
    {row.errorMessage && <p className="text-xs mt-3" style={{ color: C.danger }}>오류: {row.errorMessage}</p>}
    {row.warnings && <p className="text-xs mt-3 leading-5" style={{ color: t.muted }}>제한사항: {row.warnings.split(',').join(' · ')}</p>}
  </Card>;
}

export function AIAnalysis() {
  const { t } = useApp();
  const [mode, setMode] = useState<QueryMode>('trip');
  // 하드코딩된 주행 ID 대신 공용 주행 목록의 최신 주행을 기본값으로 사용 (직접 ID 입력도 가능)
  const { trips: tripList } = useTripList();
  const latestTripId = tripList[0] ? String(tripList[0].tripId) : '';
  const [inputIdState, setInputId] = useState<string | null>(null);
  const inputId = inputIdState ?? latestTripId;
  const [queryState, setQuery] = useState<{ mode: QueryMode; value: string } | null>(null);
  const query = queryState ?? (latestTripId ? { mode: 'trip' as QueryMode, value: latestTripId } : null);
  const [records, setRecords] = useState<AiRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);

  const url = !query ? null : query.mode === 'trip' ? `${ENDPOINT}?tripId=${encodeURIComponent(query.value)}` : `${ENDPOINT}/${encodeURIComponent(query.value)}`;
  useEffect(() => {
    if (!url) return;
    const target: string = url;
    const controller = new AbortController();
    async function run() {
      setLoading(true); setError(null); setRecords([]);
      try {
        const response = await fetch(target, { signal: controller.signal, cache: 'no-store' });
        if (!response.ok) throw new Error(`HTTP ${response.status} — 백엔드 주소/프록시를 확인해 주세요.`);
        const body: unknown = await response.json();
        if (!Array.isArray(body)) throw new Error('API 응답이 배열 형식이 아닙니다.');
        const valid = body.filter((v): v is AiRow => typeof v === 'object' && v !== null && 'modelType' in v && 'status' in v && 'measurementId' in v);
        if (valid.length !== body.length) throw new Error('일부 AI 응답에 필수 필드가 없습니다.');
        if (!controller.signal.aborted) setRecords(valid);
      } catch (err) {
        if (!controller.signal.aborted) { setError(err instanceof Error ? err.message : 'AI 데이터 조회 실패'); setRecords([]); }
      } finally { if (!controller.signal.aborted) setLoading(false); }
    }
    void run(); return () => controller.abort();
  }, [url, revision]);

  const sorted = useMemo(() => [...records].sort((a, b) => {
    // Use numeric DB ids (newer records have larger ids), independent of timezone parsing.
    return (Number(b.id) || 0) - (Number(a.id) || 0);
  }), [records]);
  const latestByModel = useMemo(() => {
    const map = new Map<string, AiRow>();
    for (const row of sorted) if (!map.has(row.modelType)) map.set(row.modelType, row);
    return [...map.values()];
  }, [sorted]);
  const successCount = records.filter(r => r.status === 'SUCCESS').length;
  const rule = sorted.find(r => isRisk(r.ruleRisk))?.ruleRisk;

  function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); const next = inputId.trim();
    if (!next) { setError(`${mode === 'trip' ? '주행' : '측정'} ID를 입력해 주세요.`); return; }
    if (mode === 'trip' && (!/^\d+$/.test(next) || Number(next) < 1)) { setError('주행 ID는 양의 정수여야 합니다.'); return; }
    setQuery({ mode, value: next }); setRevision(n => n + 1);
  }

  return <div className="space-y-4">
    <Card className="p-5">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl flex items-center justify-center" style={{ background: `${C.blue}18` }}><BrainCircuit size={24} style={{ color: C.blue }} /></div>
          <div><p className="font-bold text-sm" style={{ color: t.text }}>센서 AI 예측 결과</p><p className="text-xs mt-1" style={{ color: t.muted }}>Spring Boot DB의 주행별 TILT·CAMERA 실험용 AI 보조 분석</p></div>
        </div>
        <form className="flex flex-wrap gap-2" onSubmit={handleSubmit}>
          <select aria-label="조회 유형" className="h-9 rounded-xl px-3 text-sm" style={{ background: t.input, border: `1px solid ${t.border}`, color: t.text }} value={mode} onChange={e => { const next = e.target.value as QueryMode; setMode(next); setInputId(next === 'trip' ? null : ''); }}>
            <option value="trip">주행 ID</option><option value="measurement">측정 ID</option>
          </select>
          <input aria-label={mode === 'trip' ? '주행 ID' : '측정 ID'} value={inputId} onChange={e => setInputId(e.target.value)} className="h-9 rounded-xl px-3 text-sm" style={{ background: t.input, border: `1px solid ${t.border}`, color: t.text }} placeholder={mode === 'trip' ? 'tripId' : 'measurementId'} list={mode === 'trip' ? 'ai-trip-options' : undefined} />
          <datalist id="ai-trip-options">{tripList.map(tr => <option key={tr.tripId} value={String(tr.tripId)} />)}</datalist>
          <button type="submit" className="h-9 px-4 rounded-xl text-white text-sm font-semibold inline-flex gap-2 items-center" style={{ background: C.blue }} disabled={loading}><RefreshCw size={14} /> 조회 / 새로고침</button>
        </form>
      </div>
      <p className="mt-3 text-xs break-all" style={{ color: t.muted }}>현재 조회: {query ? (query.mode === 'trip' ? `Trip ${query.value}` : `Measurement ${query.value}`) : '없음 (주행 ID를 입력하세요)'}{url ? ` · API: ${url}` : ''}</p>
    </Card>
    {error && <Card className="p-5"><p className="text-sm flex gap-2" style={{ color: C.danger }}><AlertCircle size={16} />{error}</p></Card>}
    {query && loading && <Card className="p-5"><p className="text-sm" style={{ color: t.muted }}>AI 결과를 불러오는 중입니다…</p></Card>}
    {query && !loading && !error && records.length === 0 && <Card className="p-5"><p className="text-sm" style={{ color: t.muted }}>해당 {query.mode === 'trip' ? '주행' : '측정'} ID에 저장된 AI 결과가 없습니다.</p></Card>}
    {query && !loading && !error && records.length > 0 && <>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="p-5"><p className="text-xs" style={{ color: t.muted }}>조회된 AI 기록</p><p className="text-2xl font-bold mt-2" style={{ color: t.text }}>{records.length}건</p></Card>
        <Card className="p-5"><p className="text-xs" style={{ color: t.muted }}>예측 성공 (SUCCESS)</p><p className="text-2xl font-bold mt-2" style={{ color: t.text }}>{successCount}건</p></Card>
        <Card className="p-5"><p className="text-xs" style={{ color: t.muted }}>확인된 모델 종류</p><p className="text-2xl font-bold mt-2" style={{ color: t.text }}>{latestByModel.length}종</p></Card>
      </div>
      <Card><CardHead title="최근 기록의 규칙 기반 판정" sub="AI와 별개로 유지되는 안전 제어 기준" /><div className="p-5">{rule && isRisk(rule) ? <RiskBadge level={rule} /> : <span className="text-sm" style={{ color: t.muted }}>확인 불가</span>}</div></Card>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">{latestByModel.map(row => <AiModelCard key={row.modelType} row={row} />)}</div>
      <Card className="p-5">
        <p className="font-bold text-sm mb-3" style={{ color: t.text }}>최근 예측 기록 (최대 30건)</p>
        <div className="overflow-x-auto"><table className="w-full text-xs text-left" style={{ color: t.text }}><thead><tr style={{ color: t.muted }}><th className="p-2">ID</th><th className="p-2">모델</th><th className="p-2">AI 등급</th><th className="p-2">규칙</th><th className="p-2">상태</th><th className="p-2">시각(서버 기준)</th><th className="p-2">측정 ID</th></tr></thead><tbody>{sorted.slice(0, 30).map(row => <tr key={row.id} style={{ borderTop: `1px solid ${t.border}` }}><td className="p-2">{row.id}</td><td className="p-2">{row.modelType}</td><td className="p-2">{row.status === 'SUCCESS' ? row.predictedRisk ?? '—' : 'AI 분석 불가'}</td><td className="p-2">{row.ruleRisk ?? '—'}</td><td className="p-2">{row.status}</td><td className="p-2 whitespace-nowrap">{localTime(row.predictedAt)}</td><td className="p-2 max-w-40 truncate" title={row.measurementId}>{row.measurementId}</td></tr>)}</tbody></table></div>
      </Card>
    </>}
    <Card className="p-4"><p className="text-xs leading-5" style={{ color: t.muted }}>AI는 실험용 보조 분석이며 점수는 사고 확률이 아닙니다. 실패·미수집을 SAFE로 해석하지 않습니다. 안전 제어는 기존 규칙 기반입니다. 최근 결과는 조회 시점의 저장 기록이며 자동 실시간 갱신은 하지 않습니다.</p></Card>
  </div>;
}
