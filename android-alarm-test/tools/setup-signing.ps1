# Run locally on your Windows PC. Never commit the generated key or secret.
$ErrorActionPreference = 'Stop'
$folder = Join-Path $env:USERPROFILE 'shift-alarm-signing'
New-Item -ItemType Directory -Force -Path $folder | Out-Null
$configPath = Join-Path $folder 'signing-secret.json'
if (!(Test-Path $configPath)) {
    $keytool = Get-Command keytool -ErrorAction SilentlyContinue
    $tool = if ($keytool) { $keytool.Source } else { Join-Path $env:ProgramFiles 'Android\Android Studio\jbr\bin\keytool.exe' }
    if (!(Test-Path $tool)) { throw 'Install Android Studio first, then run this script again.' }
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $password = [Convert]::ToBase64String($bytes)
    $keyPath = Join-Path $folder 'shift-alarm.p12'
    if (Test-Path $keyPath) { throw 'Existing key without saved configuration. Restore its backup; do not overwrite it.' }
    $env:SHIFT_SIGN_PASSWORD = $password
    try {
        & $tool -genkeypair -keystore $keyPath -storetype PKCS12 -alias shift-alarm -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Shift Alarm, OU=Android, O=gy7604, C=KR' -storepass:env SHIFT_SIGN_PASSWORD -keypass:env SHIFT_SIGN_PASSWORD
        if ($LASTEXITCODE -ne 0) { throw 'Key generation failed.' }
    } finally { Remove-Item Env:\SHIFT_SIGN_PASSWORD -ErrorAction SilentlyContinue }
    $data = @{keystore=[Convert]::ToBase64String([IO.File]::ReadAllBytes($keyPath)); password=$password; alias='shift-alarm'}
    $data | ConvertTo-Json -Compress | Set-Content -Encoding UTF8 $configPath
}
$secret = [IO.File]::ReadAllText($configPath).TrimStart([char]0xFEFF).Trim()
$null = $secret | ConvertFrom-Json
Set-Clipboard -Value $secret
Write-Host 'Signing secret copied to clipboard. Keep this folder privately backed up:'
Write-Host $folder
Start-Process 'https://github.com/gy7604/worktable/settings/secrets/actions/new'
Write-Host 'Secret name: SHIFT_ALARM_SIGNING_JSON'
Write-Host 'Paste the clipboard into Secret and click Add secret. Re-running this script reuses the SAME key.'
