# 앱 APK를 빌드해서 Firebase App Distribution 그룹에 올린다.
# 사용: .\scripts\deploy-app.ps1 -AppId "1:123456789:android:abcdef"   (또는 환경변수 FIREBASE_APP_ID)
# 최초 1회: npx firebase-tools login
param(
    [string]$AppId = $env:FIREBASE_APP_ID,
    [string]$Group = "팀원",   # Firebase 콘솔 '테스터 및 그룹'의 그룹 별칭(alias)
    [string]$Notes = ""
)
$ErrorActionPreference = "Stop"
if (-not $AppId) { throw "앱 ID가 없습니다. -AppId 로 주거나 FIREBASE_APP_ID 환경변수를 설정하세요. (Firebase 콘솔 > 프로젝트 설정 > 내 앱)" }

$root = Split-Path $PSScriptRoot -Parent
Push-Location (Join-Path $root "app")
try {
    & .\gradlew.bat :app:assembleDebug --console=plain -q
    if ($LASTEXITCODE -ne 0) { throw "빌드 실패" }
} finally { Pop-Location }

$apk = Get-ChildItem -Recurse -Filter app-debug.apk (Join-Path $root "app\app\build") |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) { throw "app-debug.apk 를 찾지 못했습니다." }

if (-not $Notes) { $Notes = (git -C $root log -1 --pretty=%s) }

# 한글이 깨지지 않게 릴리스 노트는 UTF-8 파일로 넘긴다(PowerShell 5.1은 인자의 한글을 깨뜨림)
$notesFile = Join-Path $env:TEMP "release-notes.txt"
[IO.File]::WriteAllText($notesFile, $Notes, (New-Object Text.UTF8Encoding($false)))
npx firebase-tools appdistribution:distribute $apk.FullName --app $AppId --groups $Group --release-notes-file $notesFile
if ($LASTEXITCODE -ne 0) { throw "업로드 실패" }
